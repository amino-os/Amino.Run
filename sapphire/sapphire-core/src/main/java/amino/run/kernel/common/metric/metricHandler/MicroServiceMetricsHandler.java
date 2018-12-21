package amino.run.kernel.common.metric.metricHandler;

import amino.run.app.MicroServiceSpec;
import amino.run.common.ReplicaID;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.metric.*;
import amino.run.kernel.common.metric.metricHandler.RPCMetric.RPCCountHandler;
import amino.run.policy.DefaultPolicy;
import amino.run.policy.util.ResettableTimer;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.logging.Logger;

/** Handle Metric collection for RPC and MicroService Stats. */
public class MicroServiceMetricsHandler implements Serializable {
    private static Logger logger = Logger.getLogger(MicroServiceMetricsHandler.class.getName());
    private transient DefaultPolicy.DefaultServerPolicy policy;
    private transient HashMap<String, Object> metricMetadata;
    private transient ArrayList<RPCMetricHandler> rpcHandlers = new ArrayList<RPCMetricHandler>();
    private transient ResettableTimer metricSendTimer;
    private transient HashMap<String, String> labels;
    private transient int metricUpdateFrequency = 10000;
    public static transient String REPLICA_ID_LABEL = "ReplicaID";
    public static transient String MICRO_SERVICE_ID_LABEL = "MicroServiceID";

    private MicroServiceMetricsHandler(
            DefaultPolicy.DefaultServerPolicy policy,
            HashMap<String, Object> metricMetadata,
            HashMap<String, String> microServiceLabels) {
        this.policy = policy;
        this.metricMetadata = metricMetadata;

        // construct labels for metric each Metric of replica.
        // Currently labels maintains tags with ReplicaID , MicroServiceID and labels provided by
        // MicroService developer.
        ReplicaID replicaID = policy.getReplicaId();
        HashMap<String, String> labels = new HashMap<String, String>();
        labels.put(REPLICA_ID_LABEL, replicaID.getID().toString());
        labels.put(MICRO_SERVICE_ID_LABEL, replicaID.getOID().getID().toString());
        labels.putAll(microServiceLabels);
        this.labels = labels;

        // update metric update frequency with configured metric collection frequency
        Object object = metricMetadata.get(MetricConstants.METRIC_FREQUENCY);
        if (object instanceof Integer) {
            metricUpdateFrequency = (Integer) object;
        }
    }

    /**
     * Handle metric collection on RPC calls. Each constructed RPC metric handler is invokes one
     * after other for metric collection.
     *
     * @param method RPC method
     * @param params RPC parameters
     * @return onRPC response
     * @throws Exception
     */
    public Object onRPC(String method, ArrayList<Object> params) throws Exception {
        for (RPCMetricHandler handler : rpcHandlers) {
            handler.handle(method, params);
        }
        // TODO: Add special handling for Execution time handler. ExecutionTimeHandler handles
        // execution time of RPC calls and should be last RPC handler
        return policy.getAppObject().invoke(method, params);
    }

    public static MicroServiceMetricsHandler create(
            DefaultPolicy.DefaultServerPolicy policy, MicroServiceSpec spec) {
        HashMap<String, Object> annotations = spec.getAnnotations();
        // TODO: Add validation for null check when annotations are created with snakeyaml
        if (annotations == null || annotations.isEmpty()) {
            return null;
        }

        if (!GlobalKernelReferences.metricClient.isInitialized()) {
            logger.warning(
                    String.format(
                            "Metric client not initialize. Skipping metric collection for %s",
                            spec.getLabels()));
            return null;
        }

        Object metricAnnotations = annotations.get(MetricConstants.METRIC_ANNOTATION);
        if (!(metricAnnotations instanceof HashMap)) {
            return null;
        }

        HashMap<String, Object> metricMetadata = (HashMap<String, Object>) metricAnnotations;

        MicroServiceMetricsHandler handler =
                new MicroServiceMetricsHandler(policy, metricMetadata, spec.getLabels());
        handler.initialize();
        return handler;
    }

    // construct RPC metric handler chain and Timer for collecting Metrics from RPC metric handler.
    // TODO: Add Metric handler for DM specific metric collections
    private void initialize() {
        // RPC metric handler chain construction
        constructRPCMetricHandlerChain();

        // Metric collection timer
        metricSendTimer =
                new ResettableTimer(
                        new TimerTask() {
                            public void run() {
                                ArrayList<Metric> metrics = new ArrayList<Metric>();
                                Metric metric;
                                try {
                                    // collect metric for all RPC specific Metric
                                    for (RPCMetricHandler handler : rpcHandlers) {
                                        metric = handler.getMetric();
                                        if (!metric.isEmpty()) {
                                            metrics.add(metric);
                                        }
                                    }

                                    GlobalKernelReferences.metricClient.send(metrics);
                                } catch (Exception e) {
                                    logger.warning(
                                            String.format(
                                                    "%s: Sending metric failed", e.toString()));
                                    return;
                                }
                                // reset the count value and timer after push is done
                                metricSendTimer.reset();
                            }
                        },
                        metricUpdateFrequency);
        metricSendTimer.start();
    }

    // construct RPC handler chain
    private void constructRPCMetricHandlerChain() {
        RPCMetricHandler handler;
        Object object = metricMetadata.get(MetricConstants.METRIC_LIST);
        ArrayList<String> enabledMetric = new ArrayList<String>();
        if (object instanceof ArrayList) {
            enabledMetric.addAll((ArrayList<String>) object);
        }

        // rpc counter creation
        if (enabledMetric.contains(RPCCountHandler.metricName)) {
            handler = new RPCCountHandler(labels);
            GlobalKernelReferences.metricClient.registerSchema(handler.getSchema());
            rpcHandlers.add(handler);
        }
    }
}

package amino.run.kernel.common.metric.metricHandler;

import amino.run.app.MicroServiceSpec;
import amino.run.common.ReplicaID;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.metric.*;
import amino.run.kernel.common.metric.metricHandler.RPCMetric.RPCCountHandler;
import amino.run.kernel.common.metric.metricHandler.RPCMetric.RPCMetricHandler;
import amino.run.policy.DefaultPolicy;
import amino.run.policy.util.ResettableTimer;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.logging.Logger;

/** Handle Metric collection for RPC and MicroService Stats. */
public class MicroServiceMetricManager implements Serializable {
    private static Logger logger = Logger.getLogger(MicroServiceMetricManager.class.getName());
    private transient DefaultPolicy.DefaultServerPolicy policy;
    private transient ArrayList<RPCMetricHandler> rpcHandlers = new ArrayList<RPCMetricHandler>();
    private transient ResettableTimer metricSendTimer;
    private transient HashMap<String, String> labels;
    private transient int metricUpdateFrequency = 10000;
    public static transient String REPLICA_ID_LABEL = "ReplicaID";
    public static transient String MICRO_SERVICE_ID_LABEL = "MicroServiceID";
    public static transient String MICRO_SERVICE_NAME = "MicroServiceName";

    private MicroServiceMetricManager() {}

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

        return policy.upRPCCall(method, params);
    }

    public static MicroServiceMetricManager create(
            DefaultPolicy.DefaultServerPolicy policy, MicroServiceSpec spec) {
        HashMap<String, Object> annotations = spec.getAnnotations();
        // TODO: Add validation for null check when annotations are created with snakeyaml
        if (annotations == null || annotations.isEmpty()) {
            return null;
        }

        if (!GlobalKernelReferences.nodeServer.getMetricClient().isInitialized()) {
            logger.warning(
                    String.format(
                            "Metric client not initialize. Skipping metric collection for %s",
                            spec.getName()));
            return null;
        }

        Object metricAnnotations = annotations.get(MetricConstants.METRIC_ANNOTATION);
        if (!(metricAnnotations instanceof HashMap)) {
            return null;
        }

        HashMap<String, Object> metricMetadata = (HashMap<String, Object>) metricAnnotations;

        MicroServiceMetricManager metricManager = new MicroServiceMetricManager();
        metricManager.policy = policy;

        // construct labels for metric each Metric of replica.
        // Currently labels maintains tags with ReplicaID , MicroServiceID and labels provided by
        // MicroService developer.
        ReplicaID replicaID = policy.getReplicaId();
        HashMap<String, String> labels = new HashMap<String, String>();
        labels.put(REPLICA_ID_LABEL, replicaID.getID().toString());
        labels.put(MICRO_SERVICE_ID_LABEL, replicaID.getOID().getID().toString());
        if (!spec.getName().isEmpty()) {
            labels.put(MICRO_SERVICE_NAME, spec.getName());
        }
        metricManager.labels = labels;

        // update metric update frequency with configured metric collection frequency
        Object object = metricMetadata.get(MetricConstants.METRIC_FREQUENCY);
        if (object instanceof Integer) {
            metricManager.metricUpdateFrequency = (Integer) object;
        }

        metricManager.initialize(metricMetadata);
        return metricManager;
    }

    // construct RPC metric handler chain and Timer for collecting Metrics from RPC metric handler.
    // TODO: Add Metric handler for DM specific metric collections
    private void initialize(HashMap<String, Object> metricMetadata) {
        // RPC metric handler chain construction
        constructRPCMetricHandlerChain(metricMetadata);

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

                                    GlobalKernelReferences.nodeServer
                                            .getMetricClient()
                                            .send(metrics);
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
    private void constructRPCMetricHandlerChain(HashMap<String, Object> metricMetadata) {
        RPCMetricHandler handler;
        Object object = metricMetadata.get(MetricConstants.RPC_METRIC_HANDLERS);
        ArrayList<String> enabledMetric = new ArrayList<String>();
        if (object instanceof ArrayList) {
            enabledMetric.addAll((ArrayList<String>) object);
        }

        // rpc counter creation
        if (enabledMetric.contains(RPCCountHandler.METRIC_NAME)) {
            handler = new RPCCountHandler(labels);
            GlobalKernelReferences.nodeServer.getMetricClient().registerSchema(handler.getSchema());
            rpcHandlers.add(handler);
        }
    }
}

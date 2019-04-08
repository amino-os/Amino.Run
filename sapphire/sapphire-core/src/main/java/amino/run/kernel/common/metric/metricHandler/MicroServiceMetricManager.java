package amino.run.kernel.common.metric.metricHandler;

import amino.run.app.MicroServiceSpec;
import amino.run.common.ReplicaID;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.metric.*;
import amino.run.kernel.common.metric.metricHandler.RPCMetric.*;
import amino.run.policy.DefaultPolicy;
import amino.run.policy.util.ResettableTimer;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * Handle Metric collection for RPC and MicroService statistics.
 *
 * <p></> It maintains RPC metric chain for handling metric collection on onRPC call. It also
 * initialize a timer for collecting metrics from all metric handler it manages and push the metric
 * to kernel server metric client.
 *
 * @author AmitRoushan
 */
public class MicroServiceMetricManager implements Serializable {
    private static Logger logger = Logger.getLogger(MicroServiceMetricManager.class.getName());
    /** Default policy instance for handing over normal RPC call after metric collection */
    private transient DefaultPolicy.DefaultServerPolicy policy;
    /** maintains RPC handler chain which gets called for each onRPC call */
    private transient RPCMetricHandler rpcHandlerChain;
    /** Timer thread for collecting metric periodically */
    private transient ResettableTimer metricSendTimer;

    private transient int metricUpdateFrequency = 10000;
    private transient KernelMetricClient metricClient;
    public static final String REPLICA_ID_LABEL = "ReplicaID";
    public static final String MICRO_SERVICE_ID_LABEL = "MicroServiceID";
    public static final String MICRO_SERVICE_NAME = "MicroServiceName";

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
        return rpcHandlerChain.handle(method, params);
    }

    /**
     * Factory method for MicroServiceMetricManager instance
     *
     * @param policy default server policy
     * @param spec MicroServiceSpec instance
     * @return
     */
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
        metricManager.metricClient = GlobalKernelReferences.nodeServer.getMetricClient();

        // construct labels for metric each Metric of replica.
        // Currently labels maintains tags with ReplicaID, MicroServiceID and labels provided by
        // MicroService developer.
        ReplicaID replicaID = policy.getReplicaId();
        HashMap<String, String> labels = new HashMap<String, String>();
        labels.put(REPLICA_ID_LABEL, replicaID.getID().toString());
        labels.put(MICRO_SERVICE_ID_LABEL, replicaID.getOID().getID().toString());
        if (!spec.getName().isEmpty()) {
            labels.put(MICRO_SERVICE_NAME, spec.getName());
        }

        // update metric update frequency with configured metric collection frequency
        Object object = metricMetadata.get(MetricConstants.METRIC_FREQUENCY);
        if (object instanceof Integer) {
            metricManager.metricUpdateFrequency = (Integer) object;
        }

        metricManager.initialize(metricMetadata);
        return metricManager;
    }

    /**
     * Construct RPC metric handler chain and Timer for collecting Metrics from RPC metric handler
     */
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
                                try {
                                    // collect metric for all RPC specific Metric
                                    RPCMetricHandler handler = rpcHandlerChain;
                                    MicroServiceMetricManager manager = handler.getManager();

                                    while (handler != null) {
                                        metrics.add(handler.getMetric());
                                        handler = handler.getNextHandler();
                                    }

                                    metricClient.send(manager, metrics);
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

    /** Construct RPC handler chain */
    private void constructRPCMetricHandlerChain(HashMap<String, Object> metricMetadata) {
        RPCMetricHandler handler;
        RPCMetricHandler prevHandler;
        Object object = metricMetadata.get(MetricConstants.RPC_METRIC_HANDLERS);
        ArrayList<String> enabledMetric = new ArrayList<String>();
        if (object instanceof ArrayList) {
            enabledMetric.addAll((ArrayList<String>) object);
        }

        // execution time handler should be last in chain
        if (enabledMetric.contains(ExecutionTimeHandler.METRIC_NAME)) {
            handler = new ExecutionTimeHandler(this, true);
        } else {
            handler = new ExecutionTimeHandler(this, false);
        }
        metricClient.registerSchema(this, handler.getSchema());
        prevHandler = handler;

        // RPC counter creation
        if (enabledMetric.contains(CountHandler.METRIC_NAME)) {
            handler = new CountHandler(this);
            metricClient.registerSchema(this, handler.getSchema());
            handler.setNextHandler(prevHandler);
        }

        rpcHandlerChain = handler;
    }

    public DefaultPolicy.DefaultServerPolicy getPolicy() {
        return policy;
    }
}

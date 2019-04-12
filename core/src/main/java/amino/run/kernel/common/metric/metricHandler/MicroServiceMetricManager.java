package amino.run.kernel.common.metric.metricHandler;

import amino.run.app.MicroServiceSpec;
import amino.run.common.ReplicaID;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.metric.Metric;
import amino.run.kernel.common.metric.metricHandler.RPCMetric.ByteInHandler;
import amino.run.kernel.common.metric.metricHandler.RPCMetric.ByteOutHandler;
import amino.run.kernel.common.metric.metricHandler.RPCMetric.CountHandler;
import amino.run.kernel.common.metric.metricHandler.RPCMetric.ExecutionTimeHandler;
import amino.run.kernel.common.metric.metricHandler.RPCMetric.RPCMetricHandler;
import amino.run.kernel.common.metric.schema.Schema;
import amino.run.policy.DefaultPolicy;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
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
    /** metric tags */
    private HashMap<String, String> labels;

    private transient int metricUpdateFrequency = 10000;
    public static final String REPLICA_ID_LABEL = "ReplicaID";
    public static final String MICRO_SERVICE_ID_LABEL = "MicroServiceID";
    public static final String MICRO_SERVICE_NAME = "MicroServiceName";
    /** Constant for enabling metric collection */
    public static final String METRIC_ANNOTATION = "amino.metric";
    /** Constant for metric collection frequency configuration */
    public static final String METRIC_FREQUENCY = "frequency";
    /** Constant for list of enabled metrics */
    public static final String RPC_METRIC_HANDLERS = "rpcMetrics";

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

        Object metricAnnotations = annotations.get(METRIC_ANNOTATION);
        if (!(metricAnnotations instanceof HashMap)) {
            return null;
        }

        HashMap<String, Object> metricMetadata = (HashMap<String, Object>) metricAnnotations;

        MicroServiceMetricManager metricManager = new MicroServiceMetricManager();
        metricManager.policy = policy;

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
        Object object = metricMetadata.get(METRIC_FREQUENCY);
        if (object instanceof Integer) {
            metricManager.metricUpdateFrequency = (Integer) object;
        }

        metricManager.labels = labels;
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
        GlobalKernelReferences.nodeServer.getMetricClient().registerMetricManager(this);
    }

    /** Uninitialize metric manager and unregister it with kernel metric client */
    public void destroy() {
        GlobalKernelReferences.nodeServer.getMetricClient().unregisterMetricManager(this);
    }

    /** Construct RPC handler chain */
    private void constructRPCMetricHandlerChain(HashMap<String, Object> metricMetadata) {
        RPCMetricHandler handler;
        RPCMetricHandler prevHandler;
        Object object = metricMetadata.get(RPC_METRIC_HANDLERS);
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
        prevHandler = handler;

        // rpc byte in data metric handler
        if (enabledMetric.contains(ByteOutHandler.METRIC_NAME)) {
            handler = new ByteOutHandler(this);
            handler.setNextHandler(prevHandler);
            prevHandler = handler;
        }

        // rpc byte in data metric handler
        if (enabledMetric.contains(ByteInHandler.METRIC_NAME)) {
            handler = new ByteInHandler(this);
            handler.setNextHandler(prevHandler);
            prevHandler = handler;
        }

        // RPC counter creation
        if (enabledMetric.contains(CountHandler.METRIC_NAME)) {
            handler = new CountHandler(this);
            handler.setNextHandler(prevHandler);
        }

        rpcHandlerChain = handler;
    }

    /**
     * Return default server policy
     *
     * @return
     */
    public DefaultPolicy.DefaultServerPolicy getPolicy() {
        return policy;
    }

    /**
     * Return metric schema of all handlers managed by metric manager
     *
     * @return
     */
    public ArrayList<Schema> getSchemas() {
        ArrayList<Schema> schemas = new ArrayList<Schema>();

        // add rpc handler schema
        RPCMetricHandler handler = rpcHandlerChain;
        while (handler != null) {
            Schema schema = handler.getSchema();
            if (schema != null) {
                schemas.add(handler.getSchema());
            }
            handler = handler.getNextHandler();
        }

        return schemas;
    }

    /**
     * Retrieve and return metric managed in metric handlers
     *
     * @return list of metrics
     */
    public ArrayList<Metric> getMetrics() {
        ArrayList<Metric> metrics = new ArrayList<Metric>();

        // collect metric for all RPC specific Metric
        RPCMetricHandler handler = rpcHandlerChain;
        Metric metric;
        while (handler != null) {
            metric = handler.getMetric();
            if (metric != null) {
                metrics.add(metric);
            }
            handler = handler.getNextHandler();
        }

        return metrics;
    }

    /**
     * Return metric retrieval frequency
     *
     * @return
     */
    public int getMetricUpdateFrequency() {
        return metricUpdateFrequency;
    }

    /**
     * Return tags which should be supplied on each metric
     *
     * @return
     */
    public HashMap<String, String> getLabels() {
        return labels;
    }
}

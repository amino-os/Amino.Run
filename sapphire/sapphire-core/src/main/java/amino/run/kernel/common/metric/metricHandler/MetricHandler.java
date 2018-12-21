package amino.run.kernel.common.metric.metricHandler;

import amino.run.app.MicroServiceSpec;
import amino.run.common.ReplicaID;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.metric.*;
import amino.run.policy.DefaultPolicy;
import amino.run.policy.util.ResettableTimer;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.logging.Logger;

public class MetricHandler implements Serializable {
    private static Logger logger = Logger.getLogger(MetricHandler.class.getName());
    private transient DefaultPolicy.DefaultServerPolicy policy;
    private transient HashMap<String, Object> metricMetadata;
    private transient RPCMetricHandler rpcMetricHandler;
    private transient ResettableTimer metricSendTimer;
    private transient HashMap<String, String> labels;
    private transient int metricUpdateFrequency = 10000;
    public static transient String REPLICA_ID_LABEL = "ReplicaID";
    public static transient String MICRO_SERVICE_ID_LABEL = "MicroServiceID";

    private MetricHandler(
            DefaultPolicy.DefaultServerPolicy policy,
            HashMap<String, Object> metricMetadata,
            HashMap<String, String> microServiceLabels) {
        this.policy = policy;
        this.metricMetadata = metricMetadata;

        // construct metric label
        ReplicaID replicaID = policy.getReplicaId();
        HashMap<String, String> labels = new HashMap<String, String>();
        labels.put(REPLICA_ID_LABEL, replicaID.getID().toString());
        labels.put(MICRO_SERVICE_ID_LABEL, replicaID.getOID().getID().toString());
        labels.putAll(microServiceLabels);
        this.labels = labels;

        // update metric update frequency
        Object object = metricMetadata.get(MetricConstants.METRIC_FREQUENCY);
        if (object instanceof Integer) {
            metricUpdateFrequency = (Integer) object;
        }
    }

    public Object onRPC(String method, ArrayList<Object> params) throws Exception {
        return rpcMetricHandler.handle(method, params);
    }

    public static MetricHandler create(
            DefaultPolicy.DefaultServerPolicy policy, MicroServiceSpec spec) {
        HashMap<String, Object> annotations = spec.getAnnotations();
        if (annotations == null
                || annotations.isEmpty()
                || !annotations.containsKey(MetricConstants.METRIC_ANNOTATION)) {
            return null;
        }

        if (GlobalKernelReferences.metricClient == null) {
            logger.warning(
                    String.format(
                            "Metric client not initialize. Skipping metric collection for %s",
                            spec.getLabels()));
            return null;
        }

        Object object = annotations.get(MetricConstants.METRIC_ANNOTATION);
        if (!(object instanceof HashMap)) {
            return null;
        }
        HashMap<String, Object> metricMetadata = (HashMap<String, Object>) object;

        MetricHandler handler = new MetricHandler(policy, metricMetadata, spec.getLabels());
        handler.initialize();
        return handler;
    }

    private void initialize() {
        // TODO initialize metrics which need to get collected based on Developer input
        rpcMetricHandler = constructRPCMetricHandlerChain();

        metricSendTimer =
                new ResettableTimer(
                        new TimerTask() {
                            public void run() {
                                ArrayList<Metric> metrics = new ArrayList<Metric>();
                                Metric metric;
                                try {
                                    // collect metric for all RPC specific Metric
                                    RPCMetricHandler handler = rpcMetricHandler;
                                    while (handler != null) {
                                        metric = handler.getMetric();
                                        if (metric != null && !metric.isEmpty()) {
                                            metrics.add(metric);
                                        }
                                        handler = handler.getNextHandler();
                                    }

                                    // collect DM specific metrics
                                    metrics.addAll(policy.getDMMetrics());

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

    private void registerSchema(MetricSchema schema) {
        try {
            // register memory stat metric
            if (!GlobalKernelReferences.metricClient.isRegistered(schema)) {
                GlobalKernelReferences.metricClient.register(schema);
            }
        } catch (SchemaAlreadyRegistered e) {
            logger.warning(e.getMessage());
        }
    }

    private RPCMetricHandler constructRPCMetricHandlerChain() {
        RPCMetricHandler prevHandler;

        Object object = metricMetadata.get(MetricConstants.METRIC_LIST);
        ArrayList<String> enabledMetric = new ArrayList<String>();
        if (object instanceof ArrayList) {
            enabledMetric.addAll((ArrayList<String>) object);
        }

        // execution time handler should be last in chain
        RPCMetricHandler handler;
        if (enabledMetric.contains(ExecutionTimeHandler.metricName)) {
            handler = new ExecutionTimeHandler(labels, policy, true);
        } else {
            handler = new ExecutionTimeHandler(labels, policy, false);
        }
        registerSchema(handler.getSchema());
        prevHandler = handler;

        // rpc data handler creation
        if (enabledMetric.contains(RPCDataHandler.metricName)) {
            handler = new RPCDataHandler(labels);
            registerSchema(handler.getSchema());
            handler.setNextHandler(prevHandler);
            prevHandler = handler;
        }

        // rpc counter creation
        if (enabledMetric.contains(RPCCountHandler.metricName)) {
            handler = new RPCCountHandler(labels);
            registerSchema(handler.getSchema());
            handler.setNextHandler(prevHandler);
        }

        return handler;
    }
}

package sapphire.policy.metric;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.TimerTask;
import java.util.logging.Logger;
import sapphire.app.SapphireObjectServer;
import sapphire.app.SapphireObjectSpec;
import sapphire.app.labelselector.Labels;
import sapphire.app.labelselector.Selector;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireReplicaID;
import sapphire.common.Utils;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.oms.OMSServer;
import sapphire.policy.SapphirePolicyUpcalls;
import sapphire.policy.util.ResettableTimer;
import sapphire.sysSapphireObjects.metricCollector.MetricCollector;
import sapphire.sysSapphireObjects.metricCollector.MetricCollectorLabels;
import sapphire.sysSapphireObjects.metricCollector.metric.counter.CounterClientMetric;
import sapphire.sysSapphireObjects.metricCollector.metric.counter.Schema;

public class MetricAggregator implements Serializable {
    private MetricPolicy.Config config = new MetricPolicy.Config();
    private MetricCollector collectorStub;
    private CounterClientMetric rpcCounter;
    private transient ResettableTimer metricSendTimer;
    static Logger logger = Logger.getLogger(MetricAggregator.class.getCanonicalName());

    private OMSServer oms() {
        return GlobalKernelReferences.nodeServer.oms;
    }

    MetricAggregator(
            SapphireObjectSpec spec, SapphireObjectID soID, SapphireReplicaID soReplicaID) {
        Map<String, SapphirePolicyUpcalls.SapphirePolicyConfig> configMap =
                Utils.fromDMSpecListToFlatConfigMap(spec.getDmList());
        if (configMap != null) {
            SapphirePolicyUpcalls.SapphirePolicyConfig config =
                    configMap.get(MetricPolicy.Config.class.getName());
            if (config != null) {
                this.config = ((MetricPolicy.Config) config);
            }
        }

        // add sapphire object ID and replica ID in metric labels
        Map<String, String> labelsMap = config.getMetricLabels().getLabels();
        labelsMap.put(MetricDMConstants.METRIC_SAPPHIRE_OBJECT_ID, soID.getID().toString());
        labelsMap.put(MetricDMConstants.METRIC_SAPPHIRE_REPLICA_ID, soReplicaID.getID().toString());
        Labels labels = new Labels();
        labels.setLabels(labelsMap);
        config.setMetricLabels(labels);
    }

    public boolean initialize() {
        return !(collectorStub == null);
    }

    public void $__initialize() throws Exception {
        if (!initialize()) {
            // create selector
            Selector select = MetricCollectorLabels.labels.asSelector();

            SapphireObjectServer server = (SapphireObjectServer) oms();

            // acquire sapphire objects based on selector
            ArrayList<AppObjectStub> sapphireStubList = server.acquireSapphireObjectStub(select);

            switch (sapphireStubList.size()) {
                case 0:
                    throw new Exception("Metric server not deployed");
                case 1:
                    break;
                default:
                    throw new Exception("Multiple metric server detected");
            }

            for (AppObjectStub stub : sapphireStubList) {
                collectorStub = (MetricCollector) stub;
            }
        }

        // register metric
        Schema metricCounterSchema =
                new Schema(MetricDMConstants.METRIC_NAME_RPC_COUNTER, config.getMetricLabels());
        // TODO: handle exceptions if metric already registered
        collectorStub.Register(metricCounterSchema);
        rpcCounter =
                CounterClientMetric.newBuilder()
                        .setMetricName(MetricDMConstants.METRIC_NAME_RPC_COUNTER)
                        .setLabels(config.getMetricLabels())
                        .setMetricCollector(collectorStub)
                        .create();

        metricSendTimer =
                new ResettableTimer(
                        new TimerTask() {
                            public void run() {
                                sendMetric();
                            }
                        },
                        config.getMetricUpdateFrequency());
        metricSendTimer.start();
    }

    void incRpcCounter() {
        rpcCounter.incCount();
    }

    private void sendMetric() {
        try {
            if (rpcCounter.modified()) {
                rpcCounter.sendMetric();
                rpcCounter.reset();
            }
        } catch (Exception e) {
            logger.warning(String.format("%s: Sending metric failed", e.toString()));
            return;
        }

        metricSendTimer.reset();
    }

    void stop() {
        metricSendTimer.cancel();
    }
}

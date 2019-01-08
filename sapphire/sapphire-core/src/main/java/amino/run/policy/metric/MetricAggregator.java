package amino.run.policy.metric;

import amino.run.app.SapphireObjectServer;
import amino.run.app.SapphireObjectSpec;
import amino.run.app.labelselector.Labels;
import amino.run.app.labelselector.Selector;
import amino.run.common.AppObjectStub;
import amino.run.common.SapphireObjectID;
import amino.run.common.SapphireReplicaID;
import amino.run.common.Utils;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.oms.OMSServer;
import amino.run.policy.SapphirePolicyUpcalls;
import amino.run.policy.util.ResettableTimer;
import amino.run.sysSapphireObjects.metricCollector.Metric;
import amino.run.sysSapphireObjects.metricCollector.MetricCollector;
import amino.run.sysSapphireObjects.metricCollector.MetricCollectorLabels;
import amino.run.sysSapphireObjects.metricCollector.SendMetric;
import amino.run.sysSapphireObjects.metricCollector.metric.counter.CounterMetric;
import amino.run.sysSapphireObjects.metricCollector.metric.counter.Schema;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

public class MetricAggregator implements Serializable, SendMetric {
    private MetricPolicy.Config config = new MetricPolicy.Config();
    private MetricCollector collectorStub;
    private CounterMetric rpcCounter;
    private transient ResettableTimer metricSendTimer;
    static Logger logger = Logger.getLogger(MetricAggregator.class.getCanonicalName());

    private OMSServer oms() {
        return GlobalKernelReferences.nodeServer.oms;
    }

    public MetricAggregator() {}

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
        Labels metricAggregatorLabels =
                Labels.newBuilder()
                        .add(MetricDMConstants.METRIC_SAPPHIRE_OBJECT_ID, soID.getID().toString())
                        .add(
                                MetricDMConstants.METRIC_SAPPHIRE_REPLICA_ID,
                                soReplicaID.getID().toString())
                        .merge(config.getMetricLabels())
                        .create();

        config.setMetricLabels(metricAggregatorLabels);
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
        collectorStub.register(metricCounterSchema);
        rpcCounter =
                CounterMetric.newBuilder()
                        .setMetricName(MetricDMConstants.METRIC_NAME_RPC_COUNTER)
                        .setLabels(config.getMetricLabels())
                        .setFrequency(config.getMetricUpdateFrequency())
                        .setSendMetric(this)
                        .create();
    }

    void incRpcCounter() {
        rpcCounter.incCount();
    }

    @Override
    public void send(Metric metric) throws Exception {
        collectorStub.push(metric);
    }

    void stop() {
        rpcCounter.stop();
    }
}

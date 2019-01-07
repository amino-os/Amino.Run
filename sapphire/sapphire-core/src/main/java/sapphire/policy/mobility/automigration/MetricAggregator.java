package sapphire.policy.mobility.automigration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
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
import sapphire.sysSapphireObjects.metricCollector.Metric;
import sapphire.sysSapphireObjects.metricCollector.MetricCollector;
import sapphire.sysSapphireObjects.metricCollector.MetricCollectorLabels;
import sapphire.sysSapphireObjects.metricCollector.SendMetric;
import sapphire.sysSapphireObjects.metricCollector.metric.counter.CounterMetric;
import sapphire.sysSapphireObjects.metricCollector.metric.counter.Schema;
import sapphire.sysSapphireObjects.metricCollector.metric.gauge.GaugeMetric;
import sapphire.sysSapphireObjects.metricCollector.metric.gauge.GaugeSchema;

public class MetricAggregator implements Serializable, SendMetric {
    private AutoMigrationPolicy.Config config = new AutoMigrationPolicy.Config();
    private MetricCollector collectorStub;
    private CounterMetric rpcCounter;
    private GaugeMetric executionTime;
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
                    configMap.get(AutoMigrationPolicy.Config.class.getName());
            if (config != null) {
                this.config = ((AutoMigrationPolicy.Config) config);
            }
        }

        // add sapphire object ID and replica ID in metric labels
        Labels metricAggregatorLabels =
                Labels.newBuilder()
                        .add(
                                sapphire.policy.metric.MetricDMConstants.METRIC_SAPPHIRE_OBJECT_ID,
                                soID.getID().toString())
                        .add(
                                sapphire.policy.metric.MetricDMConstants.METRIC_SAPPHIRE_REPLICA_ID,
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

        try {
            Schema schema =
                    new Schema(
                            MetricDMConstants.AUTO_MIGRATION_RPC_COUNTER, config.getMetricLabels());
            // register counter metric
            if (!collectorStub.registered(schema)) {
                collectorStub.register(schema);
            }

            GaugeSchema execSchema =
                    new GaugeSchema(
                            MetricDMConstants.AUTO_MIGRATION_AVG_EXECUTION_TIME,
                            config.getMetricLabels());
            // register execution time metric
            if (!collectorStub.registered(execSchema)) {
                collectorStub.register(execSchema);
            }
        }
        // TODO define Exception for already registered metric
        catch (Exception e) {
            logger.warning("metric already registered");
        }
        // register execution time metric
        rpcCounter =
                CounterMetric.newBuilder()
                        .setMetricName(MetricDMConstants.AUTO_MIGRATION_RPC_COUNTER)
                        .setLabels(config.getMetricLabels())
                        .setFrequency(config.getMetricUpdateFrequency())
                        .setSendMetric(this)
                        .create();

        executionTime =
                GaugeMetric.newBuilder()
                        .setMetricName(MetricDMConstants.AUTO_MIGRATION_AVG_EXECUTION_TIME)
                        .setLabels(config.getMetricLabels())
                        .setFrequency(config.getMetricUpdateFrequency())
                        .setSendMetric(this)
                        .create();
    }

    void incRpcCounter() {
        rpcCounter.incCount();
    }

    Object executionTime(ExecutionTimeInterface exec) throws Exception {
        long startTime = System.nanoTime();
        Object object = exec.execute();
        long endTime = System.nanoTime();
        executionTime.setValue(endTime - startTime);
        return object;
    }

    @Override
    public void send(Metric metric) throws Exception {
        collectorStub.push(metric);
    }

    void stop() {
        rpcCounter.stop();
        executionTime.stop();
    }
}

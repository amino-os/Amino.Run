package amino.run.policy.mobility.automigration;

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
import amino.run.sysSapphireObjects.metricCollector.Metric;
import amino.run.sysSapphireObjects.metricCollector.MetricCollector;
import amino.run.sysSapphireObjects.metricCollector.MetricCollectorLabels;
import amino.run.sysSapphireObjects.metricCollector.SendMetric;
import amino.run.sysSapphireObjects.metricCollector.metric.counter.CounterMetric;
import amino.run.sysSapphireObjects.metricCollector.metric.counter.Schema;
import amino.run.sysSapphireObjects.metricCollector.metric.gauge.GaugeMetric;
import amino.run.sysSapphireObjects.metricCollector.metric.gauge.GaugeSchema;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

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
                                amino.run.policy.metric.MetricDMConstants.METRIC_SAPPHIRE_OBJECT_ID,
                                soID.getID().toString())
                        .add(
                                amino.run.policy.metric.MetricDMConstants
                                        .METRIC_SAPPHIRE_REPLICA_ID,
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

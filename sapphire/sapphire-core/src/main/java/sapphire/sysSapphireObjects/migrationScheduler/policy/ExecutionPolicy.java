package sapphire.sysSapphireObjects.migrationScheduler.policy;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.logging.Logger;
import org.yaml.snakeyaml.Yaml;
import sapphire.app.SapphireObjectServer;
import sapphire.app.labelselector.Selector;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireReplicaID;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.oms.OMSServer;
import sapphire.policy.SapphirePolicy;
import sapphire.policy.mobility.automigration.AutoMigrationPolicy;
import sapphire.policy.mobility.automigration.MetricDMConstants;
import sapphire.policy.util.ResettableTimer;
import sapphire.runtime.EventHandler;
import sapphire.sysSapphireObjects.metricCollector.Metric;
import sapphire.sysSapphireObjects.metricCollector.MetricCollector;
import sapphire.sysSapphireObjects.metricCollector.MetricCollectorLabels;
import sapphire.sysSapphireObjects.metricCollector.MetricSelector;
import sapphire.sysSapphireObjects.metricCollector.metric.gauge.GaugeMetric;
import sapphire.sysSapphireObjects.metricCollector.metric.gauge.GaugeMetricSelector;
import sapphire.sysSapphireObjects.migrationScheduler.MigrationPolicy;

public class ExecutionPolicy implements MigrationPolicy {
    private Config config;
    private MetricCollector collectorStub;
    private transient ResettableTimer metricCollectTimer;
    private transient Logger logger = Logger.getLogger(ExecutionPolicy.class.getName());
    private MetricSelector metricSelector;

    public ExecutionPolicy(String PolicySpec) {
        Yaml yaml = new Yaml();
        config = yaml.loadAs(PolicySpec, Config.class);
        logger.info("Execution policy config : " + config);
    }

    public void start() throws Exception {
        if (collectorStub == null) {
            SapphireObjectServer server = (SapphireObjectServer) oms();
            // use sys label to get deployed metric collector SO.
            // get sys selector
            Selector selector = MetricCollectorLabels.labels.asSelector();
            // acquire sapphire objects based on selector
            ArrayList<AppObjectStub> sapphireStubList = server.acquireSapphireObjectStub(selector);

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

            metricSelector =
                    new GaugeMetricSelector(
                            config.getMetricName(), config.getMetricLabels().asSelector());
        }

        metricCollectTimer =
                new ResettableTimer(
                        new TimerTask() {
                            public void run() {
                                startScheduling();
                                metricCollectTimer.reset();
                            }
                        },
                        config.getMetricCollectFrequency());
        metricCollectTimer.start();

        logger.info("ExecutionPolicy start Success!!!");
    }

    private OMSServer oms() {
        return GlobalKernelReferences.nodeServer.oms;
    }

    private void startScheduling() {
        try {
            ArrayList<Metric> metrics = collectorStub.get(metricSelector);
            // filter top metric to schedule
            Metric topMetric = null;
            for (Metric metric : metrics) {
                if (topMetric == null) {
                    topMetric = metric;
                    continue;
                }
                if (((GaugeMetric) topMetric.getMetric()).getValue()
                        < ((GaugeMetric) metric.getMetric()).getValue()) {
                    topMetric = metric;
                }
            }

            if (topMetric == null) {
                return;
            }
            logger.info("top metric : " + topMetric);
            metricHandler(topMetric);
        } catch (Exception e) {
            logger.warning("Execution migration scheduler failed");
        }
    }

    private void metricHandler(Metric metric) {
        GaugeMetric gaugeMetric = (GaugeMetric) metric;
        UUID sapphireObjectUUID =
                UUID.fromString(
                        gaugeMetric.getLabels().get(MetricDMConstants.METRIC_SAPPHIRE_OBJECT_ID));
        UUID sapphireReplicaUUID =
                UUID.fromString(
                        gaugeMetric.getLabels().get(MetricDMConstants.METRIC_SAPPHIRE_REPLICA_ID));
        SapphireReplicaID sapphireReplicaID =
                new SapphireReplicaID(
                        new SapphireObjectID(sapphireObjectUUID), sapphireReplicaUUID);

        try {
            EventHandler eventHandler = oms().getSapphireReplicaDispatcher(sapphireReplicaID);
            for (Object object : eventHandler.getObjects()) {
                if (object instanceof SapphirePolicy.SapphireServerPolicy
                        && object instanceof AutoMigrationPolicy.AutoMigrationServerPolicy) {
                    AutoMigrationPolicy.AutoMigrationServerPolicy serverPolicy =
                            (AutoMigrationPolicy.AutoMigrationServerPolicy) object;

                    if (gaugeMetric.getValue() > config.getMigrationExecutionThreshold()) {
                        InetSocketAddress currentAddress =
                                oms().lookupKernelObject(serverPolicy.$__getKernelOID());
                        List<InetSocketAddress> kernelServers = oms().getServers(null);

                        // select random server
                        InetSocketAddress address = currentAddress;
                        logger.info("Current address !!!!" + address);
                        for (InetSocketAddress kernelServerAddress : kernelServers) {
                            if (!kernelServerAddress.equals(address)) {
                                address = kernelServerAddress;
                                break;
                            }
                        }
                        logger.info("Updated address !!!!" + address);
                        serverPolicy.migrateObject(address);
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Execution migration scheduler failed !!!!" + e);
        }
    }
}

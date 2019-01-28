package amino.run.sysSapphireObjects.migrationScheduler.policy;

import amino.run.app.SapphireObjectServer;
import amino.run.app.labelselector.Selector;
import amino.run.common.AppObjectStub;
import amino.run.common.SapphireObjectID;
import amino.run.common.SapphireReplicaID;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.oms.OMSServer;
import amino.run.policy.Policy;
import amino.run.policy.metric.MetricDMConstants;
import amino.run.policy.util.ResettableTimer;
import amino.run.runtime.EventHandler;
import amino.run.sysSapphireObjects.metricCollector.Metric;
import amino.run.sysSapphireObjects.metricCollector.MetricCollector;
import amino.run.sysSapphireObjects.metricCollector.MetricCollectorLabels;
import amino.run.sysSapphireObjects.metricCollector.MetricSelector;
import amino.run.sysSapphireObjects.metricCollector.metric.wma.WMAMetric;
import amino.run.sysSapphireObjects.metricCollector.metric.wma.WMAMetricSelector;
import amino.run.sysSapphireObjects.migrationScheduler.MigrationPolicy;
import amino.run.sysSapphireObjects.migrationScheduler.MigrationSchedulerConst;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.logging.Logger;
import org.yaml.snakeyaml.Yaml;

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
                    new WMAMetricSelector(
                            config.getMetricName(), MigrationSchedulerConst.MIGRATION.asSelector());
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
            logger.info(
                    String.format(
                            "Start scheduling with selector %s",
                            MigrationSchedulerConst.MIGRATION.asSelector()));
            ArrayList<Metric> metrics = collectorStub.get(metricSelector);
            // filter top metric to schedule
            Metric topMetric = null;
            for (Metric metric : metrics) {
                if (topMetric == null) {
                    topMetric = metric;
                    continue;
                }
                if (((WMAMetric) topMetric.getMetric()).getValue()
                        < ((WMAMetric) metric.getMetric()).getValue()) {
                    topMetric = metric;
                }
            }

            if (topMetric == null) {
                return;
            }
            logger.info("top metric : " + topMetric);
            metricHandler(topMetric);
        } catch (Exception e) {
            e.printStackTrace();
            logger.warning("Execution migration scheduler failed");
        }
    }

    private void metricHandler(Metric metric) {
        WMAMetric gaugeMetric = (WMAMetric) metric;
        UUID sapphireObjectUUID =
                UUID.fromString(
                        gaugeMetric.getLabels().get(MetricDMConstants.METRIC_SAPPHIRE_OBJECT_ID));
        UUID sapphireReplicaUUID =
                UUID.fromString(
                        gaugeMetric.getLabels().get(MetricDMConstants.METRIC_SAPPHIRE_REPLICA_ID));
        SapphireObjectID sapphireObjectID = new SapphireObjectID(sapphireObjectUUID);
        SapphireReplicaID sapphireReplicaID =
                new SapphireReplicaID(sapphireObjectID, sapphireReplicaUUID);

        try {
            // get group event handler
            EventHandler groupEventHandler = oms().getSapphireObjectDispatcher(sapphireObjectID);
            // get replica Event handler
            EventHandler eventHandler = oms().getSapphireReplicaDispatcher(sapphireReplicaID);

            for (Object object : eventHandler.getObjects()) {
                if (object instanceof Policy.ServerPolicy) {
                    Policy.ServerPolicy serverPolicy = (Policy.ServerPolicy) object;

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

                        ArrayList<Object> eventParams = new ArrayList();
                        eventParams.add(serverPolicy);
                        eventParams.add(address);
                        for (Object gObject : groupEventHandler.getObjects()) {
                            groupEventHandler.notifyEvent(
                                    gObject.getClass(), "Migrate", eventParams);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.warning("Execution migration scheduler failed !!!!" + e);
        }
    }
}

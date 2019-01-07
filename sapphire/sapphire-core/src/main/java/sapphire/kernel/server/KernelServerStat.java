package sapphire.kernel.server;

import java.util.ArrayList;
import java.util.logging.Logger;
import sapphire.app.SapphireObjectServer;
import sapphire.app.labelselector.Labels;
import sapphire.app.labelselector.Selector;
import sapphire.common.AppObjectStub;
import sapphire.kernel.common.ServerInfo;
import sapphire.sysSapphireObjects.metricCollector.Metric;
import sapphire.sysSapphireObjects.metricCollector.MetricCollector;
import sapphire.sysSapphireObjects.metricCollector.MetricCollectorLabels;
import sapphire.sysSapphireObjects.metricCollector.SendMetric;
import sapphire.sysSapphireObjects.metricCollector.metric.gauge.GaugeMetric;
import sapphire.sysSapphireObjects.metricCollector.metric.gauge.GaugeSchema;

public class KernelServerStat extends Thread implements SendMetric {
    public static final String KERNEL_SERVER_MEMORY_STAT = "kernel_server_memory_stat";
    public static final String KERNEL_SERVER_HOST = "host";
    public static final String KERNEL_SERVER_PORT = "port";
    public static final long KERNEL_SERVER_MEMORY_STAT_FREQUENCY = 10000;
    private static Logger logger = Logger.getLogger(KernelServerStat.class.getName());
    private MetricCollector collectorStub;
    private GaugeMetric memoryStat;
    private ServerInfo srvInfo;

    public KernelServerStat(ServerInfo srvInfo) {
        this.srvInfo = srvInfo;
    }

    public void run() {
        if (collectorStub == null) {
            ArrayList<AppObjectStub> sapphireStubList;
            while (true) {
                try {
                    // create selector
                    Selector select = MetricCollectorLabels.labels.asSelector();

                    SapphireObjectServer server = (SapphireObjectServer) KernelServerImpl.oms;

                    // acquire sapphire objects based on selector
                    sapphireStubList = server.acquireSapphireObjectStub(select);
                } catch (Exception e) {
                    logger.warning("");
                    continue;
                }
                switch (sapphireStubList.size()) {
                    case 0:
                        logger.warning("Metric server not deployed");
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    case 1:
                        break;
                    default:
                        logger.warning("Multiple metric server detected");
                        return;
                }

                for (AppObjectStub stub : sapphireStubList) {
                    collectorStub = (MetricCollector) stub;
                }
                break;
            }
        }

        Labels kernelServerMetricLabels =
                Labels.newBuilder()
                        .add(KERNEL_SERVER_HOST, srvInfo.getHost().getHostName())
                        .add(KERNEL_SERVER_PORT, String.valueOf(srvInfo.getHost().getPort()))
                        .create();
        try {
            // register memory stat metric
            GaugeSchema memStatSchema =
                    new GaugeSchema(KERNEL_SERVER_MEMORY_STAT, kernelServerMetricLabels);
            if (!collectorStub.registered(memStatSchema)) {
                collectorStub.register(memStatSchema);
            }
        }
        // TODO define Exception for already registered metric
        catch (Exception e) {
            logger.warning(String.format("metric already registered : %s", e));
        }

        memoryStat =
                GaugeMetric.newBuilder()
                        .setMetricName(KERNEL_SERVER_MEMORY_STAT)
                        .setLabels(kernelServerMetricLabels)
                        .setFrequency(KERNEL_SERVER_MEMORY_STAT_FREQUENCY)
                        .setSendMetric(this)
                        .create();
        logger.warning("Metric client created");
        new MemoryStat().start();
    }

    @Override
    public void send(Metric metric) throws Exception {
        collectorStub.push(metric);
    }

    public class MemoryStat extends Thread {
        public void run() {
            memoryStat.setValue(Runtime.getRuntime().freeMemory());
            while (true) {
                try {
                    Thread.sleep(KERNEL_SERVER_MEMORY_STAT_FREQUENCY / 4);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                memoryStat.setValue(Runtime.getRuntime().freeMemory());
            }
        }
    }
}

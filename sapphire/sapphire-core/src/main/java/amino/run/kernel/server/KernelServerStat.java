package amino.run.kernel.server;

import amino.run.app.SapphireObjectServer;
import amino.run.app.labelselector.Labels;
import amino.run.app.labelselector.Selector;
import amino.run.common.AppObjectStub;
import amino.run.common.SapphireObjectNotFoundException;
import amino.run.common.SystemSapphireObjectStatus;
import amino.run.kernel.common.ServerInfo;
import amino.run.sysSapphireObjects.metricCollector.*;
import amino.run.sysSapphireObjects.metricCollector.metric.gauge.GaugeMetric;
import amino.run.sysSapphireObjects.metricCollector.metric.gauge.GaugeSchema;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class KernelServerStat extends Thread implements SendMetric {
    public static final String KERNEL_SERVER_MEMORY_STAT = "kernel_server_memory_stat";
    public static final String KERNEL_SERVER_HOST = "host";
    public static final String KERNEL_SERVER_PORT = "port";
    public static final long KERNEL_SERVER_MEMORY_STAT_FREQUENCY = 10000;
    private static Logger logger = Logger.getLogger(KernelServerStat.class.getName());
    private MetricCollector collectorStub;
    private GaugeMetric memoryStat;
    private ServerInfo srvInfo;

    KernelServerStat(ServerInfo srvInfo) {
        this.srvInfo = srvInfo;
    }

    public void run() {
        if (!ensureMetricServerDeployed()) {
            logger.warning("Metric server not available");
            return;
        }

        if (collectorStub == null) {
            ArrayList<AppObjectStub> sapphireStubList;
            while (true) {
                try {
                    SapphireObjectServer server = (SapphireObjectServer) KernelServerImpl.oms;
                    // create selector
                    Selector select = MetricCollectorLabels.labels.asSelector();
                    // acquire sapphire objects based on selector
                    sapphireStubList = server.acquireSapphireObjectStub(select);
                } catch (RemoteException e) {
                    logger.warning("Failed to connect OMS");
                    continue;
                } catch (SapphireObjectNotFoundException e) {
                    logger.warning("failed to get Metric server stub");
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
        logger.info("Metric client created");
        new MemoryStat().start();
    }

    private boolean ensureMetricServerDeployed() {
        boolean enabled = false;
        boolean deployed = false;
        int MAX_RETRY = 50;
        int retry = 0;
        List<SystemSapphireObjectStatus> sysSOsStatus;
        while (retry < MAX_RETRY) {
            try {
                sysSOsStatus = KernelServerImpl.oms.getSystemSapphireObjectStatus();
                for (SystemSapphireObjectStatus sysSOStatus : sysSOsStatus) {
                    if (sysSOStatus
                            .getSapphireObjectName()
                            .equals(MetricCollectorConst.SYSTEM_SO_NAME)) {
                        enabled = true;
                        deployed = sysSOStatus.isDeployed();
                    }
                }
                if (!enabled) {
                    logger.warning("Metric server not enabled");
                    return false;
                }

                if (deployed) {
                    return true;
                }

                retry++;
            } catch (RemoteException e) {
                logger.warning("Metric server not deployed");
                e.printStackTrace();
                retry++;
            }

            try {
                Thread.sleep(10000);
            } catch (InterruptedException ie) {
                logger.warning("Sleep time interrupted");
            }
        }
        return false;
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
                    Thread.sleep(KERNEL_SERVER_MEMORY_STAT_FREQUENCY / 2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                memoryStat.setValue(Runtime.getRuntime().freeMemory());
            }
        }
    }
}

package amino.run.kernel.server;

import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.ServerInfo;
import amino.run.kernel.common.metric.*;
import amino.run.kernel.common.metric.schema.GaugeSchema;
import amino.run.kernel.common.metric.type.GaugeMetric;
import amino.run.policy.util.ResettableTimer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * Update Kernel server statistics to Metric server.
 *
 * <p>Reports Kernel server statistics to Metric server.
 */
public class KernelServerStat {
    public static final String KERNEL_SERVER_MEMORY_STAT = "kernel_server_memory_stat";
    public static final String KERNEL_SERVER_HOST = "host";
    public static final String KERNEL_SERVER_PORT = "port";
    public static final long KERNEL_SERVER_MEMORY_STAT_FREQUENCY = 100000;
    private static Logger logger = Logger.getLogger(KernelServerStat.class.getName());
    private ServerInfo srvInfo;
    private MetricClient client = GlobalKernelReferences.metricClient;
    private HashMap<String, String> kernelServerMetricLabels;
    private ResettableTimer timer;
    private ArrayList<MetricCollector> collectors = new ArrayList<MetricCollector>();

    KernelServerStat(ServerInfo srvInfo) {
        this.srvInfo = srvInfo;
    }

    /**
     * Initialize and start kernel server metric collection.
     *
     * @throws Exception
     */
    public void start() throws Exception {
        if (client == null) {
            logger.warning("Metric client not initialize. Skipping kernel server stat monitoring");
            return;
        }

        kernelServerMetricLabels = new HashMap<String, String>();
        kernelServerMetricLabels.put(KERNEL_SERVER_HOST, srvInfo.getHost().getHostName());
        kernelServerMetricLabels.put(
                KERNEL_SERVER_PORT, String.valueOf(srvInfo.getHost().getPort()));

        // Memory stat collection
        MemoryStat memStat = new MemoryStat();
        registerSchema(memStat.getSchema());
        collectors.add(memStat);

        timer =
                new ResettableTimer(
                        new TimerTask() {
                            public void run() {
                                ArrayList<Metric> metrics = new ArrayList<Metric>();
                                Metric metric;
                                try {
                                    for (MetricCollector collector : collectors) {
                                        metric = collector.getMetric();
                                        if (!metric.isEmpty()) {
                                            metrics.add(collector.getMetric());
                                        }
                                    }
                                    client.send(metrics);
                                } catch (Exception e) {
                                    logger.warning(
                                            String.format(
                                                    "%s: Sending metric failed", e.toString()));
                                    return;
                                }
                                // reset the count value and timer after push is done
                                timer.reset();
                            }
                        },
                        KERNEL_SERVER_MEMORY_STAT_FREQUENCY);
        timer.start();
    }

    private class MemoryStat implements MetricCollector {
        @Override
        public Metric getMetric() {
            return new GaugeMetric(
                    KERNEL_SERVER_MEMORY_STAT,
                    kernelServerMetricLabels,
                    Runtime.getRuntime().freeMemory());
        }

        @Override
        public MetricSchema getSchema() {
            return new GaugeSchema(KERNEL_SERVER_MEMORY_STAT, kernelServerMetricLabels);
        }
    }

    private void registerSchema(MetricSchema schema) throws Exception {
        try {
            // register memory stat metric
            if (!client.isRegistered(schema)) {
                client.register(schema);
            }
        } catch (SchemaAlreadyRegistered e) {
            logger.warning(e.getMessage());
        }
    }
}

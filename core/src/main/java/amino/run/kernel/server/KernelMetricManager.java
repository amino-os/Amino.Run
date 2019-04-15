package amino.run.kernel.server;

import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.ServerInfo;
import amino.run.kernel.common.metric.Metric;
import amino.run.kernel.common.metric.MetricManager;
import amino.run.kernel.common.metric.metricHandler.MetricHandler;
import amino.run.kernel.common.metric.schema.Schema;
import amino.run.kernel.common.metric.schema.SchemaType;
import amino.run.kernel.common.metric.type.Gauge;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Update Kernel server statistics to Metric server.
 *
 * <p>Reports Kernel server statistics to Metric server.
 */
public class KernelMetricManager implements MetricManager {
    private static final String KERNEL_SERVER_MEMORY_STAT = "kernel_server_memory_stat";
    private static final String KERNEL_SERVER_HOST = "host";
    private static final String KERNEL_SERVER_PORT = "port";
    private static String hostName = "";
    private static long KERNEL_SERVER_MEMORY_STAT_FREQUENCY = 100000;
    private HashMap<String, String> labels = new HashMap<String, String>();
    private ArrayList<MetricHandler> handlers = new ArrayList<MetricHandler>();

    KernelMetricManager(ServerInfo srvInfo) {
        labels.put(KERNEL_SERVER_HOST, srvInfo.getHost().getHostName());
        labels.put(KERNEL_SERVER_PORT, String.valueOf(srvInfo.getHost().getPort()));
        hostName = srvInfo.getHost().getHostName();
    }

    /** Initialize and start kernel server metric collection. */
    public void start() {
        // Memory stat collection
        MemoryStatHandler memStat = new MemoryStatHandler();
        handlers.add(memStat);

        GlobalKernelReferences.nodeServer.getMetricClient().registerMetricManager(this);
    }

    private class MemoryStatHandler implements MetricHandler {
        Schema schema = new Schema(KERNEL_SERVER_MEMORY_STAT, SchemaType.Gauge);

        @Override
        public Metric getMetric() {
            return new Gauge(schema, Runtime.getRuntime().freeMemory());
        }

        @Override
        public Schema getSchema() {
            return schema;
        }
    }

    @Override
    public ArrayList<Metric> getMetrics() {
        ArrayList<Metric> metrics = new ArrayList<Metric>();

        // collect metric from all metric handlers
        for (MetricHandler handler : handlers) {
            Metric metric = handler.getMetric();
            if (metric != null) {
                metrics.add(metric);
            }
        }
        return metrics;
    }

    @Override
    public long getMetricUpdateFrequency() {
        return (int) KERNEL_SERVER_MEMORY_STAT_FREQUENCY;
    }

    @Override
    public HashMap<String, String> getLabels() {
        return labels;
    }

    @Override
    public String getID() {
        return hostName;
    }

    @Override
    public ArrayList<Schema> getSchemas() {
        ArrayList<Schema> schemas = new ArrayList<Schema>();

        // collect metric for all RPC specific Metric
        for (MetricHandler handler : handlers) {
            Schema schema = handler.getSchema();
            if (schema != null) {
                schemas.add(schema);
            }
        }
        return schemas;
    }
}

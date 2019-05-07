package amino.run.oms.metrics;

import amino.run.kernel.common.metric.NodeMetric;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class KernelServerMetric {
    private static final Logger logger = Logger.getLogger(KernelServerMetric.class.getName());
    private static final int METRIC_WINDOw_SIZE = 4096;
    private InetSocketAddress host;
    private int currentSampleCount = 1;
    private ConcurrentHashMap<InetSocketAddress, NodeMetric> metrics;

    public KernelServerMetric(InetSocketAddress host) {
        this.host = host;
        metrics = new ConcurrentHashMap<InetSocketAddress, NodeMetric>();
    }

    public void updateMetric(Map<InetSocketAddress, NodeMetric> neighbourNodeMetric) {
        long smoothing = 2 / (currentSampleCount + 1);
        for (Map.Entry<InetSocketAddress, NodeMetric> entry : neighbourNodeMetric.entrySet()) {
            NodeMetric newMetric = entry.getValue();
            NodeMetric currentMetric = metrics.get(entry.getKey());
            if (currentMetric != null) {
                /* calculate exponential moving average */
                currentMetric.latency =
                        (newMetric.latency - currentMetric.latency) * smoothing
                                + currentMetric.latency;
                currentMetric.rate =
                        (newMetric.rate - currentMetric.rate) * smoothing + currentMetric.rate;
            }
        }

        if (currentSampleCount == METRIC_WINDOw_SIZE) {
            currentSampleCount = 1;
        }

        logger.info(String.format("Kernel Server Metrics %s", metrics));
    }

    public void clearMetric() {
        metrics.clear();
    }

    public ConcurrentHashMap<InetSocketAddress, NodeMetric> getMetrics() {
        return metrics;
    }
}

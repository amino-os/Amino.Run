package amino.run.oms.metric;

import amino.run.kernel.metric.NodeMetric;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Kernel Server metric class maintains the kernel server metrics relative to other available kernel
 * servers. Metrics include latency and data transfer rate between a particular kernel server to
 * other remote kernel servers.
 */
public class KernelServerMetric {
    private static final Logger logger = Logger.getLogger(KernelServerMetric.class.getName());
    private InetSocketAddress host;
    private int sampleCount;
    private ConcurrentHashMap<InetSocketAddress, NodeMetric> metrics;

    public KernelServerMetric(InetSocketAddress host) {
        this.host = host;
        metrics = new ConcurrentHashMap<InetSocketAddress, NodeMetric>();
    }

    /**
     * Stores the received metrics after calculating exponential weighted moving average to them.
     * Exponential moving average calculation can be found at <a
     * href="EMA">https://en.wikipedia.org/wiki/Moving_average#Exponential_moving_average</a>
     *
     * @param neighbourNodeMetric
     */
    public void updateMetric(Map<InetSocketAddress, NodeMetric> neighbourNodeMetric) {
        /* Exponential weighted moving average formula:
        smoothingFactor = 2/(N + 1)
        EMA(now) = smoothingFactor * (Sample(now) - EMA(prev)) +  EMA(prev) */
        sampleCount = sampleCount > (Integer.MAX_VALUE / 2) ? 1 : sampleCount + 1;
        double smoothingFactor = 2.0 / (sampleCount + 1);
        for (Map.Entry<InetSocketAddress, NodeMetric> entry : neighbourNodeMetric.entrySet()) {
            NodeMetric newMetric = entry.getValue();

            if (newMetric.rate < 0) {
                continue;
            }

            NodeMetric currentMetric = metrics.get(entry.getKey());
            if (currentMetric == null) {
                metrics.put(entry.getKey(), newMetric);
                continue;
            }

            /* Calculate exponential moving average and update existing ones with newly calculated values */
            currentMetric.latency =
                    (long)
                            ((newMetric.latency - currentMetric.latency) * smoothingFactor
                                    + currentMetric.latency);
            currentMetric.rate =
                    (long)
                            ((newMetric.rate - currentMetric.rate) * smoothingFactor
                                    + currentMetric.rate);
        }

        logger.fine(String.format("Kernel Server Metrics %s", metrics));
    }

    public ConcurrentHashMap<InetSocketAddress, NodeMetric> getMetric() {
        return metrics;
    }
}

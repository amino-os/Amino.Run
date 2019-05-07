package amino.run.oms.metric;

import amino.run.common.ReplicaID;
import amino.run.kernel.metric.RPCMetric;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * MicroService metric class maintains the remote procedure call metrics for the replica with
 * respect to each client those RPCs are received from.
 */
public class MicroServiceMetric {
    private static final Logger logger = Logger.getLogger(MicroServiceMetric.class.getName());
    private ReplicaID replicaId;
    private int sampleCount;
    private ConcurrentHashMap<InetSocketAddress, RPCMetric> metrics;

    public MicroServiceMetric(ReplicaID replicaId) {
        this.replicaId = replicaId;
        metrics = new ConcurrentHashMap<InetSocketAddress, RPCMetric>();
    }

    /**
     * Stores the received metrics after calculating exponential weighted moving average to them.
     * Exponential moving average calculation can be found at <a
     * href="EMA">https://en.wikipedia.org/wiki/Moving_average#Exponential_moving_average</a>
     *
     * @param clientMetric
     */
    public void updateMetric(Map<UUID, RPCMetric> clientMetric) {
        /* Exponential weighted moving average formula:
        smoothingFactor = 2/(N + 1)
        EMA(now) = smoothingFactor * (Sample(now) - EMA(prev)) +  EMA(prev) */
        sampleCount = sampleCount > (Integer.MAX_VALUE / 2) ? 1 : sampleCount + 1;
        double smoothingFactor = 2.0 / (sampleCount + 1);
        for (Map.Entry<UUID, RPCMetric> entry : clientMetric.entrySet()) {
            RPCMetric newMetric = entry.getValue();
            RPCMetric currentMetric = metrics.get(newMetric.clientHost);
            if (currentMetric == null) {
                metrics.put(newMetric.clientHost, newMetric);
                continue;
            }

            /* Calculate exponential moving average and update existing ones with newly calculated values */
            currentMetric.dataSize =
                    (long)
                            ((newMetric.dataSize - currentMetric.dataSize) * smoothingFactor
                                    + currentMetric.dataSize);
            currentMetric.elapsedTime =
                    (long)
                            ((newMetric.elapsedTime - currentMetric.elapsedTime) * smoothingFactor
                                    + currentMetric.elapsedTime);
        }

        logger.fine(String.format("Microservice Replica [%s] Metrics %s", replicaId, metrics));
    }

    public ConcurrentHashMap<InetSocketAddress, RPCMetric> getMetric() {
        return metrics;
    }
}

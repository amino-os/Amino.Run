package amino.run.oms.metrics;

import amino.run.common.ReplicaID;
import amino.run.kernel.common.metric.RPCMetric;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class MicroServiceMetric {
    private static final Logger logger = Logger.getLogger(MicroServiceMetric.class.getName());
    private static final int METRIC_WINDOw_SIZE = 4096;
    private ReplicaID replicaId;
    private int currentSampleCount = 1;
    private ConcurrentHashMap<InetSocketAddress, RPCMetric> metrics;

    public MicroServiceMetric(ReplicaID replicaId) {
        this.replicaId = replicaId;
        metrics = new ConcurrentHashMap<InetSocketAddress, RPCMetric>();
    }

    public void updateMetric(Map<UUID, RPCMetric> clientMetric) {
        long smoothing = 2 / (currentSampleCount + 1);
        for (Map.Entry<UUID, RPCMetric> entry : clientMetric.entrySet()) {
            RPCMetric newMetric = entry.getValue();
            RPCMetric currentMetric = metrics.get(newMetric.clientHost);
            if (currentMetric != null) {
                /* calculate exponential moving average */
                currentMetric.dataSize =
                        (newMetric.dataSize - currentMetric.dataSize) * smoothing
                                + currentMetric.dataSize;
                currentMetric.processTime =
                        (newMetric.processTime - currentMetric.processTime) * smoothing
                                + currentMetric.processTime;
            }
        }

        if (currentSampleCount == METRIC_WINDOw_SIZE) {
            currentSampleCount = 1;
        }

        logger.info(String.format("Microservice Replica [%s] Metrics %s", replicaId, metrics));
    }

    public void clearMetric() {
        metrics.clear();
    }

    public ConcurrentHashMap<InetSocketAddress, RPCMetric> getMetrics() {
        return metrics;
    }
}

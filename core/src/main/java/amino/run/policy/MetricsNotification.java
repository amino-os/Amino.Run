package amino.run.policy;

import amino.run.common.Notification;
import amino.run.common.ReplicaID;
import amino.run.kernel.common.metric.RPCMetric;
import java.io.Serializable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Metrics notification class holds the microservice RPC metrics of clients which made the rpc calls
 * to the server policy. Server policy periodically reports the RPC metrics to its group policy.
 * This object is passed as parameter to {@link
 * amino.run.policy.Upcalls.GroupUpcalls#onNotification(Notification)}
 */
public class MetricsNotification implements Notification, Serializable {
    public final ReplicaID replicaId;
    public Map<UUID, RPCMetric> metrics;

    public MetricsNotification(ReplicaID replicaId) {
        this.replicaId = replicaId;
        this.metrics = new ConcurrentHashMap<UUID, RPCMetric>();
    }
}

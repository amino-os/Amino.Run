package amino.run.policy;

import amino.run.common.Notification;
import amino.run.common.ReplicaID;
import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * Migration notification class holds the replicaID and kernel server address as migration
 * notification to group policy. This object is passed as parameter to {@link
 * amino.run.policy.Upcalls.GroupUpcalls#onNotification(Notification)}
 */
public class MigrationNotification implements Notification, Serializable {
    public final ReplicaID replicaId;
    public final InetSocketAddress kernelServer;

    public MigrationNotification(ReplicaID replicaId, InetSocketAddress kernelServer) {
        this.replicaId = replicaId;
        this.kernelServer = kernelServer;
    }
}

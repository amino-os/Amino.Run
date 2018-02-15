package sapphire.policy.scalability;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link IReplicator} that replicates log entries from master
 * to slave synchronously.
 *
 * @author terryz
 */
public class SyncReplicator implements IReplicator<LogEntry> {
    private final Logger logger = Logger.getLogger(SyncReplicator.class.getName());
    private List<LoadBalancedMasterSlavePolicy.ServerPolicy> slaves;

    public SyncReplicator(List<LoadBalancedMasterSlavePolicy.ServerPolicy> slaves) {
        if (slaves == null || slaves.size() ==0) {
            throw new IllegalArgumentException("no slave server available");
        }

        this.slaves = slaves;
    }

    @Override
    public ReplicationResponse replicate(List<LogEntry> entries) {
        // TODO (Terry): Figure out what to store in ReplicationResponse
        ReplicationResponse response = new ReplicationResponse();

        for (LoadBalancedMasterSlavePolicy.ServerPolicy s : slaves) {
            try {
                s.handleSyncReplication(entries);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "failed to replicate log entry {0}: {1}", new Object[]{entries, e});
            }
        }
        return response;
    }
}

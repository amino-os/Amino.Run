package sapphire.policy.scalability;

import java.util.List;

/**
 * @author terryz
 */
public class AsyncReplicator implements IReplicator {
    /**
     * Index of highest log entry that has been successfully replicated to all slaves
     */
    private long replicatedIndex;

    /**
     * For each server, index of the highest log entry that has been replicated to the server
     */
    private long[] matchIndex;

    /**
     * Index of highest log entry that has been applied to
     * {@link sapphire.policy.SapphirePolicy.SapphireServerPolicy#appObject}
     */
    private long lastAppliedIndex;

    @Override
    public ReplicationResponse replicate(List<LogEntry> entries) {
        return null;
    }
}

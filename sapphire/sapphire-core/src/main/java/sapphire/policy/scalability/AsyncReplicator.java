package sapphire.policy.scalability;

import java.util.List;

/**
 * Created by terryz on 2/15/18.
 */

public class AsyncReplicator implements IReplicator {
    /**
     * Index of highest append entry that has been successfully replicated to all slaves
     */
    private long replicatedIndex;

    /**
     * For each server, index of the highest append entry that has been replicated to the server
     */
    private long[] matchIndex;

    /**
     * Index of highest append entry that has been applied to
     * {@link sapphire.policy.SapphirePolicy.SapphireServerPolicy#appObject}
     */
    private long lastAppliedIndex;

    @Override
    public ReplicationResponse replicate(ReplicationRequest request) {
        return null;

    }
}

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
public class SyncReplicator implements IReplicator {
    private final Logger logger = Logger.getLogger(SyncReplicator.class.getName());
    private List<LoadBalancedMasterSlavePolicy.ServerPolicy> slaves;

    public SyncReplicator(List<LoadBalancedMasterSlavePolicy.ServerPolicy> slaves) {
        if (slaves == null || slaves.size() ==0) {
            throw new IllegalArgumentException("no slave server available");
        }

        this.slaves = slaves;
    }

    @Override
    public ReplicationResponse replicate(ReplicationRequest request) {
        // TODO (Terry): handle multiple slaves
        LoadBalancedMasterSlavePolicy.ServerPolicy slave = slaves.get(0);
        ReplicationResponse response = slave.handleReplication(request);

        if (response.getReturnCode() == ReplicationResponse.ReturnCode.TRACEBACK) {
            return handleTraceBack(request);
        }

        return response;
    }

    // TODO (Terry): Implement handleTraceback
    private ReplicationResponse handleTraceBack(ReplicationRequest request) {
        return null;
    }
}

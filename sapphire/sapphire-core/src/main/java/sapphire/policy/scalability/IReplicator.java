package sapphire.policy.scalability;

import java.util.List;

/**
 * @author terryz
 */
public interface IReplicator {
    ReplicationResponse replicate(ReplicationRequest request);
}

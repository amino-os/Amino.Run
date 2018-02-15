package sapphire.policy.scalability;

import java.util.List;

/**
 * @author terryz
 */
public interface IReplicator<T> {
    ReplicationResponse replicate(List<T> entries);
}

package sapphire.policy.scalability;

/**
 * @author terryz
 */
public interface IReplicator {
    ReplicateResponse replicate(ReplicateRequest request);
}

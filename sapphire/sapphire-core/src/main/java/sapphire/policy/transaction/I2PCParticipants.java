package sapphire.policy.transaction;

import java.util.Collection;

import static sapphire.policy.SapphirePolicy.SapphireClientPolicy;

/**
 * abstraction of collection of 2PC participants
 */
public interface I2PCParticipants {
    /**
     * registers as participant of the transaction
     * @param cohort the client proxy of the Sapphire object that involves in transaction
     */
    public void register(SapphireClientPolicy cohort);

    /**
     * gets all participants of the transaction registered
     * @return the collection of participating client proxies
     */
    public Collection<SapphireClientPolicy> getParticipants();
}

package amino.run.policy.transaction;

import static amino.run.policy.SapphirePolicy.SapphireClientPolicy;

import java.util.Collection;

/** abstraction of collection of 2PC participants */
public interface TwoPCParticipants {
    /**
     * registers as participant of the transaction
     *
     * @param cohort the client proxy of the Sapphire object that involves in transaction
     */
    public void register(SapphireClientPolicy cohort);

    /**
     * gets all participants of the transaction registered
     *
     * @return the collection of participating client proxies
     */
    public Collection<SapphireClientPolicy> getRegistered();
}

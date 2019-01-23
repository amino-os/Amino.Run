package amino.run.policy.transaction;

import static amino.run.policy.SapphirePolicy.ClientPolicy;

import java.util.Collection;

/** abstraction of collection of 2PC participants */
public interface TwoPCParticipants {
    /**
     * registers as participant of the transaction
     *
     * @param cohort the client proxy of the Sapphire object that involves in transaction
     */
    public void register(ClientPolicy cohort);

    /**
     * gets all participants of the transaction registered
     *
     * @return the collection of participating client proxies
     */
    public Collection<ClientPolicy> getRegistered();
}

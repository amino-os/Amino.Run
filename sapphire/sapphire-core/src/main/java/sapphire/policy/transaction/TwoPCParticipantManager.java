package sapphire.policy.transaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static sapphire.policy.SapphirePolicy.SapphireClientPolicy;

/**
 * DCAP transaction participant manager
 */
public class TwoPCParticipantManager implements TwoPCParticipants {
    private Set participants = new ConcurrentHashMap<SapphireClientPolicy, Object>().keySet();

    @Override
    public void register(SapphireClientPolicy cohort) {
        if (!this.participants.contains(cohort)) {
            this.participants.add(cohort);
        }
    }

    @Override
    public Collection<SapphireClientPolicy> getRegistered() {
        return new ArrayList<SapphireClientPolicy>(this.participants);
    }
}

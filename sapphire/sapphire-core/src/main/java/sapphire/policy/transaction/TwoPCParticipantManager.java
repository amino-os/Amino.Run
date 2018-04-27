package sapphire.policy.transaction;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static sapphire.policy.SapphirePolicy.SapphireClientPolicy;

/**
 * DCAP transaction participant manager
 */
public class TwoPCParticipantManager implements TwoPCParticipants, Serializable {
    private Set participants = Collections.newSetFromMap(new ConcurrentHashMap<SapphireClientPolicy, Boolean>());

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

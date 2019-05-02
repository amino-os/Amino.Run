package amino.run.policy.transaction;

import amino.run.policy.Policy;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** transaction participant manager */
public class TwoPCParticipantManager implements TwoPCParticipants, Serializable {
    private Set participants =
            Collections.newSetFromMap(new ConcurrentHashMap<Policy.ClientPolicy, Boolean>());

    @Override
    public void register(Policy.ClientPolicy cohort) {
        if (!this.participants.contains(cohort)) {
            this.participants.add(cohort);
        }
    }

    @Override
    public Collection<Policy.ClientPolicy> getRegistered() {
        return new ArrayList<Policy.ClientPolicy>(this.participants);
    }
}

package sapphire.policy.transaction;

import sapphire.policy.SapphirePolicy;
import sapphire.policy.SapphirePolicy.SapphireClientPolicy;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * type to keep track of participants of transactions
 */
public class TwoPCLocalParticipants {
    private final ConcurrentHashMap<UUID, TwoPCParticipants> localParticipants = new ConcurrentHashMap<>();

    public Collection<SapphireClientPolicy> getParticipants(UUID transactionId) {
        return this.localParticipants.get(transactionId).getRegistered();
    }

    public void addParticipants(UUID transactionId, Collection<SapphireClientPolicy> participants) {
        if (!this.localParticipants.containsKey(transactionId)) {
            this.localParticipants.put(transactionId, new TwoPCParticipantManager());
        }

        TwoPCParticipants participantManager = this.localParticipants.get(transactionId);
        participants.forEach(participantManager::register);
    }

    public void cleanup(UUID transactionId) {
        this.localParticipants.remove(transactionId);
    }
}

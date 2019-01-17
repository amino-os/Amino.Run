package amino.run.policy.transaction;

import static amino.run.policy.Policy.ClientPolicy;

import amino.run.policy.Policy;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** type to keep track of participants of transactions */
public class TwoPCLocalParticipants implements Serializable {
    private final ConcurrentHashMap<UUID, TwoPCParticipants> localParticipants =
            new ConcurrentHashMap<UUID, TwoPCParticipants>();

    public TwoPCParticipants getParticipantManager(UUID transactionId) {
        if (!this.localParticipants.containsKey(transactionId)) {
            this.localParticipants.put(transactionId, new TwoPCParticipantManager());
        }

        return this.localParticipants.get(transactionId);
    }

    public Collection<ClientPolicy> getParticipants(UUID transactionId) {
        return this.getParticipantManager(transactionId).getRegistered();
    }

    public void addParticipants(UUID transactionId, Collection<ClientPolicy> participants) {
        TwoPCParticipants participantManager = this.getParticipantManager(transactionId);
        for (Policy.ClientPolicy participant : participants) {
            participantManager.register(participant);
        }
    }

    public void cleanup(UUID transactionId) {
        this.localParticipants.remove(transactionId);
    }

    /**
     * sends out 2PC protocol primitives to all registered participants; no responses collected
     *
     * @param transactionId the transaction all the participants are in
     * @param primitiveMethod the name of promitive
     * @throws TransactionExecutionException exception of RPC execution
     */
    public void fanOutTransactionPrimitive(UUID transactionId, String primitiveMethod)
            throws TransactionExecutionException {
        ArrayList<Object> paramsTX =
                TransactionWrapper.getTransactionRPCParams(transactionId, primitiveMethod, null);
        TransactionContext.enterTransaction(
                transactionId, this.getParticipantManager(transactionId));

        try {
            // todo: consider in parallel requests
            // todo: make sure the transaction context would be properly propagated in such case
            for (ClientPolicy p : this.getParticipants(transactionId)) {
                if (TransactionContext.getProcessedClients().contains(p)) {
                    continue;
                }

                TransactionContext.getProcessedClients().add(p);

                try {
                    p.onRPC(TransactionWrapper.txWrapperTag, paramsTX);
                } catch (Exception e) {
                    throw new TransactionExecutionException(
                            "DCAP 2PC transaction exception: " + primitiveMethod, e);
                }
            }
        } finally {
            TransactionContext.leaveTransaction();
        }
    }

    /**
     * collects votes from all the registered 2PC participants of a specific transaction
     *
     * @param transactionId id of the transaction
     * @return true if all participants voted yes; otherwise false
     * @throws TransactionExecutionException error happened while getting the votes
     */
    public Boolean allParticipantsVotedYes(UUID transactionId)
            throws TransactionExecutionException {
        ArrayList<Object> paramsVoteReq =
                TransactionWrapper.getTransactionRPCParams(
                        transactionId, TwoPCPrimitive.VoteReq, null);
        TransactionContext.enterTransaction(
                transactionId, this.getParticipantManager(transactionId));

        try {
            // todo: consider in parallel requests
            // todo: make sure the transaction context would be properly propagated in such case
            boolean isAllYes = true;
            for (ClientPolicy p : this.getParticipants(transactionId)) {
                if (TransactionContext.getProcessedClients().contains(p)) {
                    continue;
                }

                try {
                    TransactionContext.getProcessedClients().add(p);
                    Object vote = p.onRPC(TransactionWrapper.txWrapperTag, paramsVoteReq);
                    if (!TransactionManager.Vote.YES.equals(vote)) {
                        isAllYes = false;
                        break;
                    }
                } catch (Exception e) {
                    throw new TransactionExecutionException(
                            "DCAP 2PC transaction exception: vote_req", e);
                }
            }

            return isAllYes;
        } finally {
            TransactionContext.leaveTransaction();
        }
    }
}

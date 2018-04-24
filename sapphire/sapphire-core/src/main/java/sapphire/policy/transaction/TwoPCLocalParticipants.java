package sapphire.policy.transaction;

import sapphire.policy.SapphirePolicy;
import sapphire.policy.SapphirePolicy.SapphireClientPolicy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * type to keep track of participants of transactions
 */
public class TwoPCLocalParticipants {
    private final ConcurrentHashMap<UUID, TwoPCParticipants> localParticipants = new ConcurrentHashMap<UUID, TwoPCParticipants>();

    public TwoPCParticipants getParticipantManager(UUID transactionId) {
        if (!this.localParticipants.containsKey(transactionId)) {
            this.localParticipants.put(transactionId, new TwoPCParticipantManager());
        }

        return this.localParticipants.get(transactionId);
    }

    public Collection<SapphireClientPolicy> getParticipants(UUID transactionId) {
        return this.getParticipantManager(transactionId).getRegistered();
    }

    public void addParticipants(UUID transactionId, Collection<SapphireClientPolicy> participants) {
        TwoPCParticipants participantManager = this.getParticipantManager(transactionId);
        for (SapphireClientPolicy participant: participants) {
            participantManager.register(participant);
        }
    }

    public void cleanup(UUID transactionId) {
        this.localParticipants.remove(transactionId);
    }

    /**
     * sends out 2PC protocol primitives to all registered participants; no responses collected
     * @param transactionId the transaction all the participants are in
     * @param primitiveMethod the name of promitive
     * @throws TransactionExecutionException exception of RPC execution
     */
    public void fanOutTransactionPrimitive(UUID transactionId, String primitiveMethod) throws TransactionExecutionException {
        ArrayList<Object> paramsTX = TransactionWrapper.getTransactionRPCParams(transactionId, primitiveMethod, null);
        TransactionContext.enterTransaction(transactionId, this.getParticipantManager(transactionId));

        // todo: consider in parallel requests
        // todo: make sure the transaction context would be properly propagated in such case
        for (SapphirePolicy.SapphireClientPolicy p: this.getParticipants(transactionId))
        {
            try {
                p.onRPC(TransactionWrapper.txWrapperTag, paramsTX);
            } catch (Exception e) {
                throw new TransactionExecutionException("DCAP 2PC transaction exception: " + primitiveMethod, e);
            }
        }

        TransactionContext.leaveTransaction();
    }

    /**
     * collects votes from all the registered 2PC participants of a specific transaction
     * @param transactionId id of the transaction
     * @return true if all participants voted yes; otherwise false
     * @throws TransactionExecutionException error happened while getting the votes
     */
    public Boolean allParticipantsVotedYes(UUID transactionId) throws TransactionExecutionException {
        ArrayList<Object> paramsVoteReq =  TransactionWrapper.getTransactionRPCParams(transactionId, TwoPCPrimitive.VoteReq, null);
        TransactionContext.enterTransaction(transactionId, this.getParticipantManager(transactionId));

        // todo: consider in parallel requests
        // todo: make sure the transaction context would be properly propagated in such case
        boolean isAllYes = true;
        for (SapphirePolicy.SapphireClientPolicy p: this.getParticipants(transactionId)){
            try {
                Object vote = p.onRPC(TransactionWrapper.txWrapperTag, paramsVoteReq);
                if (!TransactionManager.Vote.YES.equals(vote)) {
                    isAllYes = false;
                    break;
                }
            } catch (Exception e) {
                throw new TransactionExecutionException("DCAP 2PC transaction exception: vote_req", e);
            }
        }

        TransactionContext.leaveTransaction();
        return isAllYes;
    }
}

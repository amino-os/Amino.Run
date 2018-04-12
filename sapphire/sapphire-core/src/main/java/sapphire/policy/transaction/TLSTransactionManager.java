package sapphire.policy.transaction;

import java.util.ArrayList;
import java.util.UUID;

import static sapphire.policy.transaction.TwoPCLocalStatus.LocalStatus;

/**
 * Transaction Manager based on thread local storage context
 */
public class TLSTransactionManager implements TransactionManager {
    private TwoPCLocalStatus localStatusManager = new TwoPCLocalStatus();
    private TwoPCLocalParticipants localParticipantsManager = new TwoPCLocalParticipants();

    // tets hook
    void setLocalParticipantsManager(TwoPCLocalParticipants participantsManager) {
        this.localParticipantsManager = participantsManager;
    }

    // test hook
    void setLocalStatusManager(TwoPCLocalStatus statusManager) {
        this.localStatusManager = statusManager;
    }

    /**
     * ensures the effective transaction
     * @param transactionId id of the effective transaction
     */
    @Override
    public void join(UUID transactionId) {
        TransactionContext.enterTransaction(transactionId);
    }

    /**
     * ensures to clean slate on leaving the transaction
     * @param transactionId id of the effective transaction
     */
    @Override
    public void leave(UUID transactionId) {
        TwoPCParticipants tlsParticipants = TransactionContext.getParticipants();
        this.localParticipantsManager.addParticipants(transactionId, tlsParticipants.getRegistered());
        TransactionContext.leaveTransaction();
    }

    /**
     * responds the vote_req as required by 2PC protocol
     * @param transactionId id of the effective transaction
     * @return the vote based on that known of local and participants
     */
    @Override
    public Vote vote(UUID transactionId) {
        LocalStatus status = this.localStatusManager.getStatus(transactionId);

        switch (status) {
            case YESVOTED:
                return Vote.YES;
            case NOVOTED:
                return Vote.NO;
            case BAD:
            case UNCERTAIN:
                this.localStatusManager.setStatus(transactionId, LocalStatus.NOVOTED);
                return Vote.NO;
            case GOOD:
                if (this.allParticipantsVotedYes(transactionId)) {
                    this.localStatusManager.setStatus(transactionId, LocalStatus.YESVOTED);
                    return Vote.YES;
                } else {
                    this.localStatusManager.setStatus(transactionId, LocalStatus.NOVOTED);
                    return Vote.NO;
                }
            default:
                throw new IllegalStateException("illegal 2PC local status: " + status.toString());
        }
    }

    @Override
    public void commit(UUID transactionId) {
        this.localStatusManager.setStatus(transactionId, LocalStatus.COMMITTED);
        this.fanOutTransactionPrimitive(transactionId, TwoPCPrimitive.Commit);
        this.localParticipantsManager.cleanup(transactionId);
    }

    @Override
    public void abort(UUID transactionId) {
        this.localStatusManager.setStatus(transactionId, LocalStatus.ABORTED);
        this.fanOutTransactionPrimitive(transactionId, TwoPCPrimitive.Abort);
        this.localParticipantsManager.cleanup(transactionId);
    }

    private void fanOutTransactionPrimitive(UUID transactionId, String primitiveMethod) {
        ArrayList<Object> paramsTX = this.genTwoPCPrimitiveParams(transactionId, primitiveMethod);
        TransactionContext.enterTransaction(transactionId);

        // todo: consider in parallel requests
        this.localParticipantsManager.getParticipants(transactionId).stream().forEach(p -> {
            try {
                p.onRPC(TransactionWrapper.txWrapperTag, paramsTX);
            } catch (Exception e) {
                throw new RuntimeException("DCAP 2PC transaction exception: " + primitiveMethod, e);
            }
        });

        TransactionContext.leaveTransaction();
    }

    private boolean allParticipantsVotedYes(UUID transactionId) {
        ArrayList<Object> paramsVoteReq = this.genTwoPCPrimitiveParams(transactionId, TwoPCPrimitive.VoteReq);
        TransactionContext.enterTransaction(transactionId);

        // todo: consider in parallel requests
        boolean allYes = this.localParticipantsManager.getParticipants(transactionId).stream().map(p -> {
            try {
                return p.onRPC(TransactionWrapper.txWrapperTag, paramsVoteReq);
            } catch (Exception e) {
                throw new RuntimeException("DCAP 2PC transaction exception: vote_req", e);
            }
        }).allMatch(v -> v.equals(Vote.YES));

        TransactionContext.leaveTransaction();
        return allYes;
    }

    private ArrayList<Object> genTwoPCPrimitiveParams(UUID transactionId, String primitive) {
        TransactionWrapper txRPC = new TransactionWrapper(transactionId, primitive, null);
        return txRPC.getRPCParams();
    }
}

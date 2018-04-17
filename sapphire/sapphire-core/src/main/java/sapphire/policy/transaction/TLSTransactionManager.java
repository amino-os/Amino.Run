package sapphire.policy.transaction;

import sapphire.policy.SapphirePolicy;

import javax.transaction.TransactionRolledbackException;
import java.util.ArrayList;
import java.util.UUID;

import static sapphire.policy.transaction.TwoPCLocalStatus.LocalStatus;

/**
 * Transaction Manager based on thread local storage context
 */
public class TLSTransactionManager implements TransactionManager {
    private final TwoPCLocalStatus localStatusManager = new TwoPCLocalStatus();
    private final TwoPCLocalParticipants localParticipantsManager = new TwoPCLocalParticipants();
    private TransactionValidator validator;

    /**
     * sets the transaction validator object
     * @param validator
     */
    public void setValidator(TransactionValidator validator) {
        this.validator = validator;
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
    public Vote vote(UUID transactionId) throws TransactionExecutionException {
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
                    return this.getVoteOnLocalValidation(transactionId);
                } else {
                    this.localStatusManager.setStatus(transactionId, LocalStatus.NOVOTED);
                    return Vote.NO;
                }
            default:
                throw new IllegalStateException("illegal 2PC local status: " + status.toString());
        }
    }

    private Vote getVoteOnLocalValidation(UUID transactionId) {
        try {
            if (this.validator.promises(transactionId)) {
                this.localStatusManager.setStatus(transactionId, LocalStatus.YESVOTED);
                return Vote.YES;
            } else {
                this.localStatusManager.setStatus(transactionId, LocalStatus.NOVOTED);
                return Vote.NO;
            }
        } catch (Exception e) {
            this.localStatusManager.setStatus(transactionId, LocalStatus.NOVOTED);
            return Vote.NO;
        }
    }

    @Override
    public void commit(UUID transactionId) {
        this.localStatusManager.setStatus(transactionId, LocalStatus.COMMITTED);
        this.validator.onCommit(transactionId);
        try {
            this.fanOutTransactionPrimitive(transactionId, TwoPCPrimitive.Commit);
        } catch (TransactionExecutionException e) {
            // todo: proper error handling - commit itself should always succeed
        }
        this.localParticipantsManager.cleanup(transactionId);
    }

    @Override
    public void abort(UUID transactionId) {
        this.localStatusManager.setStatus(transactionId, LocalStatus.ABORTED);
        this.validator.onAbort(transactionId);
        try {
            this.fanOutTransactionPrimitive(transactionId, TwoPCPrimitive.Abort);
        } catch (TransactionExecutionException e) {
            // todo: proper error handling - abort itself should always succeed
        }
        this.localParticipantsManager.cleanup(transactionId);
    }

    private void fanOutTransactionPrimitive(UUID transactionId, String primitiveMethod) throws TransactionExecutionException {
        ArrayList<Object> paramsTX = TransactionWrapper.getTransactionRPCParams(transactionId, primitiveMethod, null);
        TransactionContext.enterTransaction(transactionId);

        // todo: consider in parallel requests
        // todo: make sure the transaction context would be properly propagated in such case
        for (SapphirePolicy.SapphireClientPolicy p: this.localParticipantsManager.getParticipants(transactionId))
        {
            try {
                p.onRPC(TransactionWrapper.txWrapperTag, paramsTX);
            } catch (Exception e) {
                throw new TransactionExecutionException("DCAP 2PC transaction exception: " + primitiveMethod, e);
            }
        }

        TransactionContext.leaveTransaction();
    }

    private boolean allParticipantsVotedYes(UUID transactionId) throws TransactionExecutionException {
        ArrayList<Object> paramsVoteReq =  TransactionWrapper.getTransactionRPCParams(transactionId, TwoPCPrimitive.VoteReq, null);
        TransactionContext.enterTransaction(transactionId);

        // todo: consider in parallel requests
        // todo: make sure the transaction context would be properly propagated in such case
        boolean isAllYes = true;
        for (SapphirePolicy.SapphireClientPolicy p: this.localParticipantsManager.getParticipants(transactionId)){
            try {
                Object vote = p.onRPC(TransactionWrapper.txWrapperTag, paramsVoteReq);
                if (!vote.equals(Vote.YES)) {
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

package sapphire.policy.transaction;

import static sapphire.policy.transaction.TwoPCLocalStatus.LocalStatus;

import java.io.Serializable;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Transaction Manager based on thread local storage context */
public class TLSTransactionManager implements TransactionManager, Serializable {
    private static Logger logger =
            Logger.getLogger("sapphire.policy.transaction.TLSTransactionManager");

    private final TwoPCLocalStatus localStatusManager = new TwoPCLocalStatus();
    private final TwoPCLocalParticipants localParticipantsManager = new TwoPCLocalParticipants();
    private TransactionValidator validator;

    /**
     * sets the transaction validator object
     *
     * @param validator
     */
    public void setValidator(TransactionValidator validator) {
        this.validator = validator;
    }

    /**
     * ensures the effective transaction
     *
     * @param transactionId id of the effective transaction
     */
    @Override
    public void join(UUID transactionId) {
        TwoPCParticipants participants =
                this.localParticipantsManager.getParticipantManager(transactionId);
        TransactionContext.enterTransaction(transactionId, participants);
        // todo: set the default status UNCERTAIN instead - we may need a way
        // to signal proc finished without issue or not
        this.localStatusManager.setStatus(transactionId, LocalStatus.GOOD);
    }

    /**
     * ensures to clean slate on leaving the transaction
     *
     * @param transactionId id of the effective transaction
     */
    @Override
    public void leave(UUID transactionId) {
        TwoPCParticipants tlsParticipants = TransactionContext.getParticipants();
        this.localParticipantsManager.addParticipants(
                transactionId, tlsParticipants.getRegistered());
        TransactionContext.leaveTransaction();
    }

    /**
     * responds the vote_req as required by 2PC protocol
     *
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
                if (this.isLocalPromised(transactionId)
                        && this.localParticipantsManager.allParticipantsVotedYes(transactionId)) {
                    this.localStatusManager.setStatus(transactionId, LocalStatus.YESVOTED);
                    return Vote.YES;
                } else {
                    // todo: consider breaking the promise if made before right now
                    this.localStatusManager.setStatus(transactionId, LocalStatus.NOVOTED);
                    return Vote.NO;
                }
            default:
                throw new IllegalStateException("illegal 2PC local status: " + status.toString());
        }
    }

    private Boolean isLocalPromised(UUID transactionId) {
        try {
            return this.validator.promises(transactionId);
        } catch (Exception e) {
            // todo: pass the exception detail to client
            logger.log(Level.SEVERE, "local 2PC preparation failed", e);
            return false;
        }
    }

    @Override
    public void commit(UUID transactionId) {
        this.localStatusManager.setStatus(transactionId, LocalStatus.COMMITTED);
        this.validator.onCommit(transactionId);
        try {
            this.localParticipantsManager.fanOutTransactionPrimitive(
                    transactionId, TwoPCPrimitive.Commit);
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
            this.localParticipantsManager.fanOutTransactionPrimitive(
                    transactionId, TwoPCPrimitive.Abort);
        } catch (TransactionExecutionException e) {
            // todo: proper error handling - abort itself should always succeed
        }
        this.localParticipantsManager.cleanup(transactionId);
    }
}

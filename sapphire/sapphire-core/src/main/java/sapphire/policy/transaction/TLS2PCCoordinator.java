package sapphire.policy.transaction;

import sapphire.policy.serializability.TransactionAlreadyStartedException;

import java.util.UUID;

/**
 * 2PC Coordinator based on thread local storage
 */
public class TLS2PCCoordinator implements TwoPCCoordinator{
    private final TransactionValidator validator;
    private final TwoPCLocalParticipants localParticipantsManager = new TwoPCLocalParticipants();

    public TLS2PCCoordinator(TransactionValidator validator) {
        this.validator = validator;
    }

    @Override
    public void beginTransaction() throws TransactionAlreadyStartedException {
        if (TransactionContext.getCurrentTransaction() != null) {
            throw new TransactionAlreadyStartedException("nested transaction is unsupported.");
        }

        // todo: ensure participants of this transaction is properly newed (in separate PR soon)
        TransactionContext.enterTransaction(UUID.randomUUID());
    }

    @Override
    public UUID getTransactionId() {
        return TransactionContext.getCurrentTransaction();
    }

    @Override
    public void join(UUID transactionId) throws TransactionAlreadyStartedException {
        UUID currentTransactionId = TransactionContext.getCurrentTransaction();
        if (!(transactionId.equals(currentTransactionId))) {
            String message = String.format("already in transaction %s; illegal to join %s.", currentTransactionId.toString(), transactionId.toString());
            throw new TransactionAlreadyStartedException(message);
        }
    }

    @Override
    public void leave(UUID transactionId) {
        this.localParticipantsManager.cleanup(transactionId);
        TransactionContext.leaveTransaction();
    }

    @Override
    public Vote vote(UUID transactionId) throws TransactionExecutionException {
        try {
            if (!this.validator.promises(transactionId)) {
                return Vote.NO;
            }
        } catch (Exception e) {
            // todo: expose the error detail
            return Vote.NO;
        }

        if (this.localParticipantsManager.allParticipantsVotedYes(transactionId)) {
            return Vote.YES;
        } else {
            // todo: consider breaking the local promise right now
            return Vote.NO;
        }
    }

    @Override
    public void commit(UUID transactionId) {
        this.validator.onCommit(transactionId);
        try {
            this.localParticipantsManager.fanOutTransactionPrimitive(transactionId, TwoPCPrimitive.Commit);
        } catch (TransactionExecutionException e) {
            // todo: proper error handling - commit itself should always succeed
        }

        this.leave(transactionId);
    }

    @Override
    public void abort(UUID transactionId) {
        this.validator.onAbort(transactionId);
        try {
            this.localParticipantsManager.fanOutTransactionPrimitive(transactionId, TwoPCPrimitive.Abort);
        } catch (TransactionExecutionException e) {
            // todo: proper error handling - abort itself should always succeed
        }

        this.leave(transactionId);
    }
}

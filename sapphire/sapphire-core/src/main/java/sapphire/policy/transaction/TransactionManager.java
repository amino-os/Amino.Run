package sapphire.policy.transaction;

import sapphire.policy.serializability.TransactionAlreadyStartedException;

import java.util.UUID;

/**
 * abstraction of transaction status management
 */
public interface TransactionManager {
    /**
     * type of vote response as defined in 2PC protocol
     */
    enum Vote {
        UNCERTIAN, YES, NO,
    }

    /**
     * joins the transaction
      * @param transactionId id of the effective transaction
     */
    void join(UUID transactionId) throws TransactionAlreadyStartedException;

    /**
     * leaves the transaction while the transaction is still active/effective
     * @param transactionId id of the effective transaction
     */
    void leave(UUID transactionId);

    /**
     * reports the vote of the effective transaction based on local status
     * @param transactionId id of the effective transaction
     * @return
     */
    Vote vote(UUID transactionId) throws TransactionExecutionException;

    /**
     * commits the transaction
     * @param transactionId id of the effective transaction
     */
    void commit(UUID transactionId);

    /**
     * aborts the transaction
     * @param transactionId id of the effective transaction
     */
    void abort(UUID transactionId);
}

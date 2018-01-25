package sapphire.policy.serializability;

/**
 * Created by quinton on 1/22/18.
 * This interface must be implemented by all SO's that use LockingTransactionPolicy.
 * A convenience implementation (LockingTransactionImpl) is provided, so that SO's
 * can just extend that.
 */

public interface LockingTransaction {
    /**
     * Start a transaction with the default timeout.
     * @throws Exception
     */
    public void startTransaction() throws Exception;

    /**
     * Start a transaction with a specified timeout.
     * An exclusive lock is held on the server until the transaction is rolled back, committed, or the timeout expires.
     * @param timeoutMillisec
     */
    public void startTransaction(long timeoutMillisec);
    public void commitTransaction() throws Exception;
    public void rollbackTransaction() throws Exception;
}

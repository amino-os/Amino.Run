package sapphire.policy.serializability;

/**
 * Created by quinton on 1/22/18.
 * This interface must be implemented by all SO's that use LockingTransactionPolicy.
 * A convenience implementation (LockingTransactionImpl) is provided, so that SO's
 * can just extend that.
 */

public interface LockingTransaction {
    public void startTransaction() throws Exception;
    public void commitTransaction() throws Exception;
    public void rollbackTransaction() throws Exception;
}

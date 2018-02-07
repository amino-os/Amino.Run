package sapphire.policy.serializability;

/**
 * Created by Venugopal Reddy K 00900280 on 1/2/18.
 * This interface must be implemented by all SO's that use OptConcurrentTransactionPolicy.
 * A convenience implementation (OptConcurrentTransactionImpl) is provided, so that SO's
 * can just extend that.
 */

public interface OptConcurrentTransaction {
    /**
     * Start a transaction
     * @throws Exception
     **/
    public void startTransaction() throws Exception;
    /**
     * Commit a transaction
     * @throws Exception
     **/
    public void commitTransaction() throws Exception;
    /**
     * Rollback transaction
     * @throws Exception
     **/
    public void rollbackTransaction() throws Exception;
}

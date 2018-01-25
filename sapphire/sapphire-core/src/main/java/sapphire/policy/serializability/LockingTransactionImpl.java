package sapphire.policy.serializability;

import java.io.Serializable;

import sapphire.policy.checkpoint.explicitcheckpoint.ExplicitCheckpointer;

/**
 * Created by quinton on 1/22/18.
 */

public class LockingTransactionImpl implements LockingTransaction, Serializable {
    @Override
    public void startTransaction() throws Exception {}

    @Override
    public void startTransaction(long timeoutMillisec) {}

    @Override
    public void commitTransaction() throws Exception {}

    @Override
    public void rollbackTransaction() throws Exception {}
}

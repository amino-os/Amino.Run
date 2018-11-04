package sapphire.policy.serializability;

/** Created by quinton on 1/22/18. */
public class LockingTransactionImpl extends TransactionImpl implements LockingTransaction {
    @Override
    public void startTransaction(long timeoutMillisec)
            throws TransactionAlreadyStartedException, TransactionException {}
}

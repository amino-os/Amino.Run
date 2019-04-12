package amino.run.policy.serializability;

/**
 * Created by Venugopal Reddy K 00900280 on 1/2/18. A convenience implementation (TransactionImpl)
 * is provided, so that SOs can just extend it.
 */
public class TransactionImpl implements Transaction {

    @Override
    public void startTransaction()
            throws TransactionAlreadyStartedException, TransactionException {}

    @Override
    public void commitTransaction() throws NoTransactionStartedException, TransactionException {}

    @Override
    public void rollbackTransaction() throws NoTransactionStartedException, TransactionException {}
}

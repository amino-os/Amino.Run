package sapphire.policy.serializability;

/**
 * Created by Venugopal Reddy K 00900280 on 1/2/18. A convenience implementation (TransactionImpl)
 * is provided, so that SO's can just extend it.
 */
public class TransactionImpl implements Transaction {

    @Override
    public void startTransaction() throws Exception {}

    @Override
    public void commitTransaction() throws Exception {}

    @Override
    public void rollbackTransaction() throws Exception {}
}

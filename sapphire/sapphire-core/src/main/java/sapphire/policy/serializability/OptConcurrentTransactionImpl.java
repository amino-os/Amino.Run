package sapphire.policy.serializability;

import java.io.Serializable;

/**
 * Created by Venugopal Reddy K 00900280 on 1/2/18.
 * A convenience implementation (OptConcurrentTransactionImpl) is provided, so that SO's
 * can just extend it.
 */

public class OptConcurrentTransactionImpl implements OptConcurrentTransaction, Serializable {

    @Override
    public void startTransaction() throws Exception {}

    @Override
    public void commitTransaction() throws Exception {}

    @Override
    public void rollbackTransaction() throws Exception {}
}

package sapphire.appexamples.hankstodo.app;

import sapphire.app.SapphireObject;
import sapphire.policy.serializability.TransactionAlreadyStartedException;
import sapphire.policy.transaction.TransactionExecutionException;
import sapphire.policy.transaction.TransactionManager;
import sapphire.policy.transaction.TwoPCCohortPolicy;
import sapphire.policy.transaction.TwoPCExtResourceCohortPolicy;

import java.io.Serializable;
import java.util.UUID;

public class Doer implements Serializable, SapphireObject<TwoPCExtResourceCohortPolicy>, TransactionManager {
    private String name;

    public void setDoer(String name) {
        this.name = name;
    }

    @Override
    public void join(UUID transactionId) throws TransactionAlreadyStartedException {

    }

    @Override
    public void leave(UUID transactionId) {

    }

    @Override
    public Vote vote(UUID transactionId) throws TransactionExecutionException {
        return Vote.YES;
    }

    @Override
    public void commit(UUID transactionId) {

    }

    @Override
    public void abort(UUID transactionId) {

    }
}

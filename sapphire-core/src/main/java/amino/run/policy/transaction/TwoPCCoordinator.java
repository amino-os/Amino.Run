package amino.run.policy.transaction;

import amino.run.policy.serializability.TransactionAlreadyStartedException;
import java.util.UUID;

/** abstraction of 2PC transaction coordinator */
public interface TwoPCCoordinator extends TransactionManager {
    /** sets up when a transaction is about to start */
    public void beginTransaction() throws TransactionAlreadyStartedException;

    /**
     * gets id of the active transaction
     *
     * @return id of the active transaction
     */
    public UUID getTransactionId();
}

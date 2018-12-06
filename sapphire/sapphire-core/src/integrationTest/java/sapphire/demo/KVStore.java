package sapphire.demo;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import sapphire.app.SapphireObject;
import sapphire.policy.cache.explicitcaching.ExplicitCacher;
import sapphire.policy.checkpoint.explicitcheckpoint.ExplicitCheckpointer;
import sapphire.policy.mobility.explicitmigration.ExplicitMigrator;
import sapphire.policy.mobility.explicitmigration.MigrationException;
import sapphire.policy.serializability.LockingTransaction;
import sapphire.policy.serializability.NoTransactionStartedException;
import sapphire.policy.serializability.TransactionAlreadyStartedException;
import sapphire.policy.serializability.TransactionException;
import sapphire.policy.transaction.TransactionExecutionException;
import sapphire.policy.transaction.TransactionManager;

/**
 * A simple key value class for integration tests.
 *
 * Note: Make <code>KVStore</code> implementing
 * <code>SapphireObject</> interface in order to
 * backward compatible with annotation based sapphire
 * object specification.
 *
 * We can remove <code>SapphireObject</code> interface
 * after we completely deprecate annotation based
 * specification.
 */
public class KVStore
        implements LockingTransaction,
                ExplicitCacher,
                ExplicitCheckpointer,
                ExplicitMigrator,
                TransactionManager,
                SapphireObject {
    private Map<String, Serializable> kvStore = new HashMap<>();

    public void set(String key, Serializable value) {
        this.kvStore.put(key, value);
    }

    public Serializable get(String key) {
        return this.kvStore.get(key);
    }

    public Serializable remove(String key) {
        Serializable value = this.kvStore.get(key);
        this.kvStore.remove(key);
        return value;
    }

    @Override
    public void startTransaction(long timeoutMillisec)
            throws TransactionAlreadyStartedException, TransactionException {}

    @Override
    public void startTransaction()
            throws TransactionAlreadyStartedException, TransactionException {}

    @Override
    public void commitTransaction() throws NoTransactionStartedException, TransactionException {}

    @Override
    public void rollbackTransaction() throws NoTransactionStartedException, TransactionException {}

    @Override
    public void pull() {}

    @Override
    public void push() {}

    @Override
    public void saveCheckpoint() throws Exception {}

    @Override
    public void restoreCheckpoint() throws Exception {}

    @Override
    public void migrateObject(InetSocketAddress destinationAddr) throws MigrationException {}

    @Override
    public void join(UUID transactionId) throws TransactionAlreadyStartedException {}

    @Override
    public void leave(UUID transactionId) {}

    @Override
    public Vote vote(UUID transactionId) throws TransactionExecutionException {
        return null;
    }

    @Override
    public void commit(UUID transactionId) {}

    @Override
    public void abort(UUID transactionId) {}
}

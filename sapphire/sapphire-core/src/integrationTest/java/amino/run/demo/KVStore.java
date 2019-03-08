package amino.run.demo;

import amino.run.app.MicroService;
import amino.run.policy.cache.explicitcaching.ExplicitCacher;
import amino.run.policy.checkpoint.explicitcheckpoint.ExplicitCheckpointer;
import amino.run.policy.mobility.explicitmigration.ExplicitMigrator;
import amino.run.policy.mobility.explicitmigration.MigrationException;
import amino.run.policy.serializability.LockingTransaction;
import amino.run.policy.serializability.NoTransactionStartedException;
import amino.run.policy.serializability.TransactionAlreadyStartedException;
import amino.run.policy.serializability.TransactionException;
import amino.run.policy.transaction.TransactionExecutionException;
import amino.run.policy.transaction.TransactionManager;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A simple key value class for integration tests.
 *
 * Note: Make <code>KVStore</code> implementing
 * <code>MicroService</> interface in order to
 * backward compatible with annotation based sapphire
 * object specification.
 *
 * We can remove <code>MicroService</code> interface
 * after we completely deprecate annotation based
 * specification.
 */
public class KVStore
        implements LockingTransaction,
                ExplicitCacher,
                ExplicitCheckpointer,
                ExplicitMigrator,
                TransactionManager,
                MicroService {
    private Map<String, Serializable> kvStore = new HashMap<String, Serializable>();

    public void set(String key, Serializable value) {
        this.kvStore.put(key, value);
    }

    public Serializable get(String key) {
        return this.kvStore.get(key);
    }

    public Serializable remove(String key) {
        Serializable value = this.kvStore.remove(key);
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
    public void migrateTo(InetSocketAddress destinationAddr) throws MigrationException {}

    @Override
    public void join(UUID transactionId) throws TransactionAlreadyStartedException {}

    @Override
    public void leave(UUID transactionId) {}

    @Override
    public Vote vote(UUID transactionId) throws TransactionExecutionException {
        return Vote.YES;
    }

    @Override
    public void commit(UUID transactionId) {}

    @Override
    public void abort(UUID transactionId) {}
}

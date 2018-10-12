package sapphire.demo;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;
import sapphire.app.SapphireObject;
import sapphire.policy.serializability.LockingTransactionImpl;

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
public class KVStore extends LockingTransactionImpl implements SapphireObject {
    private Map<String, Serializable> kvStore = new Hashtable<>();

    public void set(String key, Serializable value) {
        this.kvStore.put(key, value);
    }

    public Serializable get(String key) {
        return this.kvStore.get(key);
    }
}

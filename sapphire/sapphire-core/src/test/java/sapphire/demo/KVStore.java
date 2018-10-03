package sapphire.demo;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;

/** A simple key value class for integration tests. */
public class KVStore {
    private Map<String, Serializable> kvStore = new Hashtable<>();

    public void set(String key, Serializable value) {
        this.kvStore.put(key, value);
    }

    public Serializable get(String key) {
        return this.kvStore.get(key);
    }
}

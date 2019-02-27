package amino.run.demo;

import amino.run.app.MicroService;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;

/**
 * A Key-Value Store where keys are {@code String}s and
 * values are {@code Serializable}s.
 */
public class KeyValueStore implements MicroService {
    private Map<String, Serializable> kvStore = new Hashtable<>();

    public KeyValueStore() {}

    public void set(String key, Serializable value) {

        System.out.println(String.format("<Server>: setting %s = %s", key, value));
        this.kvStore.put(key, value);
    }

    public Serializable get(String key) {
        System.out.println(String.format("<Server>: getting value with key: %s", key));
        return this.kvStore.get(key);
    }

    public Boolean contains(String key) {
        System.out.println(String.format("<Server>: checking existence with key: %s", key));
        return this.kvStore.containsKey(key);
    }
}

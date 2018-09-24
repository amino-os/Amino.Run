package sapphire.demo;

import sapphire.app.SapphireObject;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;

public class pinkisServer implements SapphireObject {
    private Map<String, Serializable> kvStore = new Hashtable<>();

    public pinkisServer() {}

    public void set(String key, Serializable value) {
        this.kvStore.put(key, value);
    }

    public Serializable get(String key) {
        return this.kvStore.get(key);
    }

    public Boolean contains(String key) {
        return this.kvStore.containsKey(key);
    }
}

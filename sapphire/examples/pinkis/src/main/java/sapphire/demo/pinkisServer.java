package sapphire.demo;

import sapphire.app.SapphireObject;

import java.util.Hashtable;
import java.util.Map;

public class pinkisServer implements SapphireObject {
    private Map<String, String> kvStore = new Hashtable<>();

    public pinkisServer() {}

    public void set(String key, String value) {
        this.kvStore.put(key, value);
    }

    public String get(String key) {
        return this.kvStore.get(key);
    }

    public Boolean contains(String key) {
        return this.kvStore.containsKey(key);
    }
}

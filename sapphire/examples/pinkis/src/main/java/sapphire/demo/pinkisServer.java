package sapphire.demo;

import sapphire.app.SapphireObject;
import sapphire.runtime.SapphireConfiguration;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;

@SapphireConfiguration(Policies = {"sapphire.policy.dht.DHTPolicy", "sapphire.policy.scalability.LoadBalancedMasterSlaveSyncPolicy"})
public class pinkisServer implements SapphireObject {
    private Map<String, Serializable> kvStore = new Hashtable<>();

    public pinkisServer() {}

    public void set(String key, Serializable value) {
        System.out.println("setting: " + key + " = " + value);
        this.kvStore.put(key, value);
    }

    public Serializable get(String key) {
        System.out.println("getting: " + key);
        return this.kvStore.get(key);
    }

    public Boolean contains(String key) {
        System.out.println("querying: " + key);
        return this.kvStore.containsKey(key);
    }
}

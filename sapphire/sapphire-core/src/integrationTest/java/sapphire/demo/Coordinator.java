package sapphire.demo;

import java.io.Serializable;
import sapphire.app.SapphireObject;

public class Coordinator implements SapphireObject {
  
    private KVStore store1;
    private KVStore store2;

    public Coordinator(KVStore s1, KVStore s2) {
        this.store1 = store1;
        this.store2 = store2;
    }

    public void migrate(String key) throws Exception {
        Serializable value, value1;
        value = this.store1.get(key);
        this.store1.remove(key);

        value1 = this.store2.get(key);
        if (value1 != null) {
            throw new Exception("TransactionException");
        }
        this.store2.set(key, value);
    }
}

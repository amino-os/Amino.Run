package amino.run.demo;

import amino.run.app.MicroService;
import java.io.Serializable;

public class Coordinator implements MicroService {

    private KVStore store1;
    private KVStore store2;

    public Coordinator(KVStore s1, KVStore s2) {
        this.store1 = s1;
        this.store2 = s2;
    }

    /* migrate  method  moves the specified key value pair from store 1 to store2,
    If store holds the specified key then exception will be thrown.
    Two PC Coordinator DM catches the exception and triggers the abort on all
    participants */

    public void migrate(String key) throws Exception {
        Serializable value, value1;
        // store1.join invoked by coordinator
        value = this.store1.remove(key);
        // store1.leave by coordinator

        // store2.join invoked by coordinator
        value1 = this.store2.get(key);
        // store2.leave invoked by coordinator
        if (value1 != null) {
            // Exception  thrown to trigger the abort
            throw new Exception("TransactionException");
            // store2.abort invoked by coordinator
            // store1.abort invoked by coordinator
        }
        // store2.join invoked by coordinator
        this.store2.set(key, value);
        // store2.leave invoked by coordinator
        // store1.vote invoked by coordinator
        // store1.commit invoked by coordinator
        // store2.vote invoked by coordinator
        // store2.commit invoked by coordinator

    }
}

package sapphire.oms;

import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireReplicaID;
import sapphire.runtime.EventHandler;

public class SapphireInstanceManager {

    private SapphireObjectID oid;
    private EventHandler instanceDispatcher;
    private HashMap<SapphireReplicaID, EventHandler> replicaDispatchers;
    private Random oidGenerator;

    /**
     * Randomly generate a new replica id
     *
     * @return Returns a new replica id
     */
    private SapphireReplicaID generateSapphireReplicaID() {
        return new SapphireReplicaID(oid, oidGenerator.nextInt());
    }

    public SapphireInstanceManager(SapphireObjectID oid, EventHandler dispatcher) {
        this.oid = oid;
        instanceDispatcher = dispatcher;
        replicaDispatchers = new HashMap<SapphireReplicaID, EventHandler>();
        oidGenerator = new Random(new Date().getTime());
    }

    /**
     * Gets the event handler of this sapphire instance
     *
     * @return Returns event handler
     */
    public EventHandler getInstanceDispatcher() {
        return instanceDispatcher;
    }

    /**
     * Sets the event handler of this sapphire instance
     *
     * @param dispatcher
     */
    public void setInstanceDispatcher(EventHandler dispatcher) {
        instanceDispatcher = dispatcher;
    }

    /**
     * Gets the event handler for the given replica of this sapphire instance
     *
     * @param rid
     * @return Returns event handler of the replica
     * @throws SapphireObjectNotFoundException
     */
    public EventHandler getReplicaDispatcher(SapphireReplicaID rid)
            throws SapphireObjectNotFoundException {
        EventHandler dispatcher = replicaDispatchers.get(rid);
        return dispatcher;
    }

    /**
     * Sets the event handler for the given replica of this sapphire instance
     *
     * @param rid
     * @param dispatcher
     */
    public void setReplicaDispatcher(SapphireReplicaID rid, EventHandler dispatcher) {
        replicaDispatchers.put(rid, dispatcher);
    }

    /**
     * Generates a replica id and add replica to this sapphire instance
     *
     * @param dispatcher
     * @return returns a new replica id
     */
    public SapphireReplicaID addReplica(EventHandler dispatcher) {
        SapphireReplicaID rid = generateSapphireReplicaID();
        replicaDispatchers.put(rid, dispatcher);
        return rid;
    }
}

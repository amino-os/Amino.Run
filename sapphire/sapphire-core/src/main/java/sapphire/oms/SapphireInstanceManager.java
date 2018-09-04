package sapphire.oms;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.common.SapphireReplicaID;
import sapphire.runtime.EventHandler;

public class SapphireInstanceManager {

    private SapphireObjectID oid;
    private String name;
    private AtomicInteger referenceCount;
    private EventHandler instanceDispatcher;
    private HashMap<SapphireReplicaID, EventHandler> replicaDispatchers;
    private Random oidGenerator;

    /**
     * Randomly generate a new replica id
     *
     * @return Returns a new replica id
     */
    private SapphireReplicaID generateSapphireReplicaID() {
        return new SapphireReplicaID(oid, UUID.randomUUID());
    }

    public SapphireInstanceManager(SapphireObjectID oid, EventHandler dispatcher) {
        this.oid = oid;
        instanceDispatcher = dispatcher;
        replicaDispatchers = new HashMap<SapphireReplicaID, EventHandler>();
        oidGenerator = new Random(new Date().getTime());
        referenceCount = new AtomicInteger(1);
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
            throws SapphireObjectReplicaNotFoundException {
        EventHandler dispatcher = replicaDispatchers.get(rid);
        if (dispatcher == null) {
            throw new SapphireObjectReplicaNotFoundException(
                    "Failed to find sapphire object replica dispatcher");
        }
        return dispatcher;
    }

    /**
     * Sets the event handler for the given replica of this sapphire instance
     *
     * @param rid
     * @param dispatcher
     */
    public void setReplicaDispatcher(SapphireReplicaID rid, EventHandler dispatcher)
            throws SapphireObjectReplicaNotFoundException {
        if (replicaDispatchers.containsKey(rid)) {
            replicaDispatchers.put(rid, dispatcher);
        } else {
            throw new SapphireObjectReplicaNotFoundException(
                    "Failed to find sapphire object replica");
        }
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

    /**
     * Removes the replica from this sapphire instance
     *
     * @param replicaId
     */
    public void removeReplica(SapphireReplicaID replicaId) {
        replicaDispatchers.remove(replicaId);
    }

    /**
     * Gets replica handlers of this sapphire instance
     *
     * @return Returns array of event handlers
     */
    public EventHandler[] getReplicas() {
        Collection<EventHandler> values = replicaDispatchers.values();
        return values.toArray(new EventHandler[values.size()]);
    }

    public void clear() {
        replicaDispatchers.clear();
    }

    public SapphireObjectID getOid() {
        return oid;
    }

    public void setName(String sapphireObjName) {
        name = sapphireObjName;
    }

    public String getName() {
        return name;
    }

    public int incrRefCountAndGet() {
        return referenceCount.incrementAndGet();
    }

    public int decrRefCountAndGet() {
        return referenceCount.decrementAndGet();
    }

    public int getReferenceCount() {
        return referenceCount.get();
    }
}

package sapphire.oms;

import static sapphire.policy.SapphirePolicy.SapphireGroupPolicy;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.common.SapphireReplicaID;
import sapphire.policy.SapphirePolicy;
import sapphire.runtime.EventHandler;

public class SapphireInstanceManager {
    private static final Logger logger = Logger.getLogger(SapphireInstanceManager.class.getName());

    private SapphireObjectID oid;
    private String name;
    private AtomicInteger referenceCount;
    private EventHandler instanceDispatcher;
    private AppObjectStub objectStub;
    private HashMap<SapphireReplicaID, EventHandler> replicaDispatchers;
    private Random oidGenerator;
    /**
     * Root group policy is the <strong>outmost</strong> group policy of this sapphire object.
     *
     * <p>For example, given a sapphire object with DM list [DHT, MasterSlave], its outmost DM is
     * DHT. In this case, {@code rootGroupPolicy} is the DHT group policy.
     *
     * <p>TODO(multi-dm): We actually need to maintain group policies of inner DMs too. We need to
     * organize group policies, their relationships, and their healthiness into a table as described
     * in the multi-DM design doc.
     *
     * @see <a
     *     href="https://docs.google.com/document/d/1g5SnzsnyGXzdZVDF_uj9MQJomQpHS-PMpfwnYn4RNDU/edit#heading=h.j9vsjm8kyruk">Multi-DM
     *     Design Doc</a>
     */
    private SapphireGroupPolicy rootGroupPolicy;

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
     * Sets the root group policy of this sapphire object.
     *
     * @param rootGroupPolicy root group policy
     */
    public void setRootGroupPolicy(SapphirePolicy.SapphireGroupPolicy rootGroupPolicy) {
        this.rootGroupPolicy = rootGroupPolicy;
    }

    /**
     * Gets the root group policy object of this sapphire instance
     * @return Sapphire Group Policy Object
     */
    public SapphireGroupPolicy getRootGroupPolicy() {
        return rootGroupPolicy;
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
     * Gets the object stub of this sapphire instance
     *
     * @return Returns object stub
     */
    public AppObjectStub getInstanceObjectStub() {
        return objectStub;
    }

    /**
     * Sets the object stub of this sapphire instance
     *
     * @param objStub
     */
    public void setInstanceObjectStub(AppObjectStub objStub) {
        objectStub = objStub;
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

    public void clear() throws RemoteException {
        if (rootGroupPolicy != null) {
            rootGroupPolicy.onDestroy();
        }
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

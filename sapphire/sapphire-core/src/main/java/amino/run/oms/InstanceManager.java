package amino.run.oms;

import amino.run.common.AppObjectStub;
import amino.run.common.MicroServiceID;
import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.common.ReplicaID;
import amino.run.policy.Policy;
import amino.run.runtime.EventHandler;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class InstanceManager {
    private static final Logger logger = Logger.getLogger(InstanceManager.class.getName());

    private MicroServiceID oid;
    private String name;
    private AtomicInteger referenceCount;
    private EventHandler instanceDispatcher;
    private AppObjectStub objectStub;
    private HashMap<ReplicaID, EventHandler> replicaDispatchers;
    private Random oidGenerator;
    /**
     * Root group policy is the <strong>outmost</strong> group policy of this microservice.
     *
     * <p>For example, given a microservice with DM list [DHT, MasterSlave], its outmost DM is DHT.
     * In this case, {@code rootGroupPolicy} is the DHT group policy.
     *
     * <p>TODO(multi-dm): We actually need to maintain group policies of inner DMs too. We need to
     * organize group policies, their relationships, and their healthiness into a table as described
     * in the multi-DM design doc.
     *
     * @see <a
     *     href="https://docs.google.com/document/d/1g5SnzsnyGXzdZVDF_uj9MQJomQpHS-PMpfwnYn4RNDU/edit#heading=h.j9vsjm8kyruk">Multi-DM
     *     Design Doc</a>
     */
    private Policy.GroupPolicy rootGroupPolicy;

    /**
     * Randomly generate a new replica id
     *
     * @return Returns a new replica id
     */
    private ReplicaID generateSapphireReplicaID() {
        return new ReplicaID(oid, UUID.randomUUID());
    }

    public InstanceManager(MicroServiceID oid, EventHandler dispatcher) {
        this.oid = oid;
        instanceDispatcher = dispatcher;
        replicaDispatchers = new HashMap<ReplicaID, EventHandler>();
        oidGenerator = new Random(new Date().getTime());
        referenceCount = new AtomicInteger(1);
    }

    /**
     * Sets the root group policy of this microservice.
     *
     * @param rootGroupPolicy root group policy
     */
    public void setRootGroupPolicy(Policy.GroupPolicy rootGroupPolicy) {
        this.rootGroupPolicy = rootGroupPolicy;
    }

    /**
     * Gets the root group policy object of this sapphire instance
     *
     * @return MicroService Group Policy Object
     */
    public Policy.GroupPolicy getRootGroupPolicy() {
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
     * @throws MicroServiceNotFoundException
     */
    public EventHandler getReplicaDispatcher(ReplicaID rid)
            throws MicroServiceReplicaNotFoundException {
        EventHandler dispatcher = replicaDispatchers.get(rid);
        if (dispatcher == null) {
            throw new MicroServiceReplicaNotFoundException(
                    "Failed to find microservice replica dispatcher");
        }
        return dispatcher;
    }

    /**
     * Sets the event handler for the given replica of this sapphire instance
     *
     * @param rid
     * @param dispatcher
     */
    public void setReplicaDispatcher(ReplicaID rid, EventHandler dispatcher)
            throws MicroServiceReplicaNotFoundException {
        if (replicaDispatchers.containsKey(rid)) {
            replicaDispatchers.put(rid, dispatcher);
        } else {
            throw new MicroServiceReplicaNotFoundException("Failed to find microservice replica");
        }
    }

    /**
     * Generates a replica id and add replica to this sapphire instance
     *
     * @param dispatcher
     * @return returns a new replica id
     */
    public ReplicaID addReplica(EventHandler dispatcher) {
        ReplicaID rid = generateSapphireReplicaID();
        replicaDispatchers.put(rid, dispatcher);
        return rid;
    }

    /**
     * Removes the replica from this sapphire instance
     *
     * @param replicaId
     */
    public void removeReplica(ReplicaID replicaId) {
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

    public MicroServiceID getOid() {
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

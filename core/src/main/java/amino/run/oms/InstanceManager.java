package amino.run.oms;

import amino.run.common.AppObjectStub;
import amino.run.common.MicroServiceID;
import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.common.ReplicaID;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.metric.RPCMetric;
import amino.run.oms.metrics.MicroServiceMetric;
import amino.run.runtime.EventHandler;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class InstanceManager {
    private static final Logger logger = Logger.getLogger(InstanceManager.class.getName());

    private MicroServiceID oid;
    private String name;
    private AtomicInteger referenceCount;
    private Map<KernelOID, EventHandler> groupDispatchers;
    private AppObjectStub objectStub;
    private HashMap<ReplicaID, EventHandler> replicaDispatchers;
    private HashMap<ReplicaID, MicroServiceMetric> replicaMetrics;
    private Random oidGenerator;

    /**
     * Randomly generate a new replica id
     *
     * @return Returns a new replica id
     */
    private ReplicaID generateReplicaID() {
        return new ReplicaID(oid, UUID.randomUUID());
    }

    public InstanceManager(MicroServiceID oid, EventHandler dispatcher) {
        this.oid = oid;
        groupDispatchers =
                Collections.synchronizedMap(new LinkedHashMap<KernelOID, EventHandler>());
        replicaDispatchers = new HashMap<ReplicaID, EventHandler>();
        replicaMetrics = new HashMap<ReplicaID, MicroServiceMetric>();
        oidGenerator = new Random(new Date().getTime());
        referenceCount = new AtomicInteger(1);
    }

    /**
     * Adds the group policy dispatcher to this microservice instance
     *
     * @param groupOid
     * @param dispatcher
     */
    public void addGroupDispatcher(KernelOID groupOid, EventHandler dispatcher) {
        groupDispatchers.put(groupOid, dispatcher);
    }

    /**
     * Removes the group policy dispatcher from this microservice instance
     *
     * @param groupOid
     */
    public void removeGroupDispatcher(KernelOID groupOid) {
        groupDispatchers.remove(groupOid);
    }

    /**
     * Gets the root group policy kernel OID of this microservice instance
     *
     * @return Kernel OID of root group policy
     */
    public KernelOID getRootGroupId() {
        return groupDispatchers.keySet().iterator().next();
    }

    /**
     * Gets the group policy dispatcher of this microservice instance
     *
     * @param oid
     * @return Returns event handler
     */
    public EventHandler getGroupDispatcher(KernelOID oid) {
        return groupDispatchers.get(oid);
    }

    /**
     * Gets the object stub of this microservice instance
     *
     * @return Returns object stub
     */
    public AppObjectStub getInstanceObjectStub() {
        return objectStub;
    }

    /**
     * Sets the object stub of this microservice instance
     *
     * @param objStub
     */
    public void setInstanceObjectStub(AppObjectStub objStub) {
        objectStub = objStub;
    }

    /**
     * Gets the event handler for the given replica of this microservice instance
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
     * Sets the event handler for the given replica of this microservice instance
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
     * Generates a replica id and add replica to this microservice instance
     *
     * @param dispatcher
     * @return returns a new replica id
     */
    public ReplicaID addReplica(EventHandler dispatcher) {
        ReplicaID rid = generateReplicaID();
        replicaDispatchers.put(rid, dispatcher);
        replicaMetrics.put(rid, new MicroServiceMetric(rid));
        return rid;
    }

    /**
     * Removes the replica from this microservice instance
     *
     * @param replicaId
     */
    public void removeReplica(ReplicaID replicaId) {
        replicaDispatchers.remove(replicaId);
        replicaMetrics.remove(replicaId);
    }

    /**
     * Updates the replica metrics of this microservice instance
     *
     * @param replicaId
     * @param metrics
     */
    public void updateMetric(ReplicaID replicaId, Map<UUID, RPCMetric> metrics) {
        replicaMetrics.get(replicaId).updateMetric(metrics);
    }

    /**
     * Gets replica handlers of this microservice instance
     *
     * @return Returns array of event handlers
     */
    public EventHandler[] getReplicas() {
        Collection<EventHandler> values = replicaDispatchers.values();
        return values.toArray(new EventHandler[values.size()]);
    }

    /** Clears resource associated with this microService instance */
    public void clear() {
        groupDispatchers.clear();
        replicaDispatchers.clear();
        replicaMetrics.clear();
    }

    public MicroServiceID getOid() {
        return oid;
    }

    public void setName(String microServiceName) {
        name = microServiceName;
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

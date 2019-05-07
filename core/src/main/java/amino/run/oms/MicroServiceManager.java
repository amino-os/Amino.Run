package amino.run.oms;

import amino.run.common.AppObjectStub;
import amino.run.common.MicroServiceID;
import amino.run.common.MicroServiceNameModificationException;
import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.common.ReplicaID;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.metric.RPCMetric;
import amino.run.runtime.EventHandler;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MicroServiceManager {
    private ConcurrentHashMap<MicroServiceID, InstanceManager> microServices;
    private ConcurrentHashMap<String, InstanceManager> microServicesByName;

    /**
     * Generate a new, globally unique microservice id
     *
     * @return
     */
    private MicroServiceID generateMicroServiceID() {
        return new MicroServiceID(UUID.randomUUID());
    }

    public MicroServiceManager() {
        microServices = new ConcurrentHashMap<MicroServiceID, InstanceManager>();
        microServicesByName = new ConcurrentHashMap<String, InstanceManager>();
    }

    private InstanceManager getInstance(MicroServiceID microServiceId)
            throws MicroServiceNotFoundException {
        InstanceManager instance = microServices.get(microServiceId);
        if (instance == null) {
            throw new MicroServiceNotFoundException(
                    String.format("Not a valid MicroService id : %s", microServiceId));
        }

        return instance;
    }

    /**
     * Generates a microservice id and adds it
     *
     * @param dispatcher
     * @return Returns the new id
     */
    public MicroServiceID addInstance(EventHandler dispatcher) {
        MicroServiceID oid = generateMicroServiceID();
        InstanceManager instance = new InstanceManager(oid, dispatcher);
        microServices.put(oid, instance);
        return oid;
    }

    /**
     * Adds the group policy dispatcher to microservice
     *
     * @param microServiceId
     * @param groupOid
     * @param dispatcher
     * @throws MicroServiceNotFoundException
     */
    public void addGroupDispatcher(
            MicroServiceID microServiceId, KernelOID groupOid, EventHandler dispatcher)
            throws MicroServiceNotFoundException {
        getInstance(microServiceId).addGroupDispatcher(groupOid, dispatcher);
    }

    /**
     * Removes the group policy dispatcher from microservice
     *
     * @param microServiceId
     * @param groupOid
     * @throws MicroServiceNotFoundException
     */
    public void removeGroupDispatcher(MicroServiceID microServiceId, KernelOID groupOid)
            throws MicroServiceNotFoundException {
        getInstance(microServiceId).removeGroupDispatcher(groupOid);
    }

    /**
     * Gets the root group policy kernel OID of microservice
     *
     * @param microServiceId
     * @return Kernel OID of root group policy
     * @throws MicroServiceNotFoundException
     */
    public KernelOID getRootGroupId(MicroServiceID microServiceId)
            throws MicroServiceNotFoundException {
        return getInstance(microServiceId).getRootGroupId();
    }

    /**
     * Set the object stub of a microservice
     *
     * @param microServiceId
     * @param objectStub
     * @throws MicroServiceNotFoundException
     */
    public void setInstanceObjectStub(MicroServiceID microServiceId, AppObjectStub objectStub)
            throws MicroServiceNotFoundException {
        getInstance(microServiceId).setInstanceObjectStub(objectStub);
    }

    /**
     * Add a replica of a microservice
     *
     * @param microServiceId
     * @param dispatcher
     * @return Returns the new replica id
     * @throws MicroServiceNotFoundException
     */
    public ReplicaID addReplica(MicroServiceID microServiceId, EventHandler dispatcher)
            throws MicroServiceNotFoundException {
        InstanceManager instance = getInstance(microServiceId);
        synchronized (instance) {
            if (instance.getReferenceCount() == 0) {
                /* MicroService object could have been deleted in another thread */
                throw new MicroServiceNotFoundException("MicroService object is deleted.");
            }
            return instance.addReplica(dispatcher);
        }
    }

    /**
     * Remove an instance of a microservice
     *
     * @param microServiceId
     * @throws MicroServiceNotFoundException
     */
    public void removeInstance(MicroServiceID microServiceId)
            throws MicroServiceNotFoundException, RemoteException {
        InstanceManager instanceManager = getInstance(microServiceId);
        instanceManager.clear();

        if (instanceManager.getName() != null) {
            microServicesByName.remove(instanceManager.getName());
        }
        microServices.remove(microServiceId);
    }

    /**
     * Get all the microservices
     *
     * @throws java.rmi.RemoteException
     */
    public ArrayList<MicroServiceID> getAllMicroServices() throws RemoteException {
        ArrayList<MicroServiceID> arr = new ArrayList<MicroServiceID>(microServices.keySet());
        return arr;
    }

    /**
     * Sets the name of a microservice
     *
     * @param microServiceId
     * @param name
     * @throws MicroServiceNotFoundException
     */
    public void setInstanceName(MicroServiceID microServiceId, String name)
            throws MicroServiceNotFoundException, MicroServiceNameModificationException {
        InstanceManager instance = getInstance(microServiceId);

        /* Object name is not allowed to change once set. Because reference count are updated based
        on attachByName and detachByName. And name change would affect it */
        if (instance.getName() != null) {
            throw new MicroServiceNameModificationException(microServiceId, instance.getName());
        }

        /* This name is already used for some other microservice */
        InstanceManager otherInstance = microServicesByName.get(name);
        if (otherInstance != null) {
            throw new MicroServiceNameModificationException(otherInstance.getOid(), name);
        }

        synchronized (instance) {
            if (instance.getReferenceCount() != 0) {
                microServicesByName.put(name, instance);
                instance.setName(name);
            }
        }
    }

    /**
     * Removes a replica of a microservice
     *
     * @param replicaId
     * @throws MicroServiceNotFoundException
     */
    public void removeReplica(ReplicaID replicaId) throws MicroServiceNotFoundException {
        InstanceManager instance = getInstance(replicaId.getOID());
        synchronized (instance) {
            instance.removeReplica(replicaId);
        }
    }

    /**
     * Set the event handler of a replica
     *
     * @param replicaId
     * @param dispatcher
     * @throws MicroServiceNotFoundException
     */
    public void setReplicaDispatcher(ReplicaID replicaId, EventHandler dispatcher)
            throws MicroServiceNotFoundException, MicroServiceReplicaNotFoundException {
        InstanceManager instance = getInstance(replicaId.getOID());
        synchronized (instance) {
            if (instance.getReferenceCount() == 0) {
                /* MicroService object could have been deleted in another thread */
                throw new MicroServiceNotFoundException("MicroService object is deleted.");
            }
            instance.setReplicaDispatcher(replicaId, dispatcher);
        }
    }

    /**
     * Updates the microservice metrics for the given replica
     *
     * @param replicaId
     * @param metrics
     * @throws MicroServiceNotFoundException
     */
    public void updateMicroServiceMetric(ReplicaID replicaId, Map<UUID, RPCMetric> metrics)
            throws MicroServiceNotFoundException {
        getInstance(replicaId.getOID()).updateMetric(replicaId, metrics);
    }

    /**
     * Gets the group dispatcher of microservice
     *
     * @param microServiceId
     * @param groupOid
     * @return
     * @throws MicroServiceNotFoundException
     */
    public EventHandler getGroupDispatcher(MicroServiceID microServiceId, KernelOID groupOid)
            throws MicroServiceNotFoundException {
        return getInstance(microServiceId).getGroupDispatcher(groupOid);
    }

    /**
     * Get the object stub of microservice
     *
     * @param microServiceId
     * @return
     * @throws MicroServiceNotFoundException
     */
    public AppObjectStub getInstanceObjectStub(MicroServiceID microServiceId)
            throws MicroServiceNotFoundException {
        return getInstance(microServiceId).getInstanceObjectStub();
    }

    /**
     * Get the event handler of a replica
     *
     * @param replicaId
     * @return
     * @throws MicroServiceNotFoundException
     */
    public EventHandler getReplicaDispatcher(ReplicaID replicaId)
            throws MicroServiceNotFoundException, MicroServiceReplicaNotFoundException {
        return getInstance(replicaId.getOID()).getReplicaDispatcher(replicaId);
    }

    /**
     * Get microservice id by name
     *
     * @param name
     * @return
     * @throws MicroServiceNotFoundException
     */
    public MicroServiceID getMicroServiceByName(String name) throws MicroServiceNotFoundException {
        InstanceManager instance = microServicesByName.get(name);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid MicroService object id.");
        }

        return instance.getOid();
    }

    /**
     * Get replicas by id
     *
     * @param microServiceId
     * @return
     * @throws MicroServiceNotFoundException
     */
    public EventHandler[] getReplicasById(MicroServiceID microServiceId)
            throws MicroServiceNotFoundException {
        return getInstance(microServiceId).getReplicas();
    }

    public int incrRefCountAndGet(MicroServiceID microServiceId)
            throws MicroServiceNotFoundException {
        InstanceManager instance = getInstance(microServiceId);
        synchronized (instance) {
            if (instance.getReferenceCount() == 0) {
                throw new MicroServiceNotFoundException("MicroService object is deleted.");
            }
            return instance.incrRefCountAndGet();
        }
    }

    public int decrRefCountAndGet(MicroServiceID microServiceId)
            throws MicroServiceNotFoundException {
        return getInstance(microServiceId).decrRefCountAndGet();
    }
}

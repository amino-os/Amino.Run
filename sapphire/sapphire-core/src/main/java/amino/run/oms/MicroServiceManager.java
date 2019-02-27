package amino.run.oms;

import amino.run.common.AppObjectStub;
import amino.run.common.MicroServiceID;
import amino.run.common.MicroServiceNameModificationException;
import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.common.ReplicaID;
import amino.run.policy.Policy;
import amino.run.runtime.EventHandler;
import java.rmi.RemoteException;
import java.util.ArrayList;
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
     * Set event handler of a microservice
     *
     * @param microServiceId
     * @param dispatcher
     * @throws MicroServiceNotFoundException
     */
    public void setInstanceDispatcher(MicroServiceID microServiceId, EventHandler dispatcher)
            throws MicroServiceNotFoundException {
        InstanceManager instance = microServices.get(microServiceId);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid MicroService id.");
        }
        instance.setInstanceDispatcher(dispatcher);
    }

    /**
     * Gets the root group policy of a microservice
     *
     * @param oid
     * @return MicroService Group Policy Object
     * @throws MicroServiceNotFoundException
     */
    public Policy.GroupPolicy getRootGroupPolicy(MicroServiceID oid)
            throws MicroServiceNotFoundException {
        InstanceManager instanceManager = microServices.get(oid);
        if (instanceManager == null) {
            throw new MicroServiceNotFoundException("Not a valid MicroService object id.");
        }
        return instanceManager.getRootGroupPolicy();
    }

    public void setRootGroupPolicy(MicroServiceID oid, Policy.GroupPolicy rootGroupPolicy)
            throws MicroServiceNotFoundException {
        InstanceManager instanceManager = microServices.get(oid);
        if (instanceManager == null) {
            throw new MicroServiceNotFoundException("Not a valid MicroService object id.");
        }
        instanceManager.setRootGroupPolicy(rootGroupPolicy);
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
        InstanceManager instance = microServices.get(microServiceId);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid MicroService object id.");
        }
        instance.setInstanceObjectStub(objectStub);
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
        InstanceManager instance = microServices.get(microServiceId);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid MicroService object id.");
        }

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
        InstanceManager instanceManager = microServices.get(microServiceId);
        if (instanceManager == null) {
            throw new MicroServiceNotFoundException(
                    "Cannot find microservice with ID " + microServiceId);
        }

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
        InstanceManager instance = microServices.get(microServiceId);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid MicroService object id.");
        }

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
        InstanceManager instance = microServices.get(replicaId.getOID());
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid MicroService id.");
        }

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
        InstanceManager instance = microServices.get(replicaId.getOID());
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid MicroService object id.");
        }

        synchronized (instance) {
            if (instance.getReferenceCount() == 0) {
                /* MicroService object could have been deleted in another thread */
                throw new MicroServiceNotFoundException("MicroService object is deleted.");
            }
            instance.setReplicaDispatcher(replicaId, dispatcher);
        }
    }

    /**
     * Get the event handler of microservice
     *
     * @param microServiceId
     * @return
     * @throws MicroServiceNotFoundException
     * @deprecated
     */
    public EventHandler getInstanceDispatcher(MicroServiceID microServiceId)
            throws MicroServiceNotFoundException {
        InstanceManager instance = microServices.get(microServiceId);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid MicroService object id.");
        }

        return instance.getInstanceDispatcher();
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
        InstanceManager instance = microServices.get(microServiceId);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid MicroService object id.");
        }

        return instance.getInstanceObjectStub();
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
        InstanceManager instance = microServices.get(replicaId.getOID());
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid MicroService object id.");
        }

        return instance.getReplicaDispatcher(replicaId);
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
     * @param oid
     * @return
     * @throws MicroServiceNotFoundException
     */
    public EventHandler[] getReplicasById(MicroServiceID oid) throws MicroServiceNotFoundException {
        InstanceManager instance = microServices.get(oid);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid MicroService object id.");
        }

        return instance.getReplicas();
    }

    public int incrRefCountAndGet(MicroServiceID microServiceId)
            throws MicroServiceNotFoundException {
        InstanceManager instance = microServices.get(microServiceId);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid MicroService object id.");
        }
        synchronized (instance) {
            if (instance.getReferenceCount() == 0) {
                throw new MicroServiceNotFoundException("MicroService object is deleted.");
            }
            return instance.incrRefCountAndGet();
        }
    }

    public int decrRefCountAndGet(MicroServiceID microServiceId)
            throws MicroServiceNotFoundException {
        InstanceManager instance = microServices.get(microServiceId);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid MicroService object id.");
        }
        return instance.decrRefCountAndGet();
    }
}

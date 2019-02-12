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

public class SapphireObjectManager {
    private ConcurrentHashMap<MicroServiceID, SapphireInstanceManager> sapphireObjects;
    private ConcurrentHashMap<String, SapphireInstanceManager> sapphireObjectsByName;

    /**
     * Randomly generate a new sapphire object id
     *
     * @return
     */
    private MicroServiceID generateSapphireObjectID() {
        return new MicroServiceID(UUID.randomUUID());
    }

    public SapphireObjectManager() {
        sapphireObjects = new ConcurrentHashMap<MicroServiceID, SapphireInstanceManager>();
        sapphireObjectsByName = new ConcurrentHashMap<String, SapphireInstanceManager>();
    }

    /**
     * Generates a sapphire object id and add new sapphire object
     *
     * @param dispatcher
     * @return Returns a new sapphire object id
     */
    public MicroServiceID addInstance(EventHandler dispatcher) {
        MicroServiceID oid = generateSapphireObjectID();
        SapphireInstanceManager instance = new SapphireInstanceManager(oid, dispatcher);
        sapphireObjects.put(oid, instance);
        return oid;
    }

    /**
     * Set the Event handler of sapphire object
     *
     * @param microServiceId
     * @param dispatcher
     * @throws MicroServiceNotFoundException
     */
    public void setInstanceDispatcher(MicroServiceID microServiceId, EventHandler dispatcher)
            throws MicroServiceNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(microServiceId);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid Sapphire object id.");
        }
        instance.setInstanceDispatcher(dispatcher);
    }

    /**
     * Gets the root group policy object with sapphire object id
     *
     * @param oid
     * @return Sapphire Group Policy Object
     * @throws MicroServiceNotFoundException
     */
    public Policy.GroupPolicy getRootGroupPolicy(MicroServiceID oid)
            throws MicroServiceNotFoundException {
        SapphireInstanceManager instanceManager = sapphireObjects.get(oid);
        if (instanceManager == null) {
            throw new MicroServiceNotFoundException("Not a valid Sapphire object id.");
        }
        return instanceManager.getRootGroupPolicy();
    }

    public void setRootGroupPolicy(MicroServiceID oid, Policy.GroupPolicy rootGroupPolicy)
            throws MicroServiceNotFoundException {
        SapphireInstanceManager instanceManager = sapphireObjects.get(oid);
        if (instanceManager == null) {
            throw new MicroServiceNotFoundException("Not a valid Sapphire object id.");
        }
        instanceManager.setRootGroupPolicy(rootGroupPolicy);
    }

    /**
     * Set the object stub of sapphire object
     *
     * @param microServiceId
     * @param objectStub
     * @throws MicroServiceNotFoundException
     */
    public void setInstanceObjectStub(MicroServiceID microServiceId, AppObjectStub objectStub)
            throws MicroServiceNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(microServiceId);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid Sapphire object id.");
        }
        instance.setInstanceObjectStub(objectStub);
    }

    /**
     * Adds a sapphire replica of given sapphire object
     *
     * @param microServiceId
     * @param dispatcher
     * @return Returns a new sapphire replica id
     * @throws MicroServiceNotFoundException
     */
    public ReplicaID addReplica(MicroServiceID microServiceId, EventHandler dispatcher)
            throws MicroServiceNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(microServiceId);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid Sapphire object id.");
        }

        synchronized (instance) {
            if (instance.getReferenceCount() == 0) {
                /* Sapphire object could have been deleted in another thread */
                throw new MicroServiceNotFoundException("Sapphire object is deleted.");
            }
            return instance.addReplica(dispatcher);
        }
    }

    /**
     * Removes the sapphire object with the given ID.
     *
     * @param microServiceId sapphire object ID
     * @throws MicroServiceNotFoundException
     */
    public void removeInstance(MicroServiceID microServiceId)
            throws MicroServiceNotFoundException, RemoteException {
        SapphireInstanceManager instanceManager = sapphireObjects.get(microServiceId);
        if (instanceManager == null) {
            throw new MicroServiceNotFoundException(
                    "Cannot find sapphire object with ID " + microServiceId);
        }

        instanceManager.clear();

        if (instanceManager.getName() != null) {
            sapphireObjectsByName.remove(instanceManager.getName());
        }
        sapphireObjects.remove(microServiceId);
    }

    /**
     * get all the sapphire objects
     *
     * @throws java.rmi.RemoteException
     */
    public ArrayList<MicroServiceID> getAllSapphireObjects() throws RemoteException {
        ArrayList<MicroServiceID> arr = new ArrayList<MicroServiceID>(sapphireObjects.keySet());
        return arr;
    }

    /**
     * Sets the name to sapphire object
     *
     * @param microServiceId
     * @param name
     * @throws MicroServiceNotFoundException
     */
    public void setInstanceName(MicroServiceID microServiceId, String name)
            throws MicroServiceNotFoundException, MicroServiceNameModificationException {
        SapphireInstanceManager instance = sapphireObjects.get(microServiceId);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid Sapphire object id.");
        }

        /* Object name is not allowed to change once set. Because reference count are updated based
        on attachByName and detachByName. And name change would affect it */
        if (instance.getName() != null) {
            throw new MicroServiceNameModificationException(microServiceId, instance.getName());
        }

        /* This name is already used for some other sapphire object */
        SapphireInstanceManager otherInstance = sapphireObjectsByName.get(name);
        if (otherInstance != null) {
            throw new MicroServiceNameModificationException(otherInstance.getOid(), name);
        }

        synchronized (instance) {
            if (instance.getReferenceCount() != 0) {
                sapphireObjectsByName.put(name, instance);
                instance.setName(name);
            }
        }
    }

    /**
     * Removes the replica of sapphire object
     *
     * @param replicaId
     * @throws MicroServiceNotFoundException
     */
    public void removeReplica(ReplicaID replicaId) throws MicroServiceNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(replicaId.getOID());
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid Sapphire object id.");
        }

        synchronized (instance) {
            instance.removeReplica(replicaId);
        }
    }

    /**
     * Set the Event handler of sapphire replica
     *
     * @param replicaId
     * @param dispatcher
     * @throws MicroServiceNotFoundException
     */
    public void setReplicaDispatcher(ReplicaID replicaId, EventHandler dispatcher)
            throws MicroServiceNotFoundException, MicroServiceReplicaNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(replicaId.getOID());
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid Sapphire object id.");
        }

        synchronized (instance) {
            if (instance.getReferenceCount() == 0) {
                /* Sapphire object could have been deleted in another thread */
                throw new MicroServiceNotFoundException("Sapphire object is deleted.");
            }
            instance.setReplicaDispatcher(replicaId, dispatcher);
        }
    }

    /**
     * Get the event handler of sapphire object
     *
     * @param microServiceId
     * @return
     * @throws MicroServiceNotFoundException
     * @deprecated
     */
    public EventHandler getInstanceDispatcher(MicroServiceID microServiceId)
            throws MicroServiceNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(microServiceId);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid Sapphire object id.");
        }

        return instance.getInstanceDispatcher();
    }

    /**
     * Get the object stub of sapphire object
     *
     * @param microServiceId
     * @return
     * @throws MicroServiceNotFoundException
     */
    public AppObjectStub getInstanceObjectStub(MicroServiceID microServiceId)
            throws MicroServiceNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(microServiceId);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid Sapphire object id.");
        }

        return instance.getInstanceObjectStub();
    }

    /**
     * Get the event handler of sapphire replica
     *
     * @param replicaId
     * @return
     * @throws MicroServiceNotFoundException
     */
    public EventHandler getReplicaDispatcher(ReplicaID replicaId)
            throws MicroServiceNotFoundException, MicroServiceReplicaNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(replicaId.getOID());
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid Sapphire object id.");
        }

        return instance.getReplicaDispatcher(replicaId);
    }

    /**
     * Get sapphire instance id by name
     *
     * @param sapphireObjName
     * @return
     * @throws MicroServiceNotFoundException
     */
    public MicroServiceID getSapphireInstanceIdByName(String sapphireObjName)
            throws MicroServiceNotFoundException {
        SapphireInstanceManager instance = sapphireObjectsByName.get(sapphireObjName);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid Sapphire object id.");
        }

        return instance.getOid();
    }

    /**
     * Get sapphire replicas by id
     *
     * @param oid
     * @return
     * @throws MicroServiceNotFoundException
     */
    public EventHandler[] getSapphireReplicasById(MicroServiceID oid)
            throws MicroServiceNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(oid);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid Sapphire object id.");
        }

        return instance.getReplicas();
    }

    public int incrRefCountAndGet(MicroServiceID microServiceId)
            throws MicroServiceNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(microServiceId);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid Sapphire object id.");
        }
        synchronized (instance) {
            if (instance.getReferenceCount() == 0) {
                throw new MicroServiceNotFoundException("Sapphire object is deleted.");
            }
            return instance.incrRefCountAndGet();
        }
    }

    public int decrRefCountAndGet(MicroServiceID microServiceId)
            throws MicroServiceNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(microServiceId);
        if (instance == null) {
            throw new MicroServiceNotFoundException("Not a valid Sapphire object id.");
        }
        return instance.decrRefCountAndGet();
    }
}

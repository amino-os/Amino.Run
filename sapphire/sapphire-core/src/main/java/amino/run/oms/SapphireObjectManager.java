package amino.run.oms;

import amino.run.common.AppObjectStub;
import amino.run.common.SapphireObjectID;
import amino.run.common.SapphireObjectNameModificationException;
import amino.run.common.SapphireObjectNotFoundException;
import amino.run.common.SapphireObjectReplicaNotFoundException;
import amino.run.common.SapphireReplicaID;
import amino.run.policy.Policy;
import amino.run.runtime.EventHandler;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SapphireObjectManager {
    private ConcurrentHashMap<SapphireObjectID, SapphireInstanceManager> sapphireObjects;
    private ConcurrentHashMap<String, SapphireInstanceManager> sapphireObjectsByName;

    /**
     * Randomly generate a new sapphire object id
     *
     * @return
     */
    private SapphireObjectID generateSapphireObjectID() {
        return new SapphireObjectID(UUID.randomUUID());
    }

    public SapphireObjectManager() {
        sapphireObjects = new ConcurrentHashMap<SapphireObjectID, SapphireInstanceManager>();
        sapphireObjectsByName = new ConcurrentHashMap<String, SapphireInstanceManager>();
    }

    /**
     * Generates a sapphire object id and add new sapphire object
     *
     * @param dispatcher
     * @return Returns a new sapphire object id
     */
    public SapphireObjectID addInstance(EventHandler dispatcher) {
        SapphireObjectID oid = generateSapphireObjectID();
        SapphireInstanceManager instance = new SapphireInstanceManager(oid, dispatcher);
        sapphireObjects.put(oid, instance);
        return oid;
    }

    /**
     * Set the Event handler of sapphire object
     *
     * @param sapphireObjId
     * @param dispatcher
     * @throws SapphireObjectNotFoundException
     */
    public void setInstanceDispatcher(SapphireObjectID sapphireObjId, EventHandler dispatcher)
            throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(sapphireObjId);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }
        instance.setInstanceDispatcher(dispatcher);
    }

    /**
     * Gets the root group policy object with sapphire object id
     *
     * @param oid
     * @return Sapphire Group Policy Object
     * @throws SapphireObjectNotFoundException
     */
    public Policy.GroupPolicy getRootGroupPolicy(SapphireObjectID oid)
            throws SapphireObjectNotFoundException {
        SapphireInstanceManager instanceManager = sapphireObjects.get(oid);
        if (instanceManager == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }
        return instanceManager.getRootGroupPolicy();
    }

    public void setRootGroupPolicy(SapphireObjectID oid, Policy.GroupPolicy rootGroupPolicy)
            throws SapphireObjectNotFoundException {
        SapphireInstanceManager instanceManager = sapphireObjects.get(oid);
        if (instanceManager == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }
        instanceManager.setRootGroupPolicy(rootGroupPolicy);
    }

    /**
     * Set the object stub of sapphire object
     *
     * @param sapphireObjId
     * @param objectStub
     * @throws SapphireObjectNotFoundException
     */
    public void setInstanceObjectStub(SapphireObjectID sapphireObjId, AppObjectStub objectStub)
            throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(sapphireObjId);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }
        instance.setInstanceObjectStub(objectStub);
    }

    /**
     * Adds a sapphire replica of given sapphire object
     *
     * @param sapphireObjId
     * @param dispatcher
     * @return Returns a new sapphire replica id
     * @throws SapphireObjectNotFoundException
     */
    public SapphireReplicaID addReplica(SapphireObjectID sapphireObjId, EventHandler dispatcher)
            throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(sapphireObjId);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }

        synchronized (instance) {
            if (instance.getReferenceCount() == 0) {
                /* Sapphire object could have been deleted in another thread */
                throw new SapphireObjectNotFoundException("Sapphire object is deleted.");
            }
            return instance.addReplica(dispatcher);
        }
    }

    /**
     * Removes the sapphire object with the given ID.
     *
     * @param sapphireObjId sapphire object ID
     * @throws SapphireObjectNotFoundException
     */
    public void removeInstance(SapphireObjectID sapphireObjId)
            throws SapphireObjectNotFoundException, RemoteException {
        SapphireInstanceManager instanceManager = sapphireObjects.get(sapphireObjId);
        if (instanceManager == null) {
            throw new SapphireObjectNotFoundException(
                    "Cannot find sapphire object with ID " + sapphireObjId);
        }

        instanceManager.clear();

        if (instanceManager.getName() != null) {
            sapphireObjectsByName.remove(instanceManager.getName());
        }
        sapphireObjects.remove(sapphireObjId);
    }

    /**
     * get all the sapphire objects
     *
     * @throws java.rmi.RemoteException
     */
    public ArrayList<SapphireObjectID> getAllSapphireObjects() throws RemoteException {
        ArrayList<SapphireObjectID> arr = new ArrayList<SapphireObjectID>(sapphireObjects.keySet());
        return arr;
    }

    /**
     * Sets the name to sapphire object
     *
     * @param sapphireObjId
     * @param name
     * @throws SapphireObjectNotFoundException
     */
    public void setInstanceName(SapphireObjectID sapphireObjId, String name)
            throws SapphireObjectNotFoundException, SapphireObjectNameModificationException {
        SapphireInstanceManager instance = sapphireObjects.get(sapphireObjId);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }

        /* Object name is not allowed to change once set. Because reference count are updated based
        on attachByName and detachByName. And name change would affect it */
        if (instance.getName() != null) {
            throw new SapphireObjectNameModificationException(sapphireObjId, instance.getName());
        }

        /* This name is already used for some other sapphire object */
        SapphireInstanceManager otherInstance = sapphireObjectsByName.get(name);
        if (otherInstance != null) {
            throw new SapphireObjectNameModificationException(otherInstance.getOid(), name);
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
     * @throws SapphireObjectNotFoundException
     */
    public void removeReplica(SapphireReplicaID replicaId) throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(replicaId.getOID());
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
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
     * @throws SapphireObjectNotFoundException
     */
    public void setReplicaDispatcher(SapphireReplicaID replicaId, EventHandler dispatcher)
            throws SapphireObjectNotFoundException, SapphireObjectReplicaNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(replicaId.getOID());
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }

        synchronized (instance) {
            if (instance.getReferenceCount() == 0) {
                /* Sapphire object could have been deleted in another thread */
                throw new SapphireObjectNotFoundException("Sapphire object is deleted.");
            }
            instance.setReplicaDispatcher(replicaId, dispatcher);
        }
    }

    /**
     * Get the event handler of sapphire object
     *
     * @param sapphireObjId
     * @return
     * @throws SapphireObjectNotFoundException
     * @deprecated
     */
    public EventHandler getInstanceDispatcher(SapphireObjectID sapphireObjId)
            throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(sapphireObjId);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }

        return instance.getInstanceDispatcher();
    }

    /**
     * Get the object stub of sapphire object
     *
     * @param sapphireObjId
     * @return
     * @throws SapphireObjectNotFoundException
     */
    public AppObjectStub getInstanceObjectStub(SapphireObjectID sapphireObjId)
            throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(sapphireObjId);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }

        return instance.getInstanceObjectStub();
    }

    /**
     * Get the event handler of sapphire replica
     *
     * @param replicaId
     * @return
     * @throws SapphireObjectNotFoundException
     */
    public EventHandler getReplicaDispatcher(SapphireReplicaID replicaId)
            throws SapphireObjectNotFoundException, SapphireObjectReplicaNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(replicaId.getOID());
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }

        return instance.getReplicaDispatcher(replicaId);
    }

    /**
     * Get sapphire instance id by name
     *
     * @param sapphireObjName
     * @return
     * @throws SapphireObjectNotFoundException
     */
    public SapphireObjectID getSapphireInstanceIdByName(String sapphireObjName)
            throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjectsByName.get(sapphireObjName);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }

        return instance.getOid();
    }

    /**
     * Get sapphire replicas by id
     *
     * @param oid
     * @return
     * @throws SapphireObjectNotFoundException
     */
    public EventHandler[] getSapphireReplicasById(SapphireObjectID oid)
            throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(oid);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }

        return instance.getReplicas();
    }

    public int incrRefCountAndGet(SapphireObjectID sapphireObjId)
            throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(sapphireObjId);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }
        synchronized (instance) {
            if (instance.getReferenceCount() == 0) {
                throw new SapphireObjectNotFoundException("Sapphire object is deleted.");
            }
            return instance.incrRefCountAndGet();
        }
    }

    public int decrRefCountAndGet(SapphireObjectID sapphireObjId)
            throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(sapphireObjId);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }
        return instance.decrRefCountAndGet();
    }
}

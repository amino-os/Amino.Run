package sapphire.oms;

import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNameModificationException;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.common.SapphireReplicaID;
import sapphire.runtime.EventHandler;

public class SapphireObjectManager {
    private ConcurrentHashMap<SapphireObjectID, SapphireInstanceManager> sapphireObjects;
    private ConcurrentHashMap<String, SapphireInstanceManager> sapphireObjectsByName;
    private Random oidGenerator;

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
        oidGenerator = new Random(new Date().getTime());
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
     * Removes the sapphire object
     *
     * @param sapphireObjId
     * @throws SapphireObjectNotFoundException
     */
    public void removeInstance(SapphireObjectID sapphireObjId)
            throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(sapphireObjId);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }

        instance.clear();
        if (instance.getName() != null) {
            sapphireObjectsByName.remove(instance.getName());
        }
        sapphireObjects.remove(sapphireObjId);
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

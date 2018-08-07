package sapphire.oms;

import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireReplicaID;
import sapphire.runtime.EventHandler;

public class SapphireObjectManager {
    private HashMap<SapphireObjectID, SapphireInstanceManager> sapphireObjects;
    private HashMap<String, SapphireObjectID> sapphireObjectNameToIDMap;
    private Random oidGenerator;

    /**
     * Randomly generate a new sapphire object id
     *
     * @return
     */
    private SapphireObjectID generateSapphireObjectID() {
        return new SapphireObjectID(oidGenerator.nextInt());
    }

    public SapphireObjectManager() {
        sapphireObjects = new HashMap<SapphireObjectID, SapphireInstanceManager>();
        sapphireObjectNameToIDMap = new HashMap<String, SapphireObjectID>();
        oidGenerator = new Random(new Date().getTime());
    }

    /**
     * Adds a new sapphire instance
     *
     * @param dispatcher
     * @return Returns a new sapphire object id
     */
    public SapphireObjectID add(EventHandler dispatcher) {
        SapphireObjectID oid = generateSapphireObjectID();
        SapphireInstanceManager instance = new SapphireInstanceManager(oid, dispatcher);
        sapphireObjects.put(oid, instance);
        return oid;
    }

    /**
     * Set the Event handler of the sapphire instance
     *
     * @param sapphireObjId
     * @param dispatcher
     * @throws SapphireObjectNotFoundException
     */
    public void set(SapphireObjectID sapphireObjId, EventHandler dispatcher)
            throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(sapphireObjId);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }
        instance.setInstanceDispatcher(dispatcher);
    }

    /**
     * Generates a new sapphire replica id of the given sapphire instance
     *
     * @param sapphireObjId
     * @param dispatcher
     * @return Returns a new sapphire replica id
     * @throws SapphireObjectNotFoundException
     */
    public SapphireReplicaID add(SapphireObjectID sapphireObjId, EventHandler dispatcher)
            throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(sapphireObjId);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }
        return instance.addReplica(dispatcher);
    }

    /**
     * Set the Event handler of the sapphire replica
     *
     * @param replicaId
     * @param dispatcher
     * @throws SapphireObjectNotFoundException
     */
    public void set(SapphireReplicaID replicaId, EventHandler dispatcher)
            throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(replicaId.getOID());
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }

        instance.setReplicaDispatcher(replicaId, dispatcher);
    }

    public void remove(SapphireObjectID oid) throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(oid);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }

        instance.clear();
        sapphireObjects.remove(oid);
    }

    public void setName(SapphireObjectID oid, String name) throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(oid);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }

        sapphireObjectNameToIDMap.put(name, oid);
        instance.setName(name);
    }

    public void remove(SapphireReplicaID oid) throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(oid.getOID());
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }

        instance.removeReplica(oid);
    }

    /**
     * Get the event handler of the Sapphire instance
     *
     * @param sapphireObjId
     * @return
     * @throws SapphireObjectNotFoundException
     */
    public EventHandler get(SapphireObjectID sapphireObjId) throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(sapphireObjId);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }
        return instance.getInstanceDispatcher();
    }

    /**
     * Get the event handler of the Sapphire replica
     *
     * @param replicaId
     * @return
     * @throws SapphireObjectNotFoundException
     */
    public EventHandler get(SapphireReplicaID replicaId) throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(replicaId.getOID());
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }
        return instance.getReplicaDispatcher(replicaId);
    }

    public SapphireObjectID get(String sapphireObjName) throws SapphireObjectNotFoundException {
        SapphireObjectID instance = sapphireObjectNameToIDMap.get(sapphireObjName);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }
        return instance;
    }

    public SapphireInstanceManager getSapphireInstanceManager(SapphireObjectID oid)
            throws SapphireObjectNotFoundException {
        return sapphireObjects.get(oid);
    }
}

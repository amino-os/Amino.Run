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
        oidGenerator = new Random(new Date().getTime());
    }

    /**
     * Generates a sapphire object id and add new sapphire object
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
     * Set the Event handler of sapphire object
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
     * Adds a sapphire replica of given sapphire object
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
     * Set the Event handler of sapphire replica
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

    /**
     * Get the event handler of sapphire object
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
     * Get the event handler of sapphire replica
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
}

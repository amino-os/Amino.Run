package sapphire.oms;

import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireReplicaID;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.runtime.EventHandler;

public class SapphireObjectManager {
    private HashMap<SapphireObjectID, SapphireInstanceManager> sapphireObjects;
    private HashMap<String, SapphireObjectID> sapphireObjectNameToIDMap;
    private Random oidGenerator;

    /**
     * Randomly generate a new kernel object id
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
     * Add the event handler for the sapphire object
     *
     * @param dispatcher
     * @return
     */
    public SapphireObjectID add(EventHandler dispatcher) {
        SapphireObjectID oid = generateSapphireObjectID();
        SapphireInstanceManager instance = new SapphireInstanceManager(oid, dispatcher);
        sapphireObjects.put(oid, instance);
        return oid;
    }

    /**
     * Set the Event handler for the sapphire object
     *
     * @param dispatcher
     * @return
     */
    public void set(SapphireObjectID oid, EventHandler dispatcher)
            throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(oid);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }
        instance.setInstanceDispatcher(dispatcher);
    }

    public SapphireReplicaID add(SapphireObjectID oid, EventHandler dispatcher)
            throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(oid);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }
        return instance.addReplica(dispatcher);
    }

    public void set(SapphireReplicaID oid, EventHandler dispatcher)
            throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(oid.getOID());
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }

        instance.setReplicaDispatcher(oid, dispatcher);
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
     * Get the event handler for a Sapphire instance
     *
     * @param oid
     * @return
     * @throws KernelObjectNotFoundException
     */
    public EventHandler get(SapphireObjectID oid) throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(oid);
        if (instance == null) {
            throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
        }
        return instance.getInstanceDispatcher();
    }

    public EventHandler get(SapphireReplicaID rid) throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance = sapphireObjects.get(rid.getOID());
        return instance.getReplicaDispatcher(rid);
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

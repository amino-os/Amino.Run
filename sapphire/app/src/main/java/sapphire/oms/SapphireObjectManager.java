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
    private HashMap<SapphireObjectID,SapphireInstanceManager> sapphireObjects;
    private Random oidGenerator;

    /**
     * Randomly generate a new kernel object id
     * @return
     */
    private SapphireObjectID generateSapphireObjectID() {
        return new SapphireObjectID(oidGenerator.nextInt());
    }

    public SapphireObjectManager() {
        sapphireObjects = new HashMap<SapphireObjectID,SapphireInstanceManager>();
        oidGenerator = new Random(new Date().getTime());
    }

    /**
     * Register a new kernel object
     * @param host
     * @return
     */
    public SapphireObjectID add(EventHandler dispatcher) {
        SapphireObjectID oid = generateSapphireObjectID();
        SapphireInstanceManager instance = new SapphireInstanceManager(oid, dispatcher);
        sapphireObjects.put(oid, instance);
        return oid;
    }
    
    public SapphireReplicaID add(SapphireObjectID oid, EventHandler dispatcher) throws SapphireObjectNotFoundException {
    	SapphireInstanceManager instance = sapphireObjects.get(oid);
    	if (instance == null) {
    		throw new SapphireObjectNotFoundException("Not a valid Sapphire object id.");
    	}
    	return instance.addReplica(dispatcher);
    }
        
    /**
     * Get the event handler for a Sapphire instance
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
}
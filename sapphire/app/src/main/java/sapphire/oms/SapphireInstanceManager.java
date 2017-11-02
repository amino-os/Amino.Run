package sapphire.oms;

import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireReplicaID;
import sapphire.runtime.EventHandler;

public class SapphireInstanceManager {

	private SapphireObjectID oid;
	private EventHandler instanceDispatcher;
	private HashMap<SapphireReplicaID,EventHandler> replicaDispatchers;
	private Random oidGenerator;

	/**
     * Randomly generate a new replica id
     * @return
     */
    private SapphireReplicaID generateSapphireReplicaID() {
        return new SapphireReplicaID(oid, oidGenerator.nextInt());
    }

	
	public SapphireInstanceManager(SapphireObjectID oid, EventHandler dispatcher) {
		this.oid = oid;
		instanceDispatcher = dispatcher;
		replicaDispatchers = new HashMap<SapphireReplicaID,EventHandler>();
		oidGenerator = new Random(new Date().getTime());
	}
	
	public EventHandler getInstanceDispatcher() {
		return instanceDispatcher;
	}
	
	public EventHandler getReplicaDispatcher(SapphireReplicaID rid) throws SapphireObjectNotFoundException {
		EventHandler dispatcher = replicaDispatchers.get(rid);
    	if (dispatcher == null) {
    		throw new SapphireObjectNotFoundException("Not a valid kernel object id.");
    	}
    	return dispatcher;
	}
	
	public SapphireReplicaID addReplica(EventHandler dispatcher) {
		SapphireReplicaID rid = generateSapphireReplicaID();
		replicaDispatchers.put(rid, dispatcher);
		return rid;
	}
}

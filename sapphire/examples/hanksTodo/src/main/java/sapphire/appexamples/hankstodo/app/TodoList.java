package sapphire.appexamples.hankstodo.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import sapphire.app.*;
import sapphire.policy.cache.CacheLeasePolicy;
import sapphire.policy.interfaces.dht.DHTInterface;
import sapphire.policy.interfaces.dht.DHTKey;
import sapphire.runtime.SapphireConfiguration;

//@SapphireConfiguration(DMs = "sapphire.policy.atleastoncerpc.AtLeastOnceRPCPolicy, sapphire.policy.atleastoncerpc.AtLeastOnceRPCPolicy")
@SapphireConfiguration(DMs = "sapphire.policy.dht.DHTPolicy2,sapphire.policy.atleastoncerpc.AtLeastOnceRPCPolicy")
//@SapphireConfiguration(DMs = "sapphire.policy.dht.DHTPolicy2,sapphire.policy.replication.ConsensusRSMPolicy")

public class TodoList implements SapphireObject, DHTInterface {
	ArrayList<String> toDos = new ArrayList<String>();
	String id = "0";

	public TodoList(String id) {
		toDos = new ArrayList<String>();
		this.id = id;
	}

	/**
	 * TODO: id should not be required. This is in place just to mimic hash key that is passed to group policy for finding a responsible node.
	 * @param id
	 * @return
	 */
	public String addToDo(int id, String todo) {
		System.out.println("TodoList>> id: " + id + " addToDo: " + todo);
		toDos.add(todo);
		return "OK!";
	}

	/**
	 * TODO: id should not be required. This is in place just to mimic hash key that is passed to group policy for finding a responsible node.
	 * @param id
	 * @return
	 */
	public String getToDoString(int id) {
		StringBuilder sb = new StringBuilder();

		for (String toDo: toDos) {
			sb.append(toDo);
			sb.append(" : ");
		}

		sb.append("\n");
		return sb.toString();
	}

	// Added for testing DHTPolicy. -->
	Map<DHTKey, String> toDoItems = new Hashtable<DHTKey, String>();
//
//	public void addToDoItem (int id, String toDo) {
//		DHTKey newKey = new DHTKey(id);
//		String toDoItem = toDoItems.get(newKey);
//
//		if (toDoItem == null) {
//			toDoItems.put(newKey, toDo);
//		}
//		System.out.println("Adding new toDo item: " + toDo);
//	}
//	public String getToDoItem(String id) {
//		String toDo = toDoItems.get(new DHTKey(id));
//		return toDo;
//	}
	@Override
	public Map<DHTKey, ?> dhtGetData() {
		return toDoItems;
	}
	//<-- End of changes for testing DHTPolicy.
}

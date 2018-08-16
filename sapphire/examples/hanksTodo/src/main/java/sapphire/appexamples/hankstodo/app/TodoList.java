package sapphire.appexamples.hankstodo.app;

import java.io.Serializable;
import java.util.ArrayList;

import sapphire.app.*;
import sapphire.policy.cache.CacheLeasePolicy;
import sapphire.runtime.SapphireConfiguration;

//@SapphireConfiguration(DMs = "sapphire.policy.DefaultSapphirePolicy,sapphire.policy.dht.DHTPolicy2")
@SapphireConfiguration(DMs = "sapphire.policy.dht.DHTPolicy2,sapphire.policy.atleastoncerpc.AtLeastOnceRPCPolicy")
//@SapphireConfiguration(DMs = "sapphire.policy.DefaultSapphirePolicy,sapphire.policy.DefaultSapphirePolicy")
public class TodoList implements SapphireObject {
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
	public String addToDo(String id, String todo) {
		System.out.println("id: " + id + " addToDo: " + todo);
		toDos.add(todo);
		return "OK!";
	}

	/**
	 * TODO: id should not be required. This is in place just to mimic hash key that is passed to group policy for finding a responsible node.
	 * @param id
	 * @return
	 */
	public String getToDoString(String id) {
		StringBuilder sb = new StringBuilder();

		for (String toDo: toDos) {
			sb.append(toDo);
			sb.append(" : ");
		}

		sb.append("\n");
		return sb.toString();
	}
}

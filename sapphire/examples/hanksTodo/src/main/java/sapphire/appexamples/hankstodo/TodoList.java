package sapphire.appexamples.hankstodo;

import java.util.ArrayList;

import sapphire.app.*;
import sapphire.runtime.SapphireConfiguration;

@SapphireConfiguration(Policies = "sapphire.policy.atleastoncerpc.AtLeastOnceRPCPolicy,sapphire.policy.replication.ConsensusRSMPolicy")
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

	public void completeToDo(String todo) {
	}

	public ArrayList<Object> getHighPriority() {
		return new ArrayList<Object>();
	}
}

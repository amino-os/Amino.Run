package sapphire.appexamples.hankstodo;

import java.util.ArrayList;

import sapphire.app.*;
import sapphire.runtime.SapphireConfiguration;

public class TodoList implements SapphireObject {
	ArrayList<String> toDos = new ArrayList<String>();
	String id = "0";

	public TodoList(String id) {
		toDos = new ArrayList<String>();
		this.id = id;
	}

	/**
	 * TODO: With DHTPolicy, TODO item should add with id so that it can be pulled by id.
	 * @param id
	 * @return
	 */
	public String addToDo(String id, String todo) {
		System.out.println("TodoList>> id: " + id + " addToDo: " + todo);
		toDos.add(todo);
		return "OK!";
	}

	/**
	 * TODO: With DHTPolicy, TODOs should be picked up based on the input param - id.
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

	public void completeToDo(String todo) {
	}

	public ArrayList<Object> getHighPriority() {
		return new ArrayList<Object>();
	}
}

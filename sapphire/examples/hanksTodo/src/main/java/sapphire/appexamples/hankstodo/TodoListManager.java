package sapphire.appexamples.hankstodo;


import sapphire.app.DMSpec;
import sapphire.app.Language;
import sapphire.app.SapphireObject;
import static sapphire.runtime.Sapphire.*;

import sapphire.app.SapphireObjectSpec;
import sapphire.policy.dht.DHTPolicy2;
import sapphire.policy.replication.ConsensusRSMPolicy;

import java.util.Hashtable;
import java.util.Map;

public class TodoListManager implements SapphireObject {
	Map<String, TodoList> todoLists = new Hashtable<String, TodoList>();

	public TodoListManager() {
		System.out.println("Instantiating TodoListManager...");
	}

	public void doSomething(String input) {
		System.out.println("Input received: " + input);
	}

	public TodoList newTodoList(String id) {
		TodoList t = todoLists.get(id);
		if (t == null) {
			SapphireObjectSpec spec;
			spec = SapphireObjectSpec.newBuilder()
					.setLang(Language.java)
					.setJavaClassName(TodoList.class.getName())
					.addDMSpec(
							DMSpec.newBuilder()
									.setName(DHTPolicy2.class.getName())
									.create())
					.addDMSpec(
							DMSpec.newBuilder()
									.setName(ConsensusRSMPolicy.class.getName())
									.create())
					.create();

			t = (TodoList) new_(spec, id);
			todoLists.put(id, t);
			System.out.println("Created new Todo list");
		} else {
			System.out.println("ToDoList for ID: "+ id + " already exists.");
		}
		System.out.println("Created new list");
		System.out.println("This managers lists" + todoLists.toString());
		return t;
	}

	public TodoList getToDoList(String id) {
		TodoList t = todoLists.get(id);
		if (t == null) {
			return null;
		}
		return t;
	}

	public void deleteTodoList(String name) {
		TodoList t = todoLists.remove(name);
		if (t != null) {
			delete_(t);
		}
	}
}
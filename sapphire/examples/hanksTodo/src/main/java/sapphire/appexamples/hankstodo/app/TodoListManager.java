package sapphire.appexamples.hankstodo.app;

import java.util.Hashtable;
import java.util.Map;

import sapphire.app.SapphireObject;
import static sapphire.runtime.Sapphire.*;

import sapphire.policy.interfaces.dht.DHTInterface;
import sapphire.policy.interfaces.dht.DHTKey;
import sapphire.runtime.SapphireConfiguration;

//@SapphireConfiguration(DMs = "sapphire.policy.atleastoncerpc.AtLeastOnceRPCPolicy,sapphire.policy.atleastoncerpc.AtLeastOnceRPCPolicy,sapphire.policy.mobility.explicitmigration.ExplicitMigrationPolicy")
@SapphireConfiguration(DMs = "sapphire.policy.DefaultSapphirePolicy")
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
			t = (TodoList) new_(TodoList.class, id);
			todoLists.put(id, t);
			System.out.println("Created new Todo list");
		} else {
			System.out.println("ToDoList for ID: "+ id + " already exists.");
		}

		return t;
	}

	public TodoList getToDoList(String id) {
		TodoList t = todoLists.get(id);
		if (t == null) {
			return null;
		}
		return t;
	}
}

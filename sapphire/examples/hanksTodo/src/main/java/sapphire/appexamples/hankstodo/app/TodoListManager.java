package sapphire.appexamples.hankstodo.app;

import java.util.Hashtable;
import java.util.Map;

import sapphire.app.SapphireObject;
import static sapphire.runtime.Sapphire.*;

import sapphire.policy.interfaces.dht.DHTInterface;
import sapphire.policy.interfaces.dht.DHTKey;
import sapphire.runtime.SapphireConfiguration;

@SapphireConfiguration(DMs = "sapphire.policy.atleastoncerpc.AtLeastOnceRPCPolicy,sapphire.policy.atleastoncerpc.AtLeastOnceRPCPolicy,sapphire.policy.mobility.explicitmigration.ExplicitMigrationPolicy")
public class TodoListManager implements SapphireObject, DHTInterface {
    Map<DHTKey, TodoList> todoLists = new Hashtable<DHTKey, TodoList>();

	public TodoListManager() {
		System.out.println("Instantiating TodoListManager...");
	}

	public void doSomething(String input) {
		System.out.println("Input received: " + input);
	}

	public TodoList newTodoList(String name) {
		TodoList t = todoLists.get(new DHTKey(name));
		if (t == null) {
			t = (TodoList) new_(TodoList.class, name);
			todoLists.put(new DHTKey(name), t);
		}
		System.out.println("Created new list");
		System.out.println("This managers lists" + todoLists.toString());
		return t;
	}

	@Override
	public Map<DHTKey, ?> dhtGetData() {
		// TODO Auto-generated method stub
		return todoLists;
	}
}

package sapphire.appexamples.hankstodo.app;

import java.util.Hashtable;
import java.util.Map;

import sapphire.app.SapphireObject;
import static sapphire.runtime.Sapphire.*;
import sapphire.policy.dht.DHTPolicy;
import sapphire.policy.interfaces.dht.DHTInterface;
import sapphire.policy.interfaces.dht.DHTKey;

public class TodoListManager implements SapphireObject<DHTPolicy>, DHTInterface {
    Map<DHTKey, TodoList> todoLists = new Hashtable<DHTKey, TodoList>();

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

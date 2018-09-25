package sapphire.appexamples.hankstodo;

import java.util.Hashtable;
import java.util.Map;

import sapphire.app.SapphireObject;
import static sapphire.runtime.Sapphire.*;
import sapphire.policy.dht.DHTPolicy;
import sapphire.policy.dht.DHTKey;

public class TodoListManager implements SapphireObject<DHTPolicy> {
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

	public void deleteTodoList(String name) {
		TodoList t = todoLists.remove(new DHTKey(name));
		if (t != null) {
			delete_(t);
		}
	}
}

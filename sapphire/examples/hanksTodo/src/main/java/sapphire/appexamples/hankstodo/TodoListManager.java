package sapphire.appexamples.hankstodo;


import sapphire.app.SapphireObject;
import static sapphire.runtime.Sapphire.*;

import sapphire.runtime.SapphireConfiguration;

import java.util.Hashtable;
import java.util.Map;

@SapphireConfiguration(Policies = "sapphire.policy.DefaultSapphirePolicy")
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
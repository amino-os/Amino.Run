package amino.run.appexamples.hankstodo;


import java.util.Hashtable;
import java.util.Map;

import amino.run.app.DMSpec;
import amino.run.app.Language;
import amino.run.app.SapphireObject;
import amino.run.app.MicroServiceSpec;
import amino.run.common.MicroServiceCreationException;
import amino.run.policy.dht.DHTPolicy;
import amino.run.policy.replication.ConsensusRSMPolicy;

import static amino.run.runtime.Sapphire.delete_;
import static amino.run.runtime.Sapphire.new_;

public class TodoListManager implements SapphireObject {
	Map<String, TodoList> todoLists = new Hashtable<String, TodoList>();

	public TodoListManager() {
		System.out.println("Instantiating TodoListManager...");
	}

	public void doSomething(String input) {
		System.out.println("Input received: " + input);
	}

	public TodoList newTodoList(String id) throws MicroServiceCreationException {
		TodoList t = todoLists.get(id);
		if (t == null) {
			MicroServiceSpec spec;
			spec = MicroServiceSpec.newBuilder()
					.setLang(Language.java)
					.setJavaClassName(TodoList.class.getName())
					.addDMSpec(
							DMSpec.newBuilder()
									.setName(DHTPolicy.class.getName())
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
package sapphire.appexamples.hankstodo.app;

import java.io.Serializable;
import java.util.ArrayList;

import sapphire.app.*;
import sapphire.policy.cache.CacheLeasePolicy;
import sapphire.policy.transaction.TwoPCCoordinatorPolicy;

import static sapphire.runtime.Sapphire.new_;

public class TodoList implements SapphireObject<TwoPCCoordinatorPolicy>{
	ArrayList<Object> toDos = new ArrayList<Object>();
	String name = "Hanks todo";
	Doer doer;

	public TodoList(String name) {
		toDos = new ArrayList<Object>();
		this.name = name;
		//this.doer = new Doer();
		this.doer = (Doer) new_(Doer.class);
	}

	public String addToDo(String todo) {
		System.out.println("Inside todo: " + todo);
		toDos.add(todo);
		this.doer.setDoer("who will do " + "todo");
		return "OK!";
	}

	public void completeToDo(String todo) {
	}

	public ArrayList<Object> getHighPriority() {
		return new ArrayList<Object>();
	}
}

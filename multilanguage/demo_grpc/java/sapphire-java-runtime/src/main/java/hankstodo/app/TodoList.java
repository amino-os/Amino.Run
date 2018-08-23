package hankstodo.app;

import java.util.ArrayList;

public class TodoList {
	ArrayList<Object> toDos = new ArrayList<Object>();
	String name = "Hanks todo";

	public TodoList(String name) {
		toDos = new ArrayList<Object>();
		this.name = name;
	}

    public String getName() {
        return name;
    }

	public String addToDo(String todo) {
		System.out.println("Inside todo: " + todo);
		toDos.add(todo);
		return "OK!";
	}

	public void completeToDo(String todo) {
	}

	public ArrayList<Object> getHighPriority() {
		return new ArrayList<Object>();
	}
}

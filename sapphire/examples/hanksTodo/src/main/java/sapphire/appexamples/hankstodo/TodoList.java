package sapphire.appexamples.hankstodo;

import java.util.ArrayList;
import java.util.HashMap;

import sapphire.app.*;
import sapphire.runtime.SapphireConfiguration;

public class TodoList implements SapphireObject {
    HashMap<String, String> toDos;
    String id = "0";

    public TodoList(String id) {
        toDos = new HashMap<>();
        this.id = id;
    }

    /**
     * Add to do list content with subject and content.
     *
     * @param subject
     * @param content
     * @return
     */
    public String addToDo(String subject, String content) {
        System.out.println("TodoList>> subject: " + subject + " addToDo: " + content);
        String oldContent = toDos.get(subject);
        oldContent = (oldContent == null) ? "" : oldContent + ", ";
        String newContent = oldContent + content;
        toDos.put(subject, newContent);
        return "OK!";
    }

    /**
     * Get to do list content based on subject.
     *
     * @param subject
     * @return
     */
    public String getToDo(String subject) {
        return toDos.get(subject);
    }

    public void completeToDo(String content) {}

    public ArrayList<Object> getHighPriority() {
        return new ArrayList<Object>();
    }
}

package sapphire.appexamples.hankstodo.app;

import org.junit.Test;

import java.lang.annotation.Annotation;

import static org.junit.Assert.*;

/**
 * Created by SMoon on 6/4/2018.
 */
public class TodoListManagerTest {
    @Test
    public void newTodoList() throws Exception {
        Annotation[] annotations = TodoListManager.class.getAnnotations();
        System.out.println(annotations);
    }

}
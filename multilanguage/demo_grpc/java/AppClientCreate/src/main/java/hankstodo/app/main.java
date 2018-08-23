package hankstodo.app;

import hankstodo.app.grpcStubs.TodoListManager_Stub;
import hankstodo.app.grpcStubs.TodoList_Stub;

/**
 * Created with IntelliJ IDEA.
 * User: root1
 * Date: 4/8/18
 * Time: 2:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class main {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Incorrect number of arguments to the App Client");
            System.out.println("[host ip] [host port] [grpc-serverip] [grpc-port]");
            return;
        }

        AppGrpcClient grpcClient = new AppGrpcClient(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]));
        GlobalGrpcClientRef.grpcClient = grpcClient;
        TodoListManager_Stub tlm;
        try {
            tlm = new TodoListManager_Stub();
            System.out.println("Finished");
            TodoList_Stub tl = tlm.newTodoList("Hanks");
            System.out.println("Received tl1: " + tl);
            System.out.println(tl.addToDo("First todo"));
            System.out.println(tl.addToDo("Second todo"));
            System.out.println(tl.addToDo("Third todo"));

            TodoList_Stub tl2 = tlm.newTodoList("AAA");
            System.out.println("Received tl2: " + tl2);
            System.out.println(tl2.addToDo("First todo"));
            System.out.println(tl2.addToDo("Second todo"));
            System.out.println(tl2.addToDo("Third todo"));

            TodoList_Stub tl3 = tlm.newTodoList("HHH");
            System.out.println("Received tl3: " + tl3);
            System.out.println(tl3.addToDo("First todo"));
            System.out.println(tl3.addToDo("Second todo"));
            System.out.println(tl3.addToDo("Third todo"));

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}

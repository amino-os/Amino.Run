package amino.run.appexamples.hankstodo;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.app.SapphireObjectServer;
import amino.run.common.SapphireObjectID;
import amino.run.kernel.server.KernelServer;
import amino.run.kernel.server.KernelServerImpl;

import static java.lang.Thread.sleep;

public class HanksTodoMain {

    static final int REPEAT_CNT = 5;
    static final int TODO_CNT = 10;
    static final String SUBJECT_PREFIX = "Subject";
    static final String TODO_PREFIX = "Task";

    public HanksTodoMain() {}

    public static void main(String[] args) {
        String ListName = "New List 1";

        if (args.length < 4) {
            System.out.println("Incorrect arguments to the program");
            System.out.println(
                    "usage: "
                            + HanksTodoMain.class.getSimpleName()
                            + " <host-ip> <host-port> <oms ip> <oms-port>");
            System.exit(1);
        }

        Registry registry;

        try {
            registry = LocateRegistry.getRegistry(args[0], Integer.parseInt(args[1]));
            SapphireObjectServer server = (SapphireObjectServer) registry.lookup("SapphireOMS");
            System.out.println(server);

            KernelServer nodeServer =
                    new KernelServerImpl(
                            new InetSocketAddress(args[2], Integer.parseInt(args[3])),
                            new InetSocketAddress(args[0], Integer.parseInt(args[1])));

            MicroServiceSpec spec =
                    MicroServiceSpec.newBuilder()
                            .setLang(Language.java)
                            .setJavaClassName("amino.run.appexamples.hankstodo.TodoListManager")
                            .create();

            SapphireObjectID sapphireObjId = server.createSapphireObject(spec.toString());
            TodoListManager tlm = (TodoListManager) server.acquireSapphireObjectStub(sapphireObjId);
            System.out.println("Received tlm: " + tlm);

            TodoList td1 = tlm.newTodoList(ListName);

            // Consensus policy needs some time after creating new Sapphire object; otherwise,
            // leader election may fail.
            sleep(7000);
            System.out.println("new to do list for 1");

            for (int i = 0; i < REPEAT_CNT; i++) {
                // Add to-do items.
                for (int j = 0; j < TODO_CNT; j++) {
                    String subject = SUBJECT_PREFIX + j;
                    String content = String.format("%s%d<%d>", TODO_PREFIX, j, i);
                    System.out.println(
                            String.format(
                                    "Adding %s. Content: %s at iteration %d", subject, content, i));
                    td1.addToDo(subject, content);
                }
            }

            // Retrieve to-do items.
            System.out.println(
                    "Please note expected String may display incorrect values depending on the policy.");

            TodoList getTd = tlm.getToDoList(ListName);
            for (int i = 0; i < TODO_CNT; i++) {
                String expected = "";
                for (int j = 0; j < REPEAT_CNT; j++) {
                    if (j != 0) {
                        expected += ", ";
                    }
                    expected += String.format("%s%d<%d>", TODO_PREFIX, i, j);
                }
                String actual = getTd.getToDo(SUBJECT_PREFIX + i);

                System.out.println(String.format("Expected for %d: %s", i, expected));
                System.out.println(String.format("Actual   for %d: %s", i, actual));
            }

            tlm.doSomething("Testing completed.");

            // Delete the created SapphireObjects
            tlm.deleteTodoList(ListName);
            server.deleteSapphireObject(sapphireObjId);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

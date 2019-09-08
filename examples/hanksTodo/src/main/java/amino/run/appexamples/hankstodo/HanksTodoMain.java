package amino.run.appexamples.hankstodo;

import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.app.Registry;
import amino.run.common.ArgumentParser.AppArgumentParser;
import amino.run.common.MicroServiceID;
import amino.run.kernel.server.KernelServer;
import amino.run.kernel.server.KernelServerImpl;
import com.google.devtools.common.options.OptionsParser;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.Collections;

public class HanksTodoMain {

    static final int REPEAT_CNT = 5;
    static final int TODO_CNT = 10;
    static final String SUBJECT_PREFIX = "Subject";
    static final String TODO_PREFIX = "Task";

    public HanksTodoMain() {}

    public static void main(String[] args) throws Exception {
        String ListName = "New List 1";

        OptionsParser parser = OptionsParser.newOptionsParser(AppArgumentParser.class);
        if (args.length < 8) {
            System.out.println("Incorrect arguments to the program");
            printUsage(parser);
            return;
        }

        try {
            parser.parse(args);
        } catch (Exception e) {

            System.out.println("Incorrect arguments to the program");
            return;
        }

        java.rmi.registry.Registry registry;
        AppArgumentParser appArgs = parser.getOptions(AppArgumentParser.class);
        registry = LocateRegistry.getRegistry(appArgs.omsIP, appArgs.omsPort);
        Registry server = (Registry) registry.lookup("io.amino.run.oms");

        KernelServer nodeServer =
                new KernelServerImpl(
                        new InetSocketAddress(appArgs.kernelServerIP, appArgs.kernelServerPort),
                        new InetSocketAddress(appArgs.omsIP, appArgs.omsPort));

        MicroServiceSpec spec =
                MicroServiceSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("amino.run.appexamples.hankstodo.TodoListManager")
                        .create();

        MicroServiceID microServiceId = server.create(spec.toString());
        TodoListManager tlm = (TodoListManager) server.acquireStub(microServiceId);
        System.out.println("Received tlm: " + tlm);

        TodoList td1 = tlm.newTodoList(ListName);

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

        // Delete the created microservices
        tlm.deleteTodoList(ListName);
        server.delete(microServiceId);
    }

    private static void printUsage(OptionsParser parser) {
        System.out.println(
                "Usage: java -cp <classpath> "
                        + HanksTodoMain.class.getSimpleName()
                        + System.getProperty("line.separator")
                        + parser.describeOptions(
                                Collections.<String, String>emptyMap(),
                                OptionsParser.HelpVerbosity.LONG));
    }
}

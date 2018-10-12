package sapphire.appexamples.hankstodo;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Level;

import sapphire.app.Language;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.SapphireObjectID;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.oms.OMSServerImpl;

import static java.lang.Thread.sleep;

public class HanksTodoMain {

    static final int NumOfRegions = 2;
    static final int NumOfKernelServersPerRegion = 2;
    static final String RegionsNameBase = "R";
    static final String ip = "127.0.0.1";
    static int omsPort = 22346;
    static int ksPort = 30001;

    public HanksTodoMain() {
    }

    public static void main(String[] args) {
        String ListName = "New List 1";
        String Do1_1 = "Do this 1.1";
        String Do1_2 = "Do this 1.2";
        String Do2_1 = "Do that 2.1";
        String Do2_2 = "Do that 2.2";

        if (args.length < 4) {
            System.out.println("Incorrect arguments to the program");
            System.out.println("<host-ip> <host-port> <oms ip> <oms-port>");
            return;
        }

        Registry registry;

        java.util.logging.Logger.getLogger("my.category").setLevel(Level.FINEST);
        try {
            OMSServerImpl.main(new String[] {ip, String.valueOf(omsPort)});
            Thread.sleep(1000);

            for (int i = 1; i < NumOfRegions + 1; i++) {
                for (int j = 0; j < NumOfKernelServersPerRegion; j++) {
                    KernelServerImpl.main(
                            new String[] {
                                ip, String.valueOf(ksPort), ip, String.valueOf(omsPort), "R" + i
                            });
                    ksPort++;
                    Thread.sleep(1000);
                }
            }

            registry = LocateRegistry.getRegistry(args[0], Integer.parseInt(args[1]));
            OMSServer omsserver = (OMSServer) registry.lookup("SapphireOMS");
            System.out.println(omsserver);

            KernelServer nodeServer =
                    new KernelServerImpl(
                            new InetSocketAddress(args[2], Integer.parseInt(args[3])),
                            new InetSocketAddress(args[0], Integer.parseInt(args[1])));

            SapphireObjectSpec spec =
                    SapphireObjectSpec.newBuilder()
                            .setLang(Language.java)
                            .setJavaClassName("sapphire.appexamples.hankstodo.TodoListManager")
                            .create();

            SapphireObjectID sapphireObjId = omsserver.createSapphireObject(spec.toString());
            TodoListManager tlm =
                    (TodoListManager) omsserver.acquireSapphireObjectStub(sapphireObjId);
            System.out.println("Received tlm: " + tlm);

            TodoList td1 = tlm.newTodoList(ListName);

            // Consensus policy needs some time after creating new Sapphire object; otherwise,
            // leader election may fail.
            sleep(10000);
            System.out.println("new to do list for 1");

            for (int i = 0; i < 5; i++) {
                // Add to-do items.
                System.out.println("Adding to do 1.1 at loop " + i);
                td1.addToDo(1, Do1_1 + "<" + i + ">");
                System.out.println("Adding to do 1.2 at loop " + i);
                td1.addToDo(1, Do1_2 + "<" + i + ">");
                System.out.println("Adding to do 2.1 at loop " + i);
                td1.addToDo(2, Do2_1 + "<" + i + ">");
                System.out.println("Adding to do 2.2 at loop " + i);
                td1.addToDo(2, Do2_2 + "<" + i + ">");
            }

            String expectedTdString2 = "", expectedTdString1 = "";
            for (int i = 0; i < 5; i++) {
                expectedTdString2 += String.format("%s<%d> : %s<%d> : ", Do2_1, i, Do2_2, i);

                expectedTdString1 += String.format("%s<%d> : %s<%d> : ", Do1_1, i, Do1_2, i);
            }

            // Retrieve to-do items.
            System.out.println(
                    "Please note expected String may display incorrect values depending on the policy.");

            TodoList getTd1 = tlm.getToDoList(ListName);
            String testTdString2 = getTd1.getToDoString(2);
            System.out.println("Expect testTdString for 2: " + expectedTdString2);
            System.out.println("Actual testTdString for 2: " + testTdString2);

            String testTdString1 = getTd1.getToDoString(1);
            System.out.println("Expect testTdString for 1: " + expectedTdString1);
            System.out.println("Actual testTdString for 1: " + testTdString1);

            tlm.doSomething("Testing completed.");

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
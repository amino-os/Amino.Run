package sapphire.appexamples.hankstodo;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import sapphire.app.DMSpec;
import sapphire.app.Language;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.SapphireObjectID;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.policy.dht.DHTPolicy;

public class hanskTododMain {

    public static void main(String[] args) {

        if (args.length < 4) {
            System.out.println("Incorrect arguments to the program");
            System.out.println("<host-ip> <host-port> <oms ip> <oms-port>");
            return;
        }
        Registry registry;
        try {
            registry = LocateRegistry.getRegistry(args[0],Integer.parseInt(args[1]));
            OMSServer omsserver = (OMSServer) registry.lookup("SapphireOMS");
            System.out.println(omsserver);

            KernelServer nodeServer = new KernelServerImpl(new InetSocketAddress(args[2], Integer.parseInt(args[3])), new InetSocketAddress(args[0], Integer.parseInt(args[1])));

            DHTPolicy.Config config = new DHTPolicy.Config();
            config.setNumOfShards(3);

            SapphireObjectSpec spec = SapphireObjectSpec.newBuilder()
                                            .setLang(Language.java)
                                            .setJavaClassName("sapphire.appexamples.hankstodo.TodoListManager")
                                            .addDMSpec(DMSpec.newBuilder()
                                                    .setName(DHTPolicy.class.getName())
                                                    .addConfig(config)
                                                    .create())
                                            .create();

            SapphireObjectID sapphireObjId = omsserver.createSapphireObject(spec.toString());

            TodoListManager tlm = (TodoListManager)omsserver.acquireSapphireObjectStub(sapphireObjId);
            System.out.println("Received tlm: " + tlm);

            TodoList tl = tlm.newTodoList("Hanks");
            System.out.println("Received tl1: " + tl);
            System.out.println(tl.addToDo("First todo"));
            System.out.println(tl.addToDo("Second todo"));
            System.out.println(tl.addToDo("Third todo"));

            TodoList tl2 = tlm.newTodoList("AAA");
            System.out.println("Received tl2: " + tl2);
            System.out.println(tl2.addToDo("First todo"));
            System.out.println(tl2.addToDo("Second todo"));
            System.out.println(tl2.addToDo("Third todo"));

            TodoList tl3 = tlm.newTodoList("HHH");
            System.out.println("Received tl3: " + tl3);
            System.out.println(tl3.addToDo("First todo"));
            System.out.println(tl3.addToDo("Second todo"));
            System.out.println(tl3.addToDo("Third todo"));

            tlm.deleteTodoList("Hanks");
            tlm.deleteTodoList("AAA");
            tlm.deleteTodoList("HHH");
            omsserver.deleteSapphireObject(sapphireObjId);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            System.out.println("Exception Received : " + e);
            e.printStackTrace();
        }
    }
}

package sapphire.appexamples.helloworld;

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
import sapphire.policy.atleastoncerpc.AtLeastOnceRPCPolicy;

public class HelloWorldMain {
    public static void main(String[] args) {
        String world = "DCAP World";

        if (args.length < 4) {
            System.out.println("Incorrect arguments to the program");
            System.out.println("<host-ip> <host-port> <oms ip> <oms-port> [program-arg]");
            return;
        }

        if (args.length > 4 && args[4] != null && args[4].length()>0) {
            world = args[4];
        }

        try {
            Registry registry = LocateRegistry.getRegistry(args[0], Integer.parseInt(args[1]));
            OMSServer server = (OMSServer) registry.lookup("SapphireOMS");

            KernelServer nodeServer = new KernelServerImpl(new InetSocketAddress(args[2], Integer.parseInt(args[3])), new InetSocketAddress(args[0], Integer.parseInt(args[1])));

            SapphireObjectSpec spec = SapphireObjectSpec.newBuilder()
                    .setLang(Language.java)
                    .setJavaClassName("sapphire.appexamples.helloworld.HelloWorld")
                    .addDMSpec(DMSpec.newBuilder()
                            .setName(AtLeastOnceRPCPolicy.class.getName())
                            .create())
                    .create();

            SapphireObjectID sapphireObjId =
                    server.createSapphireObject(spec.toString(), world);
            HelloWorld helloWorld = (HelloWorld) server.acquireSapphireObjectStub(sapphireObjId);
            System.out.println(helloWorld.sayHello());

            server.deleteSapphireObject(sapphireObjId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

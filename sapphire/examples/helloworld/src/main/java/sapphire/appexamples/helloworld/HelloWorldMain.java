package sapphire.appexamples.helloworld;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import sapphire.common.SapphireObjectID;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

public class HelloWorldMain {
    public static void main(String[] args) {
        String world = "Hello DCAP";
        int cnt = 1;

        if (args.length < 5) {
            System.out.println("Incorrect arguments to the program");
            showUsage();
            return;
        }

        world = args[4];
        if (args.length > 5 && args[5] != null && args[5].length()>0) {
            try {
                cnt = Integer.valueOf(args[5]);
            } catch (NumberFormatException e) {
                System.out.println(String.format("Invalid RepeatTime '%s'. RepeatTime must be a number.", args[5]));
                showUsage();
                return;
            }
        }

        try {
            Registry registry = LocateRegistry.getRegistry(args[0], Integer.parseInt(args[1]));
            OMSServer server = (OMSServer) registry.lookup("SapphireOMS");

            new KernelServerImpl(new InetSocketAddress(args[2], Integer.parseInt(args[3])), new InetSocketAddress(args[0], Integer.parseInt(args[1])));

            SapphireObjectID sapphireObjId =
                    server.createSapphireObject("sapphire.appexamples.helloworld.HelloWorld", world);
            HelloWorld helloWorld = (HelloWorld) server.acquireSapphireObjectStub(sapphireObjId);

            for (int i=0; i<cnt; i++) {
                System.out.println(helloWorld.sayHello());
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void showUsage() {
        System.out.println("Usage:");
        System.out.println("<host-ip> <host-port> <oms ip> <oms-port> <Message> [RepeatTime]");
        System.out.println("<Message>: the string you plan to send to HelloWorld");
        System.out.println("[RepeatTime]: an optional integer specifies the number of time to " +
                "send the message repeatedly. Default value is 1.");
    }
}

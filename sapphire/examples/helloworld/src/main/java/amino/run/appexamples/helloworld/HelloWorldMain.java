package amino.run.appexamples.helloworld;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;

import amino.run.app.DMSpec;
import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.app.Registry;
import amino.run.common.MicroServiceID;
import amino.run.kernel.server.KernelServer;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.policy.atleastoncerpc.AtLeastOnceRPCPolicy;

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
            java.rmi.registry.Registry registry = LocateRegistry.getRegistry(args[0], Integer.parseInt(args[1]));
            Registry server = (Registry) registry.lookup("io.amino.run.oms");

            KernelServer nodeServer = new KernelServerImpl(new InetSocketAddress(args[2], Integer.parseInt(args[3])), new InetSocketAddress(args[0], Integer.parseInt(args[1])));

            MicroServiceSpec spec = MicroServiceSpec.newBuilder()
                    .setLang(Language.java)
                    .setJavaClassName("amino.run.appexamples.helloworld.HelloWorld")
                    .addDMSpec(DMSpec.newBuilder()
                            .setName(AtLeastOnceRPCPolicy.class.getName())
                            .create())
                    .create();

            MicroServiceID microServiceId =
                    server.create(spec.toString(), world);
            HelloWorld helloWorld = (HelloWorld) server.acquireStub(microServiceId);
            System.out.println(helloWorld.sayHello());

            server.delete(microServiceId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

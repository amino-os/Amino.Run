package amino.run.appexamples.helloworld;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.Collections;

import amino.run.app.DMSpec;
import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.app.Registry;
import amino.run.common.ArgumentParser.AppArgumentParser;
import amino.run.common.MicroServiceID;
import amino.run.kernel.server.KernelServer;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.policy.atleastoncerpc.AtLeastOnceRPCPolicy;
import com.google.devtools.common.options.OptionsParser;

public class HelloWorldMain {
    public static void main(String[] args) {
        String world = "World";

        OptionsParser parser = OptionsParser.newOptionsParser(AppArgumentParser.class);

        if (args.length < 8) {
            System.out.println("Incorrect arguments to the program");
            printUsage(parser);
            return;
        }
        try {
            parser.parse(args);
        } catch (Exception e) {
            System.out.println(e.getMessage()+System.lineSeparator()+System.lineSeparator()+
                    "Usage: "
                            + HelloWorldMain.class.getSimpleName()+System.lineSeparator()
                            + parser.describeOptions(
                            Collections.<String, String>emptyMap(),
                            OptionsParser.HelpVerbosity.LONG));
            return;
        }
        AppArgumentParser appArgs = parser.getOptions(AppArgumentParser.class);
        if (appArgs.appArgs != "") {
            world=appArgs.appArgs;
        }

        try {
            java.rmi.registry.Registry registry = LocateRegistry.getRegistry(appArgs.omsIP, appArgs.omsPort);
            Registry server = (Registry) registry.lookup("io.amino.run.oms");

            KernelServer nodeServer = new KernelServerImpl(new InetSocketAddress(appArgs.kernelServerIP, appArgs.kernelServerPort), new InetSocketAddress(appArgs.omsIP, appArgs.omsPort));

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

    private static void printUsage(OptionsParser parser) {
        System.out.println(
                "Usage: java -cp <classpath> "
                        + HelloWorldMain.class.getSimpleName()
                        + System.lineSeparator()
                        + parser.describeOptions(
                        Collections.<String, String>emptyMap(),
                        OptionsParser.HelpVerbosity.LONG));
    }
}

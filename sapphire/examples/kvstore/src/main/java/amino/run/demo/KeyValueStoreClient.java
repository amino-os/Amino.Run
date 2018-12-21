package amino.run.demo;

import amino.run.app.Registry;
import amino.run.common.ArgumentParser.AppArgumentParser;
import amino.run.common.MicroServiceID;
import amino.run.kernel.server.KernelServerImpl;
import com.google.devtools.common.options.OptionsParser;
import amino.run.app.labelselector.Requirement;
import amino.run.app.labelselector.Selector;
import amino.run.common.AppObjectStub;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.rmi.registry.LocateRegistry;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

/**
 * Client class for testing {@link KeyValueStore}
 */
public class KeyValueStoreClient {
    public static void main(String[] args) throws Exception {
        OptionsParser parser = OptionsParser.newOptionsParser(AppArgumentParser.class);
        if ( args.length < 4) {
            System.out.println("Incorrect arguments to the program");
            printUsage(parser);
            return;
        }
        try {
            parser.parse(args);
        } catch (Exception e) {
            System.out.println(e.getMessage()+System.lineSeparator()+System.lineSeparator()+
                    "Usage: "
                            + KeyValueStoreClient.class.getSimpleName()+System.lineSeparator()
                            +  parser.describeOptions(
                            Collections.<String, String>emptyMap(),
                            OptionsParser.HelpVerbosity.LONG));
            return;
        }

        AppArgumentParser appArgs = parser.getOptions(AppArgumentParser.class);

        Registry registry = getRegistry(appArgs.omsIP, appArgs.omsPort);

        MicroServiceID oid = registry.create(getSpec());
        KeyValueStore store = (KeyValueStore)registry.acquireStub(oid);

        for (int i=0; i<30; ++i) {
            String key = "key_" + i;
            String val = "val_" + i;

            System.out.println(String.format("<Client> setting %s = %s", key, val));
            store.set(key, val);
            val = String.valueOf(store.get(key));
            System.out.println(String.format("<Client> got value %s with key %s", val, key));
        }

        // create requirement
        Requirement req = Requirement.newBuilder().key("key1")
                .equal()
                .value("value1")
                .create();
        // create selector
        Selector select = new Selector();
        select.add(req);

        // acquire sapphire objects based on selector
        ArrayList<AppObjectStub> sapphireStubList = server.acquireSapphireObjectStub(select);

        if(sapphireStubList.size() != 1 ){
            throw new Exception("invalid list of stubs");
        }

        for(AppObjectStub stub:sapphireStubList){
            store = (KeyValueStore)stub;
        }

        for (int i=31; i<60; ++i) {
            String key = "key_" + i;
            String val = "val_" + i;

            System.out.println(String.format("<Client> setting %s = %s", key, val));
            store.set(key, val);
            val = String.valueOf(store.get(key));
            System.out.println(String.format("<Client> got value %s with key %s", val, key));
        }
    }

    private static Registry getRegistry(String omsIp, int omsPort) throws Exception {
        new KernelServerImpl(new InetSocketAddress("127.0.0.2", 11111), new InetSocketAddress(omsIp,omsPort));
        java.rmi.registry.Registry rmiRegistry = LocateRegistry.getRegistry(omsIp, omsPort);
        Registry aminoRegistry = (Registry) rmiRegistry.lookup("io.amino.run.oms");
        return aminoRegistry;
    }

    private static String getSpec() throws Exception {
        ClassLoader classLoader = new KeyValueStoreClient().getClass().getClassLoader();
        File file = new File(classLoader.getResource("KeyValueStore.yaml").getFile());
        List<String> lines = Files.readAllLines(file.toPath());
        return String.join("\n", lines);
    }

    private static void printUsage(OptionsParser parser) {
        System.out.println(
                "Usage: java -cp <classpath> "
                        + KeyValueStoreClient.class.getSimpleName()
                        + System.lineSeparator()
                        + parser.describeOptions(
                        Collections.<String, String>emptyMap(),
                        OptionsParser.HelpVerbosity.LONG));
    }

}

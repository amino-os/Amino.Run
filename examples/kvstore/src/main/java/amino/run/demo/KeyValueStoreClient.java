package amino.run.demo;

import amino.run.app.*;
import amino.run.common.ArgumentParser.AppArgumentParser;
import amino.run.common.MicroServiceID;
import amino.run.kernel.server.KernelServerImpl;
import com.google.devtools.common.options.OptionsParser;
import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.rmi.registry.LocateRegistry;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Client class for testing {@link KeyValueStore} */
public class KeyValueStoreClient {
    private static final String APP_NAME = "KVStoreApp";
    public static void main(String[] args) throws Exception {
        OptionsParser parser = OptionsParser.newOptionsParser(AppArgumentParser.class);
        if (args.length < 4) {
            System.out.println("Incorrect arguments to the program");
            printUsage(parser);
            return;
        }
        try {
            parser.parse(args);
        } catch (Exception e) {
            System.out.println(
                    e.getMessage()
                            + System.lineSeparator()
                            + System.lineSeparator()
                            + "Usage: "
                            + KeyValueStoreClient.class.getSimpleName()
                            + System.lineSeparator()
                            + parser.describeOptions(
                                    Collections.<String, String>emptyMap(),
                                    OptionsParser.HelpVerbosity.LONG));
            return;
        }

        AppArgumentParser appArgs = parser.getOptions(AppArgumentParser.class);

        Registry registry = getRegistry(args, appArgs.omsIP, appArgs.omsPort);
        MicroServiceID oid = null;
        KeyValueStore store = null;
        try {
            if (!args[0].equals("withGetName")) {
                MicroServiceSpec spec = MicroServiceSpec.fromYaml(getSpec());
                NodeSelectorSpec nodeSpec =
                        getNodeSelectorSpec("KernelServerType", Operator.NotIn, "AppClient");
                spec.setNodeSelectorSpec(nodeSpec);
                oid = registry.create(spec.toString());
                registry.setName(oid, APP_NAME);
                store = (KeyValueStore) registry.acquireStub(oid);
            } else {
                store = (KeyValueStore) registry.attachTo(APP_NAME);
            }
            for (int i = 0; i < 3000; ++i) {
                String key = "key_" + i;
                String val = "val_" + i;

                System.out.println(String.format("<Client> setting %s = %s", key, val));
                store.set(key, val);
                val = String.valueOf(store.get(key));
                System.out.println(String.format("<Client> got value %s with key %s", val, key));
            }

            // TODO: Currently for collecting metric on logging server sleep is introduced.
            //  When metric server will be available new application will get added to demonstrate metric collection
            Thread.sleep(200000);
        } finally {
            if (oid != null) {
                // TODO currently removing micro service delete for Migration testing
                registry.delete(oid);
            }
        }
    }

    private static Registry getRegistry(String[] args, String omsIp, int omsPort) throws Exception {
        KernelServerImpl.main(args);
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

    private static NodeSelectorTerm getNodeSelectorTerm(String key, Operator operator, String... labels) {
        NodeSelectorTerm term = new NodeSelectorTerm();
        term.addMatchRequirements(getRequirement(key, operator, labels));
        return term;
    }

    private static Requirement getRequirement(String key, Operator operator, String... labels) {
        if (Operator.Exists.equals(operator)) {
            return new Requirement(key, operator, null);
        }
        return new Requirement(key, operator, Arrays.asList(labels));
    }

    private static NodeSelectorSpec getNodeSelectorSpec(String key, Operator operator, String... labels) {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        spec.addNodeSelectorTerms(getNodeSelectorTerm(key, operator, labels));
        return spec;
    }
}

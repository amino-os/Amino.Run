package amino.run.demo;

import amino.run.app.Registry;
import amino.run.common.MicroServiceID;
import amino.run.kernel.server.KernelServerImpl;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.rmi.registry.LocateRegistry;
import java.util.List;

/**
 * Client class for testing {@link KeyValueStore}
 */
public class KeyValueStoreClient {
    public static void main(String[] args) throws Exception {
        Registry server = getSapphireObjectServer(args[0], args[1]);

        MicroServiceID oid = server.create(getSpec());
        KeyValueStore store = (KeyValueStore)server.acquireStub(oid);

        for (int i=0; i<30; ++i) {
            String key = "key_" + i;
            String val = "val_" + i;

            System.out.println(String.format("<Client> setting %s = %s", key, val));
            store.set(key, val);
            val = String.valueOf(store.get(key));
            System.out.println(String.format("<Client> got value %s with key %s", val, key));
        }
    }

    private static Registry getSapphireObjectServer(String omsIp, String omsPort) throws Exception {
        new KernelServerImpl(new InetSocketAddress("127.0.0.2", 11111), new InetSocketAddress(omsIp, Integer.parseInt(omsPort)));
        java.rmi.registry.Registry registry = LocateRegistry.getRegistry(omsIp, Integer.parseInt(omsPort));
        Registry server = (Registry) registry.lookup("io.amino.run.oms");
        return server;
    }

    private static String getSpec() throws Exception {
        ClassLoader classLoader = new KeyValueStoreClient().getClass().getClassLoader();
        File file = new File(classLoader.getResource("KeyValueStore.yaml").getFile());
        List<String> lines = Files.readAllLines(file.toPath());
        return String.join("\n", lines);
    }
}

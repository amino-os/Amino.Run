package sapphire.demo;

import sapphire.common.SapphireObjectID;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

/**
 * Client class for testing {@link KeyValueStore}
 */
public class KeyValueStoreClient {
    public static void main(String[] args) throws Exception {
        OMSServer oms = getOMS(args[0], args[1]);

        SapphireObjectID oid = oms.createSapphireObject(getSpec());
        KeyValueStore store = (KeyValueStore)oms.acquireSapphireObjectStub(oid);

        for (int i=0; i<30; ++i) {
            String key = "key_" + i;
            String val = "val_" + i;

            System.out.println(String.format("<Client> setting %s = %s", key, val));
            store.set(key, val);
            val = String.valueOf(store.get(key));
            System.out.println(String.format("<Client> got value %s with key %s", val, key));
        }
    }

    private static OMSServer getOMS(String omsIp, String omsPort) throws Exception {
        new KernelServerImpl(new InetSocketAddress("127.0.0.2", 11111), new InetSocketAddress(omsIp, Integer.parseInt(omsPort)));
        Registry registry = LocateRegistry.getRegistry(omsIp, Integer.parseInt(omsPort));
        OMSServer omsserver = (OMSServer) registry.lookup("SapphireOMS");
        return omsserver;
    }

    private static String getSpec() throws Exception {
        ClassLoader classLoader = new KeyValueStoreClient().getClass().getClassLoader();
        File file = new File(classLoader.getResource("KeyValueStore.yaml").getFile());
        List<String> lines = Files.readAllLines(file.toPath());
        return String.join("\n", lines);
    }
}

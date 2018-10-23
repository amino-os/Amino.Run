package sapphire.demo.stubs;

import sapphire.app.Language;

import java.net.InetSocketAddress;

public class Client_Stub {
    public Client_Stub(){}

    public sapphire.demo.stubs.JSKeyValueStore_Stub GetKeyValueStore(
            String hostIp,
            String hostPort,
            String omsIp,
            String omsPort,
            String langStr,
            String fileName) {

        try {
            InetSocketAddress host = new InetSocketAddress(hostIp, Integer.parseInt(hostPort));
            InetSocketAddress omsHost = new InetSocketAddress(omsIp, Integer.parseInt(omsPort));
            sapphire.app.Language lang = Language.valueOf(langStr);

            // Create OMSClient for oms server interaction
            sapphire.app.OMSClient omsClient = new sapphire.app.OMSClient(host, omsHost);

            sapphire.policy.dht.DHTPolicy.Config config = new sapphire.policy.dht.DHTPolicy.Config();
            config.setNumOfShards(3);

            sapphire.app.SapphireObjectSpec spec = sapphire.app.SapphireObjectSpec.newBuilder()
                    .setLang(lang)
                    .setConstructorName("JSKeyValueStore")
                    .setJavaClassName("sapphire.demo.stubs.JSKeyValueStore_Stub")
                    .setSourceFileLocation((new java.io.File(fileName).toString()))
                    .addDMSpec(sapphire.app.DMSpec.newBuilder()
                            .setName(sapphire.policy.dht.DHTPolicy.class.getName()) // DHT policy
                            .addConfig(config)
                            .create())
                    .addDMSpec(sapphire.app.DMSpec.newBuilder()
                            .setName(sapphire.policy.scalability.LoadBalancedMasterSlaveSyncPolicy.class.getName()) // Master-Slave policy
                            .create())
                    .create();

            sapphire.common.SapphireObjectID sapphireObjId = omsClient.createSapphireObject(spec);
            sapphire.demo.stubs.JSKeyValueStore_Stub kvs =
                    (sapphire.demo.stubs.JSKeyValueStore_Stub) omsClient.acquireSapphireObjectStub(sapphireObjId);
            kvs.$__initializeGraal(spec);
            return kvs;
        } catch (java.lang.Exception e) {
            throw new java.lang.RuntimeException(e);
        }
    }

    public static void main(String... args){
        if (args.length < 4) {
            System.out.println("Incorrect arguments to the kernel server");
            System.out.println("[host ip] [host port] [oms ip] [oms port]");
            return;
        }

        String hostIp = args[0];
        String hostPort = args[1];
        String omsIp = args[2];
        String omsPort = args[3];

        java.lang.String jsHome = java.lang.System.getProperty("JS_HOME");
        if (jsHome == null || jsHome.isEmpty()) jsHome = "./examples/kvstore/src/main/js/sapphire/demo";
        Test(hostIp, hostPort, omsIp, omsPort, sapphire.app.Language.js, jsHome + "/KeyValueStore.js");
    }

    private static void Test(String hostIp, String hostPort, String omsIp, String omsPort, sapphire.app.Language lang, String fileName) {
        Client_Stub client = new Client_Stub();
        sapphire.demo.stubs.JSKeyValueStore_Stub kvs =
                client.GetKeyValueStore(hostIp, hostPort, omsIp, omsPort, lang.name(), fileName);

        for (int i=0; i<30; ++i) {
            String key = "key_" + i;
            String val = "val_" + i;

            System.out.println(String.format("<client>: setting %s = %s", key, val));
            kvs.set(key, val);
            val = String.valueOf(kvs.get(key));
            System.out.println(String.format("<client>: got value %s with key %s", val, key));
        }
    }
}

package amino.run.multidm;

import static amino.run.kernel.IntegrationTestBase.*;

import amino.run.app.MicroServiceSpec;
import amino.run.app.Registry;
import amino.run.common.MicroServiceID;
import amino.run.demo.KVStore;
import amino.run.kernel.server.KernelServerImpl;
import java.io.File;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import org.junit.*;

/**
 * Test <strong>multi-dm</strong> deployment managers, DHT & Consensus , DHT & MasterSlave,
 * AtLeastOnceRPC & DHT & Consensus , AtLeastOnceRPC & DHT & MasterSlave with multiple kernel
 * servers are covered here.
 *
 * <p>Every test in this class tests microservice specifications in <code>
 * src/integrationTest/resources/specs/multi-dm</code> directory.
 */
public class MultiDMTestCases {
    Registry registry;
    private static String regionName = "";

    @BeforeClass
    public static void bootstrap() throws Exception {
        String labels = KernelServerImpl.REGION_KEY + "=" + regionName;
        startOmsAndKernelServers(labels);
    }

    @Before
    public void setUp() throws Exception {
        java.rmi.registry.Registry registry = LocateRegistry.getRegistry(omsIp, omsPort);
        this.registry = (Registry) registry.lookup("io.amino.run.oms");
        new KernelServerImpl(
                new InetSocketAddress(hostIp, hostPort), new InetSocketAddress(omsIp, omsPort));
    }

    private void runTest(MicroServiceSpec spec, boolean consensus) throws Exception {
        MicroServiceID microServiceId = registry.create(spec.toString());
        KVStore store = (KVStore) registry.acquireStub(microServiceId);
        // consensus DM needs some time to elect the leader other wise function call will fail
        if (consensus) {
            Thread.sleep(5000);
        }
        for (int i = 0; i < 10; i++) {
            String key = "k1_" + i;
            String value = "v1_" + i;
            store.set(key, value);
            waitForValue(store, key, value, -1);
            Assert.assertEquals(value, store.get(key));
        }
    }

    @Test
    public void test_DHT_Consensus() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHT_Consensus.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @Test
    public void test_DHT_Consensus_AtleastRPC() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHT_Consensus_AtleastRPC.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @Test
    public void test_DHT_Consensus_AtleastRPC_CacheLease() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHT_Consensus_AtleastRPC_CacheLease.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @Test
    public void test_DHT_Consensus_CacheLease() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHT_Consensus_CacheLease.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @Test
    public void test_DHT_Consensus_CacheLease_AtleastRPC() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHT_Consensus_CacheLease_AtleastRPC.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @Test
    public void test_DHT_MasterSlave() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHT_MasterSlave.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    public void test_DHT_MasterSlave_AtleastRPC() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHT_MasterSlave_AtleastRPC.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    public void test_DHT_MasterSlave_AtleastRPC_CacheLease() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHT_MasterSlave_AtleastRPC_CacheLease.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    public void test_DHT_MasterSlave_CacheLease() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHT_MasterSlave_CacheLease.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    @Ignore("Test is ignored will be removed once the multi DM issues are resolved")
    public void test_AtleastRPC_DHT_MasterSlave() throws Exception {
        File file = getResourceFile("specs/multi-dm/AtleastRPC_DHT_MasterSlave.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    @Ignore("Test is ignored will be removed once the multi DM issues are resolved")
    public void test_AtleastRPC_DHT_Consensus() throws Exception {
        File file = getResourceFile("specs/multi-dm/AtleastRPC_DHT_Consensus.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @Test
    public void test_AtleastRPC_CacheLease() throws Exception {
        File file = getResourceFile("specs/multi-dm/AtleastRPC_CacheLease.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    public void test_AtleastRPC_CacheLease_Consensus() throws Exception {
        File file = getResourceFile("specs/multi-dm/AtleastRPC_CacheLease_Consensus.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @Test
    public void test_AtleastRPC_CacheLease_DHT_Consensus() throws Exception {
        File file = getResourceFile("specs/multi-dm/AtleastRPC_CacheLease_DHT_Consensus.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @Test
    public void test_AtleastRPC_CacheLease_MasterSlave() throws Exception {
        File file = getResourceFile("specs/multi-dm/AtleastRPC_CacheLease_MasterSlave.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    public void test_CacheLease_AtleastRPC() throws Exception {
        File file = getResourceFile("specs/multi-dm/CacheLease_AtleastRPC.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    public void test_CacheLease_AtleastRPC_DHT_Consensus() throws Exception {
        File file = getResourceFile("specs/multi-dm/CacheLease_AtleastRPC_DHT_Consensus.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @Test
    public void test_CacheLease_DHT() throws Exception {
        File file = getResourceFile("specs/multi-dm/CacheLease_DHT.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    public void test_CacheLease_DHT_MasterSlave() throws Exception {
        File file = getResourceFile("specs/multi-dm/CacheLease_DHT_MasterSlave.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    public void test_CacheLease_DHT_Consensus() throws Exception {
        File file = getResourceFile("specs/multi-dm/CacheLease_DHT_Consensus.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

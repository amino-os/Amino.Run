package amino.run.multidm;

import static amino.run.kernel.IntegrationTestBase.*;

import amino.run.app.MicroServiceSpec;
import amino.run.app.Registry;
import amino.run.common.SapphireObjectID;
import amino.run.demo.KVStore;
import amino.run.kernel.server.KernelServerImpl;
import java.io.File;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test <strong>multi-dm</strong> deployment managers, DHT & Consensus , DHT & MasterSlave,
 * AtLeastOnceRPC & DHT & Consensus , AtLeastOnceRPC & DHT & MasterSlave with multiple kernel
 * servers are covered here.
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
        this.registry = (Registry) registry.lookup("SapphireOMS");
        new KernelServerImpl(
                new InetSocketAddress(hostIp, hostPort), new InetSocketAddress(omsIp, omsPort));
    }

    private void runTest(MicroServiceSpec spec, boolean consensus) throws Exception {
        SapphireObjectID sapphireObjId = registry.create(spec.toString());
        KVStore store = (KVStore) registry.acquireStub(sapphireObjId);
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

    /**
     * Test sapphire object specifications in <code>src/integrationTest/resources/specs/multi-dm
     * </code> directory.
     *
     * @throws Exception
     */
    @Test
    public void testDHTNConsensusMultiDM() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHTNConsensus.yaml");
        MicroServiceSpec spec = readSapphireSpec(file);
        runTest(spec, true);
    }

    /**
     * Test sapphire object specifications in <code>src/integrationTest/resources/specs/multi-dm
     * </code> directory.
     *
     * @throws Exception
     */
    @Test
    public void testDHTNMasterSlaveMultiDM() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHTNMasterSlave.yaml");
        MicroServiceSpec spec = readSapphireSpec(file);
        runTest(spec, false);
    }

    /**
     * Test sapphire object specifications in <code>src/integrationTest/resources/specs/multi-dm
     * </code> directory.
     *
     * @throws Exception
     */
    @Test
    @Ignore("Test is ignored will be removed once the multi DM issues are resolved")
    public void testAtleastOnceRPCDHTNMasterSlaveMultiDM() throws Exception {
        File file = getResourceFile("specs/multi-dm/AtleastRPCDHTNMasterSlave.yaml");
        MicroServiceSpec spec = readSapphireSpec(file);
        runTest(spec, false);
    }

    /**
     * Test sapphire object specifications in <code>src/integrationTest/resources/specs/multi-dm
     * </code> directory.
     *
     * @throws Exception
     */
    @Test
    @Ignore("Test is ignored will be removed once the multi DM issues are resolved")
    public void testAtleastOnceRPCDHTNConsensusMultiDM() throws Exception {
        File file = getResourceFile("specs/multi-dm/AtleastRPCDHTNConsensus.yaml");
        MicroServiceSpec spec = readSapphireSpec(file);
        runTest(spec, true);
    }

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

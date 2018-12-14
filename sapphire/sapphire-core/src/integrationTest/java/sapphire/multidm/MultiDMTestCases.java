package sapphire.multidm;

import static sapphire.kernel.IntegrationTestBase.*;

import java.io.File;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import sapphire.app.SapphireObjectServer;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.SapphireObjectID;
import sapphire.demo.KVStore;
import sapphire.kernel.server.KernelServerImpl;

/**
 * Test <strong>multi-dm</strong> deployment managers, DHT & Consensus , DHT & MasterSlave,
 * AtLeastOnceRPC & DHT & Consensus , AtLeastOnceRPC & DHT & MasterSlave with multiple kernel
 * servers are covered here.
 */
public class MultiDMTestCases {
    SapphireObjectServer sapphireObjectServer;
    private static String regionName = "";

    @BeforeClass
    public static void bootstrap() throws Exception {
        String labels = KernelServerImpl.REGION_KEY + "=" + regionName;
        startOmsAndKernelServers(labels);
    }

    @Before
    public void setUp() throws Exception {
        Registry registry = LocateRegistry.getRegistry(omsIp, omsPort);
        sapphireObjectServer = (SapphireObjectServer) registry.lookup("SapphireOMS");
        new KernelServerImpl(
                new InetSocketAddress(hostIp, hostPort), new InetSocketAddress(omsIp, omsPort));
    }

    private void runTest(SapphireObjectSpec spec, boolean consensus) throws Exception {
        SapphireObjectID sapphireObjId = sapphireObjectServer.createSapphireObject(spec.toString());
        KVStore store = (KVStore) sapphireObjectServer.acquireSapphireObjectStub(sapphireObjId);
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
        SapphireObjectSpec spec = readSapphireSpec(file);
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
        SapphireObjectSpec spec = readSapphireSpec(file);
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
        SapphireObjectSpec spec = readSapphireSpec(file);
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
        SapphireObjectSpec spec = readSapphireSpec(file);
        runTest(spec, true);
    }

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

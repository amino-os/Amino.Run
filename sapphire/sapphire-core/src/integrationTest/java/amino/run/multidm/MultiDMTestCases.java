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
import java.util.logging.Logger;
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
 *
 * <p>Every test in this class tests microservice specifications in <code>
 * src/integrationTest/resources/specs/multi-dm</code> directory.
 */
public class MultiDMTestCases {
    final String MULTI_DM_PATH = "specs/multi-dm/";
    Registry registry;
    private static String regionName = "";
    static Logger logger = java.util.logging.Logger.getLogger(MultiDMTestCases.class.getName());

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

    private void runTest(String testName) throws Exception {
        File file = getResourceFile(MULTI_DM_PATH + testName + ".yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        MicroServiceID microServiceId = registry.create(spec.toString());
        KVStore store = (KVStore) registry.acquireStub(microServiceId);
        // consensus DM needs some time to elect the leader other wise function call will fail
        if (testName.contains("Consensus")) {
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
    public void testDHTConsensus() throws Exception {
        runTest("DHTConsensus");
    }

    @Test
    public void testDHTConsensusAtleastRPC() throws Exception {
        runTest("DHTConsensusAtleastRPC");
    }

    @Test
    public void testDHTConsensusAtleastRPCCacheLease() throws Exception {
        runTest("DHTConsensusAtleastRPCCacheLease");
    }

    @Test
    public void testDHTConsensusCacheLease() throws Exception {
        runTest("DHTConsensusCacheLease");
    }

    @Test
    public void testDHTConsensusCacheLeaseAtleastRPC() throws Exception {
        runTest("DHTConsensusCacheLeaseAtleastRPC");
    }

    @Test
    public void testDHTMasterSlave() throws Exception {
        runTest("DHTMasterSlave");
    }

    @Test
    public void testDHTMasterSlaveAtleastRPC() throws Exception {
        runTest("DHTMasterSlaveAtleastRPC");
    }

    @Test
    public void testDHTMasterSlaveAtleastRPCCacheLease() throws Exception {
        runTest("DHTMasterSlaveAtleastRPCCacheLease");
    }

    @Test
    public void testDHTMasterSlaveCacheLease() throws Exception {
        runTest("DHTMasterSlaveCacheLease");
    }

    @Test
    @Ignore("Test is ignored will be removed once the multi DM issues are resolved")
    public void testAtleastRPCDHTMasterSlave() throws Exception {
        runTest("AtleastRPCDHTMasterSlave");
    }

    @Test
    @Ignore("Test is ignored will be removed once the multi DM issues are resolved")
    public void testAtleastRPCDHTConsensus() throws Exception {
        runTest("AtleastRPCDHTConsensus");
    }

    @Test
    public void testAtleastRPCCacheLease() throws Exception {
        runTest("AtleastRPCCacheLease");
    }

    @Test
    public void testAtleastRPCCacheLeaseConsensus() throws Exception {
        runTest("AtleastRPCCacheLeaseConsensus");
    }

    @Test
    public void testAtleastRPCCacheLeaseDHTConsensus() throws Exception {
        runTest("AtleastRPCCacheLeaseDHTConsensus");
    }

    @Test
    public void testAtleastRPCCacheLeaseMasterSlave() throws Exception {
        runTest("AtleastRPCCacheLeaseMasterSlave");
    }

    @Test
    public void testCacheLeaseAtleastRPC() throws Exception {
        runTest("CacheLeaseAtleastRPC");
    }

    @Test
    public void testCacheLeaseAtleastRPCDHTConsensus() throws Exception {
        runTest("CacheLeaseAtleastRPCDHTConsensus");
    }

    @Test
    public void testCacheLeaseDHT() throws Exception {
        runTest("CacheLeaseDHT");
    }

    @Test
    public void testCacheLeaseDHTMasterSlave() throws Exception {
        runTest("CacheLeaseDHTMasterSlave");
    }

    @Test
    public void testCacheLeaseDHTConsensus() throws Exception {
        runTest("CacheLeaseDHTConsensus");
    }

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

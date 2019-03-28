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
    public void testDHTConsensus() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHTConsensus.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @Test
    public void testDHTConsensusAtleastRPC() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHTConsensusAtleastRPC.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @Test
    public void testDHTConsensusAtleastRPCCacheLease() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHTConsensusAtleastRPCCacheLease.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @Test
    public void testDHTConsensusCacheLease() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHTConsensusCacheLease.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @Test
    public void testDHTConsensusCacheLeaseAtleastRPC() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHTConsensusCacheLeaseAtleastRPC.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @Test
    public void testDHTMasterSlave() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHTMasterSlave.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    public void testDHTMasterSlaveAtleastRPC() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHTMasterSlaveAtleastRPC.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    public void testDHTMasterSlaveAtleastRPCCacheLease() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHTMasterSlaveAtleastRPCCacheLease.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    public void testDHTMasterSlaveCacheLease() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHTMasterSlaveCacheLease.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    @Ignore("Test is ignored will be removed once the multi DM issues are resolved")
    public void testAtleastRPCDHTMasterSlave() throws Exception {
        File file = getResourceFile("specs/multi-dm/AtleastRPCDHTMasterSlave.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    @Ignore("Test is ignored will be removed once the multi DM issues are resolved")
    public void testAtleastRPCDHTConsensus() throws Exception {
        File file = getResourceFile("specs/multi-dm/AtleastRPCDHTConsensus.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @Test
    public void testAtleastRPCCacheLease() throws Exception {
        File file = getResourceFile("specs/multi-dm/AtleastRPCCacheLease.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    public void testAtleastRPCCacheLeaseConsensus() throws Exception {
        File file = getResourceFile("specs/multi-dm/AtleastRPCCacheLeaseConsensus.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @Test
    public void testAtleastRPCCacheLeaseDHTConsensus() throws Exception {
        File file = getResourceFile("specs/multi-dm/AtleastRPCCacheLeaseDHTConsensus.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @Test
    public void testAtleastRPCCacheLeaseMasterSlave() throws Exception {
        File file = getResourceFile("specs/multi-dm/AtleastRPCCacheLeaseMasterSlave.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    public void testCacheLeaseAtleastRPC() throws Exception {
        File file = getResourceFile("specs/multi-dm/CacheLeaseAtleastRPC.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    public void testCacheLeaseAtleastRPCDHTConsensus() throws Exception {
        File file = getResourceFile("specs/multi-dm/CacheLeaseAtleastRPCDHTConsensus.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @Test
    public void testCacheLeaseDHT() throws Exception {
        File file = getResourceFile("specs/multi-dm/CacheLeaseDHT.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    public void testCacheLeaseDHTMasterSlave() throws Exception {
        File file = getResourceFile("specs/multi-dm/CacheLeaseDHTMasterSlave.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, false);
    }

    @Test
    public void testCacheLeaseDHTConsensus() throws Exception {
        File file = getResourceFile("specs/multi-dm/CacheLeaseDHTConsensus.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec, true);
    }

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

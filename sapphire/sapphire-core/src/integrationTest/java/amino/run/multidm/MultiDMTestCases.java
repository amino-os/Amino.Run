package amino.run.multidm;

import static amino.run.kernel.IntegrationTestBase.*;

import amino.run.app.DMSpec;
import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.app.Registry;
import amino.run.common.MicroServiceID;
import amino.run.demo.KVStore;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.policy.Upcalls;
import amino.run.policy.dht.DHTPolicy;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test <strong>multi-dm</strong> deployment managers, DHT & ConsensusRSM , DHT &
 * LoadBalancedMasterSlaveSync, AtLeastOnceRPC & DHT & ConsensusRSM , AtLeastOnceRPC & DHT &
 * LoadBalancedMasterSlaveSync with multiple kernel servers are covered here.
 *
 * <p>Every test in this class tests microservice specifications in <code>
 * src/integrationTest/resources/specs/multi-dm</code> directory.
 */
public class MultiDMTestCases {
    final String JAVA_CLASS_NAME = "amino.run.demo.KVStore";
    final Language DEFAULT_LANG = Language.java;
    final String POLICY_POSTFIX = "Policy";
    // ConsensusRSM policy needs a delay for start-up.
    final String CONSENSUS = "ConsensusRSM";
    final int DHT_SHARDS = 2;
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

    private void runTest(String... dmNames) throws Exception {
        MicroServiceSpec spec = createMultiDMTestSpec(dmNames);
        MicroServiceID microServiceId = registry.create(spec.toString());
        KVStore store = (KVStore) registry.acquireStub(microServiceId);

        // consensus DM needs some time to elect the leader other wise function call will fail
        if (spec.getName().contains(CONSENSUS)) {
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
     * Creates a spec for multi-DM integration test based on input array of DM names
     *
     * @param names variable arguments of DM names (that needs to be converted to actual DM name)
     * @return created spec based on input DM names
     */
    private MicroServiceSpec createMultiDMTestSpec(String... names) throws Exception {
        String testName = names[0];
        for (int i = 1; i < names.length; i++) {
            testName += Character.toUpperCase(names[i].charAt(0)) + names[i].substring(1);
        }

        MicroServiceSpec spec =
                MicroServiceSpec.newBuilder()
                        .setName(testName)
                        .setJavaClassName(JAVA_CLASS_NAME)
                        .setLang(DEFAULT_LANG)
                        .create();

        for (String shortDMName : names) {
            Upcalls.PolicyConfig config;
            String completeDMName =
                    getPackageName(shortDMName) + "." + shortDMName + POLICY_POSTFIX;
            DMSpec dmSpec = DMSpec.newBuilder().setName(completeDMName).create();
            
            if (shortDMName.equals("DHT")) {
                config = new DHTPolicy.Config();
                ((DHTPolicy.Config) config).setNumOfShards(DHT_SHARDS);
                dmSpec.setConfigs(new ArrayList<Upcalls.PolicyConfig>(Arrays.asList(config)));
            }
            spec.addDMSpec(dmSpec);
        }

        return spec;
    }

    /**
     * Gets the package name for the given short DM name.
     *
     * @param name shortened DM name
     * @return package name for the given short DM name.
     */
    private String getPackageName(String name) throws Exception {
        if (name.equals("DHT")) {
            return "amino.run.policy.dht";
        }
        if (name.equals("ConsensusRSM")) {
            return "amino.run.policy.replication";
        }
        if (name.equals("AtLeastOnceRPC")) {
            return "amino.run.policy.atleastoncerpc";
        }
        if (name.equals("CacheLease")) {
            return "amino.run.policy.cache";
        }
        if (name.equals("LoadBalancedMasterSlaveSync")) {
            return "amino.run.policy.scalability";
        }

        throw new Exception("There is no package name found for " + name);
    }

    @Test
    public void testDHTConsensusRSM() throws Exception {
        runTest("DHT", "ConsensusRSM");
    }

    @Test
    public void testDHTConsensusRSMAtLeastOnceRPC() throws Exception {
        runTest("DHT", "ConsensusRSM", "AtLeastOnceRPC");
    }

    @Test
    public void testDHTConsensusRSMAtLeastOnceRPCCacheLease() throws Exception {
        runTest("DHT", "ConsensusRSM", "AtLeastOnceRPC", "CacheLease");
    }

    @Test
    public void testDHTConsensusRSMCacheLease() throws Exception {
        runTest("DHT", "ConsensusRSM", "CacheLease");
    }

    @Test
    public void testDHTConsensusRSMCacheLeaseAtLeastOnceRPC() throws Exception {
        runTest("DHT", "ConsensusRSM", "CacheLease", "AtLeastOnceRPC");
    }

    @Test
    public void testDHTLoadBalancedMasterSlaveSync() throws Exception {
        runTest("DHT", "LoadBalancedMasterSlaveSync");
    }

    @Test
    public void testDHTLoadBalancedMasterSlaveSyncAtLeastOnceRPC() throws Exception {
        runTest("DHT", "LoadBalancedMasterSlaveSync", "AtLeastOnceRPC");
    }

    @Test
    public void testDHTLoadBalancedMasterSlaveSyncAtLeastOnceRPCCacheLease() throws Exception {
        runTest("DHT", "LoadBalancedMasterSlaveSync", "AtLeastOnceRPC", "CacheLease");
    }

    @Test
    public void testDHTLoadBalancedMasterSlaveSyncCacheLease() throws Exception {
        runTest("DHT", "LoadBalancedMasterSlaveSync", "CacheLease");
    }

    @Test
    @Ignore("Test is ignored will be removed once the multi DM issues are resolved")
    public void testAtLeastOnceRPCDHTLoadBalancedMasterSlaveSync() throws Exception {
        runTest("AtLeastOnceRPC", "DHT", "LoadBalancedMasterSlaveSync");
    }

    @Test
    @Ignore("Test is ignored will be removed once the multi DM issues are resolved")
    public void testAtLeastOnceRPCDHTConsensusRSM() throws Exception {
        runTest("AtLeastOnceRPC", "DHT", "ConsensusRSM");
    }

    @Test
    public void testAtLeastOnceRPCCacheLease() throws Exception {
        runTest("AtLeastOnceRPC", "CacheLease");
    }

    @Test
    public void testAtLeastOnceRPCCacheLeaseConsensusRSM() throws Exception {
        runTest("AtLeastOnceRPC", "CacheLease", "ConsensusRSM");
    }

    @Test
    public void testAtLeastOnceRPCCacheLeaseDHTConsensusRSM() throws Exception {
        runTest("AtLeastOnceRPC", "CacheLease", "DHT", "ConsensusRSM");
    }

    @Test
    public void testAtLeastOnceRPCCacheLeaseLoadBalancedMasterSlaveSync() throws Exception {
        runTest("AtLeastOnceRPC", "CacheLease", "LoadBalancedMasterSlaveSync");
    }

    @Test
    public void testCacheLeaseAtLeastOnceRPC() throws Exception {
        runTest("CacheLease", "AtLeastOnceRPC");
    }

    @Test
    public void testCacheLeaseAtLeastOnceRPCDHTConsensusRSM() throws Exception {
        runTest("CacheLease", "AtLeastOnceRPC", "DHT", "ConsensusRSM");
    }

    @Test
    public void testCacheLeaseDHT() throws Exception {
        runTest("CacheLease", "DHT");
    }

    @Test
    public void testCacheLeaseDHTLoadBalancedMasterSlaveSync() throws Exception {
        runTest("CacheLease", "DHT", "LoadBalancedMasterSlaveSync");
    }

    @Test
    public void testCacheLeaseDHTConsensusRSM() throws Exception {
        runTest("CacheLease", "DHT", "ConsensusRSM");
    }

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

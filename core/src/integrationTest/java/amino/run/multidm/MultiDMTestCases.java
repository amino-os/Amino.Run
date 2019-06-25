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
import amino.run.policy.util.consensus.raft.LeaderException;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test multiple deployment managers (<strong>"multi-dm"</strong>) with multiple kernel servers.

 * <p>How to add an integration test for new combination: Specify the name of DM without 'policy'
 * suffix. i.e., runTest("DHT", "ConsensusRSM", "AtLeastOnceRPC"); If it has a new DM name, update
 * getPackageName() method to include the package name for it.
 */
// TODO: Current integration tests only check the result back to client. Ideally, it should check
// each kernel server to verify whether RPC was made to intended kernel servers.
public class MultiDMTestCases {
    final String JAVA_CLASS_NAME = "amino.run.demo.KVStore";
    final Language DEFAULT_LANG = Language.java;
    final String POLICY_POSTFIX = "Policy";

    // Unless AtLeastOnceRPC policy is used, we need to explicitly retry.
    final String AT_LEAST_ONCE_RPC = "AtLeastOnceRPC";
    final long retryTimeoutMs = 10000L;
    final long retryPeriodMs = 100L;

    final int DHT_SHARDS = 2;
    Registry registry;
    private static String regionName = "";
    private static final Logger logger = Logger.getLogger(MultiDMTestCases.class.getName());

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
        MicroServiceID microServiceId = null;
        try {
            MicroServiceSpec spec = createMultiDMTestSpec(dmNames);
            microServiceId = registry.create(spec.toString());
            KVStore store = (KVStore) registry.acquireStub(microServiceId);

            for (int i = 0; i < 10; i++) {
                String key = "k1_" + i;
                String value = "v1_" + i;
                long startTime = System.currentTimeMillis();
                String returnValue = "";
                while (System.currentTimeMillis() - startTime < retryTimeoutMs) {
                    try {
                        store.set(key, value);
                        returnValue = (String) store.get(key);
                        logger.info("Got value " + returnValue + " for key " + key);
                        break; // Success, no more retries necessary
                    } catch (RuntimeException r) {
                        logger.info(
                                "Runtime exception after "
                                        + (System.currentTimeMillis() - startTime)
                                        + "ms for key "
                                        + key
                                        + ", value "
                                        + value
                                        + " : "
                                        + r.toString());
                        if (r.getCause() != null
                                && r.getCause()
                                        .getClass()
                                        .isAssignableFrom(LeaderException.class)) {
                            logger.info("Cause of runtime exception is LeaderException");
                            if (!Arrays.asList(dmNames).contains(AT_LEAST_ONCE_RPC)) {
                                // Swallow the exception and retry, after sleeping
                                logger.info(
                                        "Swallowing runtime exception because AtLeastOnceRPC is not used.");
                                Thread.sleep(retryPeriodMs);
                            } else {
                                logger.info(
                                        "Not swallowing runtime exception because AtLeastOnceRPC is used.");
                                throw r;
                            }
                        } else {
                            logger.info(
                                    "Not swallowing runtime exception because cause is not LeaderException");
                            throw r;
                        }
                    }
                }
                if (System.currentTimeMillis() - startTime >= retryTimeoutMs) {
                    throw new TimeoutException("Timed out retrying key " + key + ", value " + value);
                }
                Assert.assertEquals(value, returnValue);
            }
        } finally {
            if (microServiceId != null) {
                registry.delete(microServiceId);
            }
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
        /*
         * Please keep this list in alphabetical order.
         */
        if (name.equals("AtLeastOnceRPC")) {
            return "amino.run.policy.atleastoncerpc";
        }
        if (name.equals("CacheLease")) {
            return "amino.run.policy.cache";
        }
        if (name.equals("ConsensusRSM")) {
            return "amino.run.policy.replication";
        }
        if (name.equals("DHT")) {
            return "amino.run.policy.dht";
        }
        if (name.equals("DurableSerializableRPC")) {
            return "amino.run.policy.checkpoint.durableserializable";
        }
        if (name.equals("LoadBalancedMasterSlaveSync") || name.equals("LoadBalancedFrontend")) {
            return "amino.run.policy.scalability";
        }
        if (name.equals("OptConcurrentTransact") || name.equals("LockingTransaction")) {
            return "amino.run.policy.serializability";
        }
        if (name.equals("PeriodicCheckpoint")) {
            return "amino.run.policy.checkpoint.periodiccheckpoint";
        }
        if (name.equals("TwoPCCoordinator")) {
            return "amino.run.policy.transaction";
        }
        if (name.equals("WriteThroughCache")) {
            return "amino.run.policy.cache";
        }

        throw new Exception("There is no package name found for " + name);
    }

    /*
     * Please keep these tests in alphabetical order.
     */

    @Test
    public void testAtLeastOnceRPC() throws Exception {
        runTest("AtLeastOnceRPC");
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
    public void testAtLeastOnceRPCConsensusRSM() throws Exception {
        runTest("AtLeastOnceRPC", "ConsensusRSM");
    }

    @Test
    @Ignore("See DM issue #618. Enable this test once resolved.")
    public void testAtLeastOnceRPCConsensusRSMDHT() throws Exception {
        runTest("AtLeastOnceRPC", "ConsensusRSM", "DHT");
    }

    @Test
    public void testAtLeastOnceRPCDHT() throws Exception {
        runTest("AtLeastOnceRPC", "DHT");
    }

    @Test
    @Ignore("See DM issue #642. Enable this test once resolved.")
    public void testAtLeastOnceRPCDHTConsensusRSM() throws Exception {
        runTest("AtLeastOnceRPC", "DHT", "ConsensusRSM");
    }

    @Test
    public void testAtLeastOnceRPCDurableSerializableRPC() throws Exception {
        runTest("AtLeastOnceRPC", "DurableSerializableRPC");
    }

    @Test
    public void testAtLeastOnceRPCDHTLoadBalancedMasterSlaveSync() throws Exception {
        runTest("AtLeastOnceRPC", "DHT", "LoadBalancedMasterSlaveSync");
    }


    @Test
    public void testAtLeastOnceRPCLockingTransaction() throws Exception {
        runTest("AtLeastOnceRPC", "LockingTransaction");
    }

    @Test
    @Ignore("See DM issue #642. Enable this test once resolved.")
    public void testAtLeastOnceRPCLoadBalancedFrontend() throws Exception {
        runTest("AtLeastOnceRPC", "LoadBalancedFrontend");
    }

    @Test
    public void testAtLeastOnceRPCLoadBalancedMasterSlaveSync() throws Exception {
        runTest("AtLeastOnceRPC", "LoadBalancedMasterSlaveSync");
    }

    @Test
    public void testAtLeastOnceRPCOptConcurrentTransact() throws Exception {
        runTest("AtLeastOnceRPC", "OptConcurrentTransact");
    }

    @Test
    public void testAtLeastOnceRPCPeriodicCheckpoint() throws Exception {
        runTest("AtLeastOnceRPC", "PeriodicCheckpoint");
    }

    @Test
    public void testAtLeastOnceRPCTwoPCCoordinator() throws Exception {
        runTest("AtLeastOnceRPC", "TwoPCCoordinator");
    }

    @Test
    public void testAtLeastOnceRPCWriteThroughCache() throws Exception {
        runTest("AtLeastOnceRPC", "WriteThroughCache");
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

    @Test
    @Ignore("See DM issue #618. Enable this test once resolved.")
    public void testConsensusRSMDHT() throws Exception {
        runTest("ConsensusRSM", "DHT");
    }

    @Test
    @Ignore("See DM issue #618. Enable this test once resolved.")
    public void testConsensusRSMDHTAtLeastOnceRPC() throws Exception {
        runTest("ConsensusRSM", "DHT", "AtLeastOnceRPC");
    }

    @Test
    public void testDHTAtLeastOnceRPCConsensusRSMCacheLease() throws Exception {
        runTest("DHT", "AtLeastOnceRPC", "ConsensusRSM", "CacheLease");
    }

    @Test
    public void testDHTConsensusRSM() throws Exception {
        runTest("DHT", "ConsensusRSM");
    }

    @Test
    @Ignore("See DM issue #618. Enable this test once resolved")
    public void testDHTConsensusRSMAtLeastOnceRPC() throws Exception {
        runTest("DHT", "ConsensusRSM", "AtLeastOnceRPC");
    }

    @Test
    @Ignore("See DM issue #642. Enable this test once resolved.")
    public void testDHTConsensusRSMAtLeastOnceRPCCacheLease() throws Exception {
        runTest("DHT", "ConsensusRSM", "AtLeastOnceRPC", "CacheLease");
    }

    @Test
    public void testDHTConsensusRSMCacheLease() throws Exception {
        runTest("DHT", "ConsensusRSM", "CacheLease");
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
    @Ignore("See DM issue #642. Enable this test once resolved.")
    public void testDHTLoadBalancedMasterSlaveSyncAtLeastOnceRPCCacheLease() throws Exception {
        runTest("DHT", "LoadBalancedMasterSlaveSync", "AtLeastOnceRPC", "CacheLease");
    }

    @Test
    @Ignore("See DM issue #642. Enable this test once resolved.")
    public void testDHTLoadBalancedMasterSlaveSyncCacheLease() throws Exception {
        runTest("DHT", "LoadBalancedMasterSlaveSync", "CacheLease");
    }

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

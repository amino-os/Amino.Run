package amino.run.multidm;

import static amino.run.kernel.IntegrationTestBase.*;

import amino.run.app.DMSpec;
import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.app.Registry;
import amino.run.common.MicroServiceID;
import amino.run.common.Utils;
import amino.run.demo.KVStore;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.policy.Policy;
import amino.run.policy.Upcalls;
import amino.run.policy.atleastoncerpc.AtLeastOnceRPCPolicy;
import amino.run.policy.cache.CacheLeasePolicy;
import amino.run.policy.cache.WriteThroughCachePolicy;
import amino.run.policy.checkpoint.durableserializable.DurableSerializableRPCPolicy;
import amino.run.policy.checkpoint.periodiccheckpoint.PeriodicCheckpointPolicy;
import amino.run.policy.dht.DHTPolicy;
import amino.run.policy.replication.ConsensusRSMPolicy;
import amino.run.policy.scalability.LoadBalancedFrontendPolicy;
import amino.run.policy.scalability.LoadBalancedMasterSlaveSyncPolicy;
import amino.run.policy.serializability.LockingTransactionPolicy;
import amino.run.policy.serializability.OptConcurrentTransactPolicy;
import amino.run.policy.transaction.TwoPCCoordinatorPolicy;
import amino.run.policy.util.consensus.raft.LeaderException;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeoutException;
import java.util.List;
import java.util.logging.Logger;
import java.util.Set;
import java.util.HashSet;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runners.Parameterized;
import org.junit.runner.RunWith;
import org.junit.Test;

/**
 * Test multiple deployment managers (<strong>"multi-dm"</strong>) with multiple kernel servers.
 *
 * <p>How to add an integration test for new combination: Specify the name of DM without 'policy'
 * suffix. i.e., runTest(DHTPolicy.class, ConsensusRSMPolicy.class, AtLeastOnceRPCPolicy.class); If
 * it has a new DM name, update getPackageName() method to include the package name for it.
 */
// TODO: Current integration tests only check the result back to client. Ideally, it should check
// each kernel server to verify whether RPC was made to intended kernel servers.
@RunWith(Parameterized.class)
public class MultiDMTestCases {
    final String JAVA_CLASS_NAME = "amino.run.demo.KVStore";
    final Language DEFAULT_LANG = Language.java;

    // Unless AtLeastOnceRPC policy is used, we need to explicitly retry.
    final long retryTimeoutMs = 10000L;
    final long retryPeriodMs = 100L;

    final int DHT_SHARDS = 2;
    Registry registry;
    private static String regionName = "";
    private static final Logger logger = Logger.getLogger(MultiDMTestCases.class.getName());

    private static Class[] allDmClasses = {
            AtLeastOnceRPCPolicy.class,
            // CacheLeasePolicy.class,
            ConsensusRSMPolicy.class,
            // DHTPolicy.class,
            // LoadBalancedMasterSlaveSyncPolicy.class,
    };

    private static Class[][] ignoredCombinations= {
            { ConsensusRSMPolicy.class, AtLeastOnceRPCPolicy.class }, // TODO: quinton, just testing
            { ConsensusRSMPolicy.class, DHTPolicy.class},
    };


    @Parameterized.Parameter(0)
    public Class dmClasses[] = new Class[0];

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

    // create test data - one row per DM combination
    @Parameterized.Parameters(name = "{index}: Test with dms={0}")
    public static Collection<Object[]> data() {
        Utils.ArrayToStringComparator comparator = new Utils.ArrayToStringComparator();
        ArrayList<Object[]> data = new ArrayList<Object[]>();
        Arrays.sort(ignoredCombinations, comparator);
        // Add all combinations of two DMs
        for(Class first: allDmClasses) {
            for(Class second: allDmClasses) {
                if (!first.equals(second)) { // Don't test DMs in combination with themselves
                    Class[] combo = new Class[] {first, second};
                    if (Arrays.binarySearch(ignoredCombinations, combo, comparator) >= 0) {
                        data.add(new Class[][] { new Class[] { first, second } } );
                    }
                }
            }
        }
        // Explicitly add some extra combinations of more than 2 DMs
        // TODO: quinton: data.add(new Class[]{ AtLeastOnceRPCPolicy.class, CacheLeasePolicy.class, ConsensusRSMPolicy.class });
        // Explicitly remove all ignored combinations.
        List<Class[]> ignored = Arrays.asList(ignoredCombinations);
        data.removeAll(ignored);
        logger.info("Using data: Combinations: " + data.size() + ", DM classes: " + data.toString());
        return data;
    }


    @Test
    public void runTest() throws Exception {
        MicroServiceID microServiceId = null;
        try {
            MicroServiceSpec spec = createMultiDMTestSpec(dmClasses);
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
                            if (!Arrays.asList(dmClasses).contains(AtLeastOnceRPCPolicy.class)) {
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
                    throw new TimeoutException(
                            "Timed out retrying key " + key + ", value " + value);
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
    private MicroServiceSpec createMultiDMTestSpec(Class... dmClasses) throws Exception {
        MicroServiceSpec spec =
                MicroServiceSpec.newBuilder()
                        .setName(JAVA_CLASS_NAME)
                        .setJavaClassName(JAVA_CLASS_NAME)
                        .setLang(DEFAULT_LANG)
                        .create();

        for (Class dmClass : dmClasses) {
            Upcalls.PolicyConfig config;
            DMSpec dmSpec = DMSpec.newBuilder().setName(dmClass.getCanonicalName()).create();

            if (dmClass.equals(DHTPolicy.class)) {
                config = new DHTPolicy.Config();
                ((DHTPolicy.Config) config).setNumOfShards(DHT_SHARDS);
                dmSpec.setConfigs(new ArrayList<Upcalls.PolicyConfig>(Arrays.asList(config)));
            }
            spec.addDMSpec(dmSpec);
        }

        return spec;
    }

    /*
     * Please keep these tests in alphabetical order.
     */

    /* TODO: Quinton: Replace all tests with data
    @Test
    public void testAtLeastOnceRPC() throws Exception {
        runTest(AtLeastOnceRPCPolicy.class);
    }

    @Test
    public void testAtLeastOnceRPCCacheLease() throws Exception {
        runTest(AtLeastOnceRPCPolicy.class, CacheLeasePolicy.class);
    }

    @Test
    public void testAtLeastOnceRPCCacheLeaseConsensusRSM() throws Exception {
        runTest(AtLeastOnceRPCPolicy.class, CacheLeasePolicy.class, ConsensusRSMPolicy.class);
    }

    @Test
    public void testAtLeastOnceRPCCacheLeaseDHTConsensusRSM() throws Exception {
        runTest(
                AtLeastOnceRPCPolicy.class,
                CacheLeasePolicy.class,
                DHTPolicy.class,
                ConsensusRSMPolicy.class);
    }

    @Test
    public void testAtLeastOnceRPCCacheLeaseLoadBalancedMasterSlaveSync() throws Exception {
        runTest(
                AtLeastOnceRPCPolicy.class,
                CacheLeasePolicy.class,
                LoadBalancedMasterSlaveSyncPolicy.class);
    }

    @Test
    public void testAtLeastOnceRPCConsensusRSM() throws Exception {
        runTest(AtLeastOnceRPCPolicy.class, ConsensusRSMPolicy.class);
    }

    @Test
    @Ignore("See DM issue #618. Enable this test once resolved.")
    public void testAtLeastOnceRPCConsensusRSMDHT() throws Exception {
        runTest(AtLeastOnceRPCPolicy.class, ConsensusRSMPolicy.class, DHTPolicy.class);
    }

    @Test
    public void testAtLeastOnceRPCDHT() throws Exception {
        runTest(AtLeastOnceRPCPolicy.class, DHTPolicy.class);
    }

    @Test
    @Ignore("See DM issue #642. Enable this test once resolved.")
    public void testAtLeastOnceRPCDHTConsensusRSM() throws Exception {
        runTest(AtLeastOnceRPCPolicy.class, DHTPolicy.class, ConsensusRSMPolicy.class);
    }

    @Test
    public void testAtLeastOnceRPCDurableSerializableRPC() throws Exception {
        runTest(AtLeastOnceRPCPolicy.class, DurableSerializableRPCPolicy.class);
    }

    @Test
    public void testAtLeastOnceRPCDHTLoadBalancedMasterSlaveSync() throws Exception {
        runTest(
                AtLeastOnceRPCPolicy.class,
                DHTPolicy.class,
                LoadBalancedMasterSlaveSyncPolicy.class);
    }

    @Test
    public void testAtLeastOnceRPCLockingTransaction() throws Exception {
        runTest(AtLeastOnceRPCPolicy.class, LockingTransactionPolicy.class);
    }

    @Test
    @Ignore("See DM issue #642. Enable this test once resolved.")
    public void testAtLeastOnceRPCLoadBalancedFrontend() throws Exception {
        runTest(AtLeastOnceRPCPolicy.class, LoadBalancedFrontendPolicy.class);
    }

    @Test
    public void testAtLeastOnceRPCLoadBalancedMasterSlaveSync() throws Exception {
        runTest(AtLeastOnceRPCPolicy.class, LoadBalancedMasterSlaveSyncPolicy.class);
    }

    @Test
    public void testAtLeastOnceRPCOptConcurrentTransact() throws Exception {
        runTest(AtLeastOnceRPCPolicy.class, OptConcurrentTransactPolicy.class);
    }

    @Test
    public void testAtLeastOnceRPCPeriodicCheckpoint() throws Exception {
        runTest(AtLeastOnceRPCPolicy.class, PeriodicCheckpointPolicy.class);
    }

    @Test
    public void testAtLeastOnceRPCTwoPCCoordinator() throws Exception {
        runTest(AtLeastOnceRPCPolicy.class, TwoPCCoordinatorPolicy.class);
    }

    @Test
    public void testAtLeastOnceRPCWriteThroughCache() throws Exception {
        runTest(AtLeastOnceRPCPolicy.class, WriteThroughCachePolicy.class);
    }

    @Test
    public void testCacheLeaseAtLeastOnceRPC() throws Exception {
        runTest(CacheLeasePolicy.class, AtLeastOnceRPCPolicy.class);
    }

    @Test
    public void testCacheLeaseAtLeastOnceRPCDHTConsensusRSM() throws Exception {
        runTest(
                CacheLeasePolicy.class,
                AtLeastOnceRPCPolicy.class,
                DHTPolicy.class,
                ConsensusRSMPolicy.class);
    }

    @Test
    public void testCacheLeaseDHT() throws Exception {
        runTest(CacheLeasePolicy.class, DHTPolicy.class);
    }

    @Test
    public void testCacheLeaseDHTLoadBalancedMasterSlaveSync() throws Exception {
        runTest(CacheLeasePolicy.class, DHTPolicy.class, LoadBalancedMasterSlaveSyncPolicy.class);
    }

    @Test
    public void testCacheLeaseDHTConsensusRSM() throws Exception {
        runTest(CacheLeasePolicy.class, DHTPolicy.class, ConsensusRSMPolicy.class);
    }

    @Test
    @Ignore("See DM issue #618. Enable this test once resolved.")
    public void testConsensusRSMDHT() throws Exception {
        runTest(ConsensusRSMPolicy.class, DHTPolicy.class);
    }

    @Test
    @Ignore("See DM issue #618. Enable this test once resolved.")
    public void testConsensusRSMDHTAtLeastOnceRPC() throws Exception {
        runTest(ConsensusRSMPolicy.class, DHTPolicy.class, AtLeastOnceRPCPolicy.class);
    }

    @Test
    public void testDHTAtLeastOnceRPCConsensusRSMCacheLease() throws Exception {
        runTest(
                DHTPolicy.class,
                AtLeastOnceRPCPolicy.class,
                ConsensusRSMPolicy.class,
                CacheLeasePolicy.class);
    }

    @Test
    public void testDHTConsensusRSM() throws Exception {
        runTest(DHTPolicy.class, ConsensusRSMPolicy.class);
    }

    @Test
    @Ignore("See DM issue #618. Enable this test once resolved")
    public void testDHTConsensusRSMAtLeastOnceRPC() throws Exception {
        runTest(DHTPolicy.class, ConsensusRSMPolicy.class, AtLeastOnceRPCPolicy.class);
    }

    @Test
    @Ignore("See DM issue #642. Enable this test once resolved.")
    public void testDHTConsensusRSMAtLeastOnceRPCCacheLease() throws Exception {
        runTest(
                DHTPolicy.class,
                ConsensusRSMPolicy.class,
                AtLeastOnceRPCPolicy.class,
                CacheLeasePolicy.class);
    }

    @Test
    public void testDHTConsensusRSMCacheLease() throws Exception {
        runTest(DHTPolicy.class, ConsensusRSMPolicy.class, CacheLeasePolicy.class);
    }

    @Test
    public void testDHTLoadBalancedMasterSlaveSync() throws Exception {
        runTest(DHTPolicy.class, LoadBalancedMasterSlaveSyncPolicy.class);
    }

    @Test
    public void testDHTLoadBalancedMasterSlaveSyncAtLeastOnceRPC() throws Exception {
        runTest(
                DHTPolicy.class,
                LoadBalancedMasterSlaveSyncPolicy.class,
                AtLeastOnceRPCPolicy.class);
    }

    @Test
    @Ignore("See DM issue #642. Enable this test once resolved.")
    public void testDHTLoadBalancedMasterSlaveSyncAtLeastOnceRPCCacheLease() throws Exception {
        runTest(
                DHTPolicy.class,
                LoadBalancedMasterSlaveSyncPolicy.class,
                AtLeastOnceRPCPolicy.class,
                CacheLeasePolicy.class);
    }

    @Test
    @Ignore("See DM issue #642. Enable this test once resolved.")
    public void testDHTLoadBalancedMasterSlaveSyncCacheLease() throws Exception {
        runTest(DHTPolicy.class, LoadBalancedMasterSlaveSyncPolicy.class, CacheLeasePolicy.class);
    }
    */

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

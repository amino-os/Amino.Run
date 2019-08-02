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
import amino.run.policy.serializability.SerializableRPCPolicy;
import amino.run.policy.transaction.TwoPCCoordinatorPolicy;
import amino.run.policy.util.consensus.raft.LeaderException;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test multiple deployment managers (<strong>"multi-dm"</strong>) with multiple kernel servers.
 *
 * <p>TODO: Current integration tests only check the results returned back to the client. Ideally,
 * they should check each kernel server to verify whether RPCs were made to intended kernel servers.
 */
@RunWith(Parameterized.class)
public class MultiDMTestCases {
    final String JAVA_CLASS_NAME = "amino.run.demo.KVStore";
    final Language DEFAULT_LANG = Language.java;

    // Unless AtLeastOnceRPC policy is used as outer DM, we need to explicitly retry.
    final long RETRY_TIMEOUT_MS = 10000L;
    final long RETRY_PERIOD_MS = 1000L;

    final int DHT_SHARDS = 2;
    Registry registry;
    private static String regionName = "";
    private static final Logger logger = Logger.getLogger(MultiDMTestCases.class.getName());

    /**
     * Array of all DM classes to be tested. Every binary combination (i.e. 2) of these DM's will be
     * tested unless explicitly ignored below.
     */
    private static Class[] allDmClasses = {
        AtLeastOnceRPCPolicy.class,
        CacheLeasePolicy.class,
        ConsensusRSMPolicy.class,
        DurableSerializableRPCPolicy.class,
        DHTPolicy.class,
        LoadBalancedMasterSlaveSyncPolicy.class,
        LockingTransactionPolicy.class,
        OptConcurrentTransactPolicy.class,
        PeriodicCheckpointPolicy.class,
        SerializableRPCPolicy.class,
        TwoPCCoordinatorPolicy.class,
        WriteThroughCachePolicy.class
    };

    /**
     * Array of additional combinations of DM's to be tested. Because the number of combinations of
     * 3 or more DM's is so large, we do not automatically test all possible combinations of 3 or
     * more, only those combinations listed here.
     */
    private static Class[][] additionalCombinations = {
        {AtLeastOnceRPCPolicy.class, CacheLeasePolicy.class, ConsensusRSMPolicy.class},
        {
            AtLeastOnceRPCPolicy.class,
            CacheLeasePolicy.class,
            DHTPolicy.class,
            ConsensusRSMPolicy.class
        },
        {
            AtLeastOnceRPCPolicy.class,
            CacheLeasePolicy.class,
            LoadBalancedMasterSlaveSyncPolicy.class
        },
        {AtLeastOnceRPCPolicy.class, ConsensusRSMPolicy.class, DHTPolicy.class},
        {AtLeastOnceRPCPolicy.class, DHTPolicy.class, LoadBalancedMasterSlaveSyncPolicy.class},
        {AtLeastOnceRPCPolicy.class, DHTPolicy.class, ConsensusRSMPolicy.class},
        {
            CacheLeasePolicy.class,
            AtLeastOnceRPCPolicy.class,
            DHTPolicy.class,
            ConsensusRSMPolicy.class
        },
        {CacheLeasePolicy.class, DHTPolicy.class, LoadBalancedMasterSlaveSyncPolicy.class},
        {CacheLeasePolicy.class, DHTPolicy.class, ConsensusRSMPolicy.class},
        {ConsensusRSMPolicy.class, DHTPolicy.class, AtLeastOnceRPCPolicy.class},
        {
            DHTPolicy.class,
            AtLeastOnceRPCPolicy.class,
            ConsensusRSMPolicy.class,
            CacheLeasePolicy.class
        },
        {DHTPolicy.class, ConsensusRSMPolicy.class, CacheLeasePolicy.class},
        {
            DHTPolicy.class,
            ConsensusRSMPolicy.class,
            AtLeastOnceRPCPolicy.class,
            CacheLeasePolicy.class
        },
        {DHTPolicy.class, LoadBalancedMasterSlaveSyncPolicy.class, AtLeastOnceRPCPolicy.class},
        {
            DHTPolicy.class,
            LoadBalancedMasterSlaveSyncPolicy.class,
            AtLeastOnceRPCPolicy.class,
            CacheLeasePolicy.class
        }
    };

    /**
     * Array of all combinations of DM that should not be tested. Typically these are known not to
     * work. When adding to this list, please include the reason why the combination is known not to
     * work, and an issue number to track fixing the problem.
     */
    private static Class[][] ignoredCombinations = {
        /* See DM issue #618. Enable this test once resolved. */
        {AtLeastOnceRPCPolicy.class, ConsensusRSMPolicy.class, DHTPolicy.class},
        /* See DM issue #642. Enable this test once resolved. */
        {AtLeastOnceRPCPolicy.class, DHTPolicy.class, ConsensusRSMPolicy.class},
        /* See DM issue #642. Enable this test once resolved. */
        {AtLeastOnceRPCPolicy.class, LoadBalancedFrontendPolicy.class},
        /* TimeoutException at MultiDMTestCases.java:283 Caused by: consensus.raft.LeaderException */
        {ConsensusRSMPolicy.class, AtLeastOnceRPCPolicy.class},
        /* See DM issue #618. Enable this test once resolved. */
        {ConsensusRSMPolicy.class, DHTPolicy.class},
        /* See DM issue #618. Enable this test once resolved. */
        {ConsensusRSMPolicy.class, DHTPolicy.class, AtLeastOnceRPCPolicy.class},
        /* TimeoutException: Timed out retrying key k1_0, value v1_0 at runTest(MultiDMTestCases.java:278) */
        {ConsensusRSMPolicy.class, DurableSerializableRPCPolicy.class},
        /* LeaderException: Current Leader is 00000000-0000-0000-0000-000000000000 */
        {ConsensusRSMPolicy.class, LoadBalancedMasterSlaveSyncPolicy.class},
        /* TimeoutException: Timed out retrying key k1_0, value v1_0 at runTest(MultiDMTestCases.java:278) */
        {ConsensusRSMPolicy.class, PeriodicCheckpointPolicy.class},
        /* TransactionAbortException: Distributed transaction has been rolled back. execution had error. */
        {ConsensusRSMPolicy.class, TwoPCCoordinatorPolicy.class},
        /* TimeoutException: Timed out retrying key k1_0, value v1_0 at MultiDMTestCases.runTest(MultiDMTestCases.java:278) */
        {
            DHTPolicy.class,
            AtLeastOnceRPCPolicy.class,
            ConsensusRSMPolicy.class,
            CacheLeasePolicy.class
        },
        /* See DM issue #618. Enable this test once resolved */
        {DHTPolicy.class, ConsensusRSMPolicy.class, AtLeastOnceRPCPolicy.class},
        /* See DM issue #642. Enable this test once resolved. */
        {
            DHTPolicy.class,
            ConsensusRSMPolicy.class,
            AtLeastOnceRPCPolicy.class,
            CacheLeasePolicy.class
        },
        /* See DM issue #642. Enable this test once resolved. */
        {
            DHTPolicy.class,
            LoadBalancedMasterSlaveSyncPolicy.class,
            AtLeastOnceRPCPolicy.class,
            CacheLeasePolicy.class
        },
        /* LeaseNotAvailableException: Could not get lease */
        {LoadBalancedMasterSlaveSyncPolicy.class, CacheLeasePolicy.class},
        /* java.lang.NullPointerException */
        {LoadBalancedMasterSlaveSyncPolicy.class, ConsensusRSMPolicy.class},
        /* AssertionError at amino.run.policy.Library$ClientPolicyLibrary.extractAppContext(Library.java:87) */
        {LoadBalancedMasterSlaveSyncPolicy.class, DHTPolicy.class},
        /* AssertionError at amino.run.policy.Library$ClientPolicyLibrary.extractAppContext(Library.java:87) */
        {LoadBalancedMasterSlaveSyncPolicy.class, LockingTransactionPolicy.class},
        /* AssertionError at amino.run.policy.Library$ClientPolicyLibrary.extractAppContext(Library.java:87) */
        {LoadBalancedMasterSlaveSyncPolicy.class, OptConcurrentTransactPolicy.class},
        /* AssertionError at amino.run.policy.Library$ClientPolicyLibrary.extractAppContext(Library.java:87) */
        {LoadBalancedMasterSlaveSyncPolicy.class, WriteThroughCachePolicy.class},
        /* MicroServiceCreationException casused by KernelObjectMigratingException */
        {OptConcurrentTransactPolicy.class, ConsensusRSMPolicy.class},
        /* MicroServiceCreationException casused by KernelObjectMigratingException */
        {OptConcurrentTransactPolicy.class, DHTPolicy.class},
        /* MicroServiceCreationException casused by KernelObjectMigratingException */
        {OptConcurrentTransactPolicy.class, LoadBalancedMasterSlaveSyncPolicy.class},
        /* java.lang.NullPointerException */
        {TwoPCCoordinatorPolicy.class, ConsensusRSMPolicy.class},
        /* java.lang.NullPointerException */
        {TwoPCCoordinatorPolicy.class, DHTPolicy.class},
        /* TransactionAlreadyStartedException: nested transaction is unsupported. */
        {TwoPCCoordinatorPolicy.class, DurableSerializableRPCPolicy.class},
        /* TransactionAlreadyStartedException: nested transaction is unsupported. */
        {TwoPCCoordinatorPolicy.class, PeriodicCheckpointPolicy.class},
        /* TransactionAlreadyStartedException: nested transaction is unsupported. */
        {TwoPCCoordinatorPolicy.class, LockingTransactionPolicy.class},
        /* TransactionAlreadyStartedException: nested transaction is unsupported. */
        {TwoPCCoordinatorPolicy.class, WriteThroughCachePolicy.class},
        /* java.lang.NullPointerException */
        {TwoPCCoordinatorPolicy.class, LoadBalancedMasterSlaveSyncPolicy.class}
    };

    @Parameterized.Parameter(0)
    public List<Class> dmClasses;

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
    public static Collection<Collection<Class>> data() {
        Utils.ArrayToStringComparator comparator = new Utils.ArrayToStringComparator();
        ArrayList<Collection<Class>> data = new ArrayList<Collection<Class>>();
        Arrays.sort(ignoredCombinations, comparator);
        logger.info("Ignored combinations: " + Arrays.toString(ignoredCombinations));
        // Add all combinations of two DMs
        for (Class first : allDmClasses) {
            for (Class second : allDmClasses) {
                if (!first.equals(second)) { // Don't test DMs in combination with themselves
                    Class[] combo = new Class[] {first, second};
                    int foundIndex = Arrays.binarySearch(ignoredCombinations, combo, comparator);
                    logger.info(
                            "Found Index: "
                                    + foundIndex
                                    + " for "
                                    + first.getName()
                                    + ", "
                                    + second.getName());
                    if (foundIndex < 0) {
                        data.add(Arrays.asList(combo));
                    }
                }
            }
        }
        // Explicitly add some extra combinations of more than 2 DMs
        for (Class[] combo : additionalCombinations) {
            data.add(Arrays.asList(combo));
        }
        // Explicitly remove all ignored combinations.
        for (Class[] combo : ignoredCombinations) {
            data.remove(Arrays.asList(combo));
        }
        logger.info(
                "Using data: Combinations: " + data.size() + ", DM classes: " + data.toString());
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
                while (System.currentTimeMillis() - startTime < RETRY_TIMEOUT_MS) {
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
                            if (!(dmClasses.indexOf(AtLeastOnceRPCPolicy.class) == 0)) {
                                /* If AtLeastOnceRPC is not the outermost DM, then we might need to
                                  retry, so swallow the exception and retry, after sleeping.
                                */
                                logger.info(
                                        "Swallowing runtime exception because AtLeastOnceRPC is not used.");
                                Thread.sleep(RETRY_PERIOD_MS);
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
                if (System.currentTimeMillis() - startTime >= RETRY_TIMEOUT_MS) {
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
    private MicroServiceSpec createMultiDMTestSpec(Collection<Class> dmClasses) throws Exception {
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

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

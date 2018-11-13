package sapphire.policy;

import static sapphire.kernel.IntegrationTestBase.*;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.concurrent.*;
import org.junit.*;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.SapphireObjectID;
import sapphire.demo.KVStore;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.policy.serializability.TransactionAlreadyStartedException;
import sapphire.policy.serializability.TransactionException;

/**
 * Test <strong>simple</strong> deployment managers. Complex DMs, e.g. Consensus, that require
 * multiple kernel servers are not covered here.
 */
public class SimpleDMIntegrationTest {
    private static String RESOURCE_PATH = "specs/simple-dm/";
    private static String RESOURCE_REAL_PATH;
    private static String kstIp = "127.0.0.1";
    private OMSServer oms;
    private SapphireObjectID sapphireObjId = null;

    @BeforeClass
    public static void bootstrap() throws Exception {
        startOmsAndKernelServers("r1");
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(RESOURCE_PATH);
        RESOURCE_REAL_PATH = url.getPath();
    }

    @Before
    public void setUp() throws Exception {
        Registry registry = LocateRegistry.getRegistry(omsIp, omsPort);
        oms = (OMSServer) registry.lookup("SapphireOMS");
        new KernelServerImpl(
                new InetSocketAddress(hostIp, hostPort), new InetSocketAddress(omsIp, omsPort));
    }

    private SapphireObjectSpec getSapphireObjectSpecForDM(String dmFileName) throws Exception {
        File file = new File(RESOURCE_REAL_PATH + dmFileName);
        return readSapphireSpec(file);
    }

    private SapphireObjectSpec readSapphireSpec(File file) throws Exception {
        List<String> lines = Files.readAllLines(file.toPath());
        String yamlStr = String.join("\n", lines);
        SapphireObjectSpec spec = SapphireObjectSpec.fromYaml(yamlStr);
        return spec;
    }

    /**
     * Generic test method to be used for a given DM
     *
     * @param dmFileName
     * @throws Exception
     */
    private void runTest(String dmFileName) throws Exception {
        SapphireObjectSpec spec = getSapphireObjectSpecForDM(dmFileName);
        sapphireObjId = oms.createSapphireObject(spec.toString());
        KVStore store = (KVStore) oms.acquireSapphireObjectStub(sapphireObjId);
        for (int i = 0; i < 10; i++) {
            String key = "k1_" + i;
            String value = "v1_" + i;
            store.set(key, value);
            Assert.assertEquals(value, store.get(key));
        }
    }

    /**
     * Method to execute two concurrent transactions on the given app client objects.
     *
     * @param client1
     * @param client2
     * @param expectedException
     * @throws Exception
     */
    private void concurrentTransaction(
            KVStore client1, KVStore client2, Class<? extends Exception> expectedException)
            throws Exception {
        class CustomTask implements Callable<Void> {
            private KVStore store;
            private String key;
            private String value;

            public CustomTask(KVStore store, String key, String value) {
                this.store = store;
                this.key = key;
                this.value = value;
            }

            @Override
            public Void call() throws Exception {
                store.startTransaction();
                for (int i = 0; i < 50; i++) {
                    store.set(key, value);
                }
                store.commitTransaction();
                return null;
            }
        }

        String key1 = "k1";
        String value1 = "v1";
        String key2 = "k2";
        String value2 = "v2";
        int failedTransactionCount = 0;

        /* Create a thread pool */
        ExecutorService executor = Executors.newFixedThreadPool(2);

        CustomTask task1 = new CustomTask(client1, key1, value1);
        CustomTask task2 = new CustomTask(client2, key2, value2);

        FutureTask<Void> futureTask1 = new FutureTask<>(task1);
        FutureTask<Void> futureTask2 = new FutureTask<>(task2);

        executor.execute(futureTask1);
        executor.execute(futureTask2);

        /* Wait for the tasks to be executed and collect the result. One thread or both the threads can succeed the
        transaction. Verify whether set operation is successful or expected exception has occurred */

        try {
            /* Collect the result */
            futureTask1.get();
            Assert.assertEquals(value1, client1.get(key1));
        } catch (ExecutionException e) {
            /* Execution exception wraps the exception thrown by thread */
            Assert.assertEquals(expectedException, e.getCause().getClass());
            failedTransactionCount++;
        }

        try {
            /* Collect the result */
            futureTask2.get();
            Assert.assertEquals(value2, client2.get(key2));
        } catch (ExecutionException e) {
            /* Execution exception wraps the exception thrown by thread */
            Assert.assertEquals(expectedException, e.getCause().getClass());
            failedTransactionCount++;
        }

        /* Both threads are never expected to fail. Atleast one thread must succeed. */
        Assert.assertNotEquals(2, failedTransactionCount);

        /* shut down the executor service */
        executor.shutdown();
    }

    /**
     * Test case with default DM
     *
     * @throws Exception
     */
    @Test
    public void runDefaultDMTest() throws Exception {
        runTest("NoDM.yaml");
    }

    /**
     * Test case with atleast once RPC DM
     *
     * @throws Exception
     */
    @Test
    public void runAtLeastOnceRpcDMTest() throws Exception {
        runTest("AtLeastOnceDM.yaml");
    }

    /**
     * Test case with cache upon lease DM
     *
     * @throws Exception
     */
    @Test
    public void runCacheLeaseDMTest() throws Exception {
        runTest("CacheLease.yaml");
    }

    /**
     * Test case for durable serializable RPC DM
     *
     * @throws Exception
     */
    @Test
    public void runDurableSerializableRPCDMTest() throws Exception {
        runTest("DurableSerializableRPCDM.yaml");
    }

    /**
     * Test case with serializable RPC DM
     *
     * @throws Exception
     */
    @Test
    public void runSerializableRPCDMTest() throws Exception {
        runTest("SerializableRPCDM.yaml");
    }

    /**
     * Test case with immutable DM
     *
     * @throws Exception
     */
    @Test
    public void runImmutableDMTest() throws Exception {
        runTest("Immutable.yaml");
    }

    /**
     * Test case with write through cache DM
     *
     * @throws Exception
     */
    @Test
    public void runWriteThroughCacheDMTest() throws Exception {
        runTest("WriteThroughCacheDM.yaml");
    }

    /**
     * Test case with DHT DM
     *
     * @throws Exception
     */
    @Test
    public void runDhtDMTest() throws Exception {
        runTest("DHTDM.yaml");
    }

    /**
     * Test case with locking transaction DM
     *
     * @throws Exception
     */
    @Test
    public void runLockingTransactionDMTest() throws Exception {
        SapphireObjectSpec spec = getSapphireObjectSpecForDM("LockingTransaction.yaml");
        sapphireObjId = oms.createSapphireObject(spec.toString());
        KVStore client1 = (KVStore) oms.acquireSapphireObjectStub(sapphireObjId);

        /* Test 1: Single app client with 2 threads doing concurrent transactions. One thread is expected to start the
        transaction. Other fails with transaction already started exception. Verify the value set in successful
        transaction thread. And verify the transaction already started exception for the failed one */
        concurrentTransaction(client1, client1, TransactionAlreadyStartedException.class);

        /* Get the second client stub */
        KVStore client2 = (KVStore) oms.acquireSapphireObjectStub(sapphireObjId);

        /* Test 2: Two app clients with a thread each doing concurrent transactions.One thread or both the threads can
        succeed the transaction. Verify whether set operation is successful or transaction exception has occurred */
        concurrentTransaction(client1, client2, TransactionException.class);
    }

    /**
     * Test case with optimistic concurrent transaction DM
     *
     * @throws Exception
     */
    @Test
    public void runOptimisticConcurrentTransactionDMTest() throws Exception {
        SapphireObjectSpec spec = getSapphireObjectSpecForDM("OptConcurrentTransactionDM.yaml");
        sapphireObjId = oms.createSapphireObject(spec.toString());
        KVStore client1 = (KVStore) oms.acquireSapphireObjectStub(sapphireObjId);

        /* Test 1: Single app client with 2 threads doing concurrent transactions. One thread is expected to start the
        transaction. Other fails with transaction already started exception.Verify the value set in successful
        transaction thread. And verify the transaction already started exception for the failed one */
        concurrentTransaction(client1, client1, TransactionAlreadyStartedException.class);

        KVStore client2 = (KVStore) oms.acquireSapphireObjectStub(sapphireObjId);

        /* Test 2: Two app clients with a thread each doing concurrent transactions.One thread or both the threads can
        succeed the transaction. Verify whether set operation is successful or transaction exception has occurred */
        concurrentTransaction(client1, client2, TransactionException.class);
    }

    /**
     * Test case with explicit migration DM
     *
     * @throws Exception
     */
    @Test
    public void runExplicitMigration() throws Exception {
        SapphireObjectSpec spec = getSapphireObjectSpecForDM("ExplicitMigrationDM.yaml");
        sapphireObjId = oms.createSapphireObject(spec.toString());
        KVStore store = (KVStore) oms.acquireSapphireObjectStub(sapphireObjId);
        String key0 = "k1";
        String value0 = "v1_0";
        store.set(key0, value0);
        Assert.assertEquals(value0, store.get(key0));

        /* Sapphire object is created on a random server. It is not known on which server SO is created.
           Migrate SO twice to ensure migration to other server happens atleast once.
        */

        /* Migrate SO to first server and verify the value */
        store.migrateObject(new InetSocketAddress(ksIp, ksPort[0]));
        Assert.assertEquals(value0, store.get(key0));

        /* Add another key-value entry and verify */
        String key1 = "k2";
        String value1 = "v1_1";
        store.set(key1, value1);
        Assert.assertEquals(value1, store.get(key1));

        /* Migrate SO to second server and verify the values */
        store.migrateObject(new InetSocketAddress(ksIp, ksPort[1]));
        Assert.assertEquals(value0, store.get(key0));
        Assert.assertEquals(value1, store.get(key1));
    }

    /**
     * Test case with explicit caching DM
     *
     * @throws Exception
     */
    @Test
    public void runExplicitCachingTest() throws Exception {
        SapphireObjectSpec spec = getSapphireObjectSpecForDM("ExplicitCachingDM.yaml");
        sapphireObjId = oms.createSapphireObject(spec.toString());
        KVStore store = (KVStore) oms.acquireSapphireObjectStub(sapphireObjId);

        /* Cache the object */
        store.pull();

        for (int i = 0; i < 10; i++) {
            String key = "k1_" + i;
            String value = "v1_" + i;

            /* update on local cached object */
            store.set(key, value);
            Assert.assertEquals(value, store.get(key));
        }

        /* Push the modified cached object to server */
        store.push();

        /* Verify the map values on the server */
        for (int i = 0; i < 10; i++) {
            String key = "k1_" + i;
            String value = "v1_" + i;
            Assert.assertEquals(value, store.get(key));
        }
    }

    /**
     * Test case with explicit checkpoint DM
     *
     * @throws Exception
     */
    @Test
    public void runExplicitCheckPointTest() throws Exception {
        SapphireObjectSpec spec = getSapphireObjectSpecForDM("ExplicitCheckpointDM.yaml");
        sapphireObjId = oms.createSapphireObject(spec.toString());
        KVStore store = (KVStore) oms.acquireSapphireObjectStub(sapphireObjId);

        String key = "k1";
        String value0 = "v1_0";
        store.set(key, value0);

        /* checkpoint this state */
        store.saveCheckpoint();
        Assert.assertEquals(value0, store.get(key));

        /* Set another value and verify it */
        String value1 = "v1_1";
        store.set(key, value1);
        Assert.assertEquals(value1, store.get(key));

        /* Restore to previous state */
        store.restoreCheckpoint();

        /* verify the restore value */
        Assert.assertEquals(value0, store.get(key));
    }

    /**
     * Test case with periodic checkpoint DM
     *
     * @throws Exception
     */
    @Test
    public void runPeriodicCheckpointTest() throws Exception {
        SapphireObjectSpec spec = getSapphireObjectSpecForDM("PeriodicCheckpointDM.yaml");
        sapphireObjId = oms.createSapphireObject(spec.toString());
        KVStore store = (KVStore) oms.acquireSapphireObjectStub(sapphireObjId);

        String key = "k1";
        String preValue = null;
        int checkpointPeriod =
                sapphire.policy.checkpoint.periodiccheckpoint.PeriodicCheckpointPolicy.ServerPolicy
                        .MAX_RPCS_BEFORE_CHECKPOINT;
        for (int i = 1; i <= checkpointPeriod + 1; i++) {
            String value = "v1_" + i;
            store.set(key, value);
            Assert.assertEquals(value, store.get(key));

            if (i % checkpointPeriod == 0) {
                /* record the checkpointed value */
                preValue = value;
            }
        }

        /* Restore to previous state */
        store.restoreCheckpoint();

        /* verify the restore value */
        Assert.assertEquals(preValue, store.get(key));
    }

    @After
    public void tearDown() throws Exception {
        if (sapphireObjId != null) {
            oms.deleteSapphireObject(sapphireObjId);
        }
    }

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

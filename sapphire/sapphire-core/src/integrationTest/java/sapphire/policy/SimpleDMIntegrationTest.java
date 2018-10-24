package sapphire.policy;

import static sapphire.kernel.IntegrationTestBase.*;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import org.junit.*;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.SapphireObjectID;
import sapphire.demo.KVStore;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

/**
 * Test <strong>simple</strong> deployment managers. Complex DMs, e.g. Consensus, that require
 * multiple kernel servers are not covered here.
 */
public class SimpleDMIntegrationTest {
    private static String kstIp = "127.0.0.1";
    private static int ksPort = 22345;
    private OMSServer oms;

    @BeforeClass
    public static void bootstrap() throws Exception {
        startOmsAndKernelServers("r1");
    }

    @Before
    public void setUp() throws Exception {
        Registry registry = LocateRegistry.getRegistry(omsIp, omsPort);
        oms = (OMSServer) registry.lookup("SapphireOMS");
        new KernelServerImpl(
                new InetSocketAddress(hostIp, hostPort), new InetSocketAddress(omsIp, omsPort));
    }

    /**
     * Test sapphire object specifications in <code>src/test/resources/specs</code> directory.
     *
     * @throws Exception
     */
    @Test
    public void testDMs() throws Exception {
        File[] files = getResourceFiles("specs/simple-dm/");
        for (File f : files) {
            if (!f.getName().endsWith(".yaml")) {
                continue;
            }

            SapphireObjectSpec spec = readSapphireSpec(f);
            if (f.getName().startsWith("LockingTransaction")) {
                runLockingTransactionTest(spec);
            } else if (f.getName().startsWith("ExplicitCaching")) {
                runExplicitCachingTest(spec);
            } else if (f.getName().startsWith("ExplicitCheckpoint")) {
                runExplicitCheckPointTest(spec);
            } else if (f.getName().startsWith("PeriodicCheckpoint")) {
                runPeriodicCheckpointTest(spec);
            } else if (f.getName().startsWith("ExplicitMigration")) {
                runExplicitMigration(spec);
            } else {
                runTest(spec);
            }
        }
    }

    /**
     * Returns files from given subdirectory in resources.
     *
     * @param folder the path relative to src/test/resources
     * @return a list of {@link File}s one for each file in src/test/resources/folder
     */
    private static File[] getResourceFiles(String folder) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(folder);
        String path = url.getPath();
        return new File(path).listFiles();
    }

    private SapphireObjectSpec readSapphireSpec(File file) throws Exception {
        List<String> lines = Files.readAllLines(file.toPath());
        String yamlStr = String.join("\n", lines);
        SapphireObjectSpec spec = SapphireObjectSpec.fromYaml(yamlStr);
        return spec;
    }

    private void runTest(SapphireObjectSpec spec) throws Exception {
        SapphireObjectID sapphireObjId = oms.createSapphireObject(spec.toString());
        KVStore store = (KVStore) oms.acquireSapphireObjectStub(sapphireObjId);
        for (int i = 0; i < 10; i++) {
            String key = "k1_" + i;
            String value = "v1_" + i;
            store.set(key, value);
            Assert.assertEquals(value, store.get(key));
        }
    }

    private void runLockingTransactionTest(SapphireObjectSpec spec) throws Exception {
        SapphireObjectID sapphireObjId = oms.createSapphireObject(spec.toString());
        KVStore store = (KVStore) oms.acquireSapphireObjectStub(sapphireObjId);
        for (int i = 0; i < 10; i++) {
            String key = "k1_" + i;
            String value = "v1_" + i;
            store.startTransaction();
            store.set(key, value);
            store.commitTransaction();
            Assert.assertEquals(value, store.get(key));
        }
    }

    private void runExplicitMigration(SapphireObjectSpec spec) throws Exception {
        SapphireObjectID sapphireObjId = oms.createSapphireObject(spec.toString());
        KVStore store = (KVStore) oms.acquireSapphireObjectStub(sapphireObjId);
        String key0 = "k1";
        String value0 = "v1_0";
        store.set(key0, value0);
        Assert.assertEquals(value0, store.get(key0));

        /* Sapphire object is created on a random server. It is not known on which server SO is created.
           Migrate SO twice to ensure migration to other server happens atleast once.
        */

        /* Migrate SO to first server and verify the value */
        store.migrateObject(new InetSocketAddress(ksIp, ks1Port));
        Assert.assertEquals(value0, store.get(key0));

        /* Add another key-value entry and verify */
        String key1 = "k2";
        String value1 = "v1_1";
        store.set(key1, value1);
        Assert.assertEquals(value1, store.get(key1));

        /* Migrate SO to second server and verify the values */
        store.migrateObject(new InetSocketAddress(ksIp, ks2Port));
        Assert.assertEquals(value0, store.get(key0));
        Assert.assertEquals(value1, store.get(key1));
    }

    private void runExplicitCachingTest(SapphireObjectSpec spec) throws Exception {
        SapphireObjectID sapphireObjId = oms.createSapphireObject(spec.toString());
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

    private void runExplicitCheckPointTest(SapphireObjectSpec spec) throws Exception {
        SapphireObjectID sapphireObjId = oms.createSapphireObject(spec.toString());
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

    private void runPeriodicCheckpointTest(SapphireObjectSpec spec) throws Exception {
        SapphireObjectID sapphireObjId = oms.createSapphireObject(spec.toString());
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

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

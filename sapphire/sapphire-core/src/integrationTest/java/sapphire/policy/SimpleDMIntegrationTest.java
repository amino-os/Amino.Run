package sapphire.policy;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.junit.*;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.SapphireObjectID;
import sapphire.demo.KVStore;
import sapphire.kernel.IntegrationTestBase;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

/**
 * Test <strong>simple</strong> deployment managers. Complex DMs, e.g. Consensus, that require
 * multiple kernel servers are not covered here.
 */
public class SimpleDMIntegrationTest  extends IntegrationTestBase {

    private static int BASE_PORT = 25001; // Make sure that this does not overlap with other tests or processes running on the machine.

    int hostPort;

    public SimpleDMIntegrationTest() throws Exception {
        super(BASE_PORT);
        hostPort = BASE_PORT -1;
    }

    @Before
    public void setup() {
        createEmbeddedKernelServer(hostPort);
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

    private void runTest(SapphireObjectSpec spec) throws Exception {
        SapphireObjectID sapphireObjId = getOms().createSapphireObject(spec.toString());
        KVStore store = (KVStore) getOms().acquireSapphireObjectStub(sapphireObjId);
        for (int i = 0; i < 10; i++) {
            String key = "k1_" + i;
            String value = "v1_" + i;
            store.set(key, value);
            Assert.assertEquals(value, store.get(key));
        }
    }

    private void runLockingTransactionTest(SapphireObjectSpec spec) throws Exception {
        SapphireObjectID sapphireObjId = getOms().createSapphireObject(spec.toString());
        KVStore store = (KVStore) getOms().acquireSapphireObjectStub(sapphireObjId);
        for (int i = 0; i < 10; i++) {
            String key = "k1_" + i;
            String value = "v1_" + i;
            store.startTransaction();
            store.set(key, value);
            store.commitTransaction();
            Assert.assertEquals(value, store.get(key));
        }
    }
}

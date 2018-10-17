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

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

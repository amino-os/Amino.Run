package amino.run.app;

import static amino.run.kernel.IntegrationTestBase.*;
import static amino.run.kernel.IntegrationTestBase.readSapphireSpec;

import amino.run.common.AppObjectStub;
import amino.run.demo.KVStore;
import amino.run.kernel.server.KernelServerImpl;
import java.io.File;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import org.junit.*;

/**
 * Test <strong>LabelSelector</strong> deployment managers, DHT & Consensus , DHT & MasterSlave,
 * AtLeastOnceRPC & DHT & Consensus , AtLeastOnceRPC & DHT & MasterSlave with multiple kernel
 * servers are covered here.
 */
public class LabelSelector {
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
        this.registry = (Registry) registry.lookup("SapphireOMS");
        new KernelServerImpl(
                new InetSocketAddress(hostIp, hostPort), new InetSocketAddress(omsIp, omsPort));
    }

    private void runTest(MicroServiceSpec spec) throws Exception {
        registry.create(spec.toString());
        ArrayList<AppObjectStub> stores = registry.acquireStub(spec.getLabels().asSelector());
        // only one MicroService app deployed
        Assert.assertEquals(1, stores.size());
        // get app stub
        KVStore store = (KVStore) stores.get(0);

        for (int i = 0; i < 10; i++) {
            String key = "k1_" + i;
            String value = "v1_" + i;
            store.set(key, value);
            waitForValue(store, key, value, -1);
            Assert.assertEquals(value, store.get(key));
        }
    }

    /**
     * Test sapphire object specifications in <code>
     * src/integrationTest/resources/specs/label-selector
     * </code> directory.
     *
     * @throws Exception
     */
    @Test
    public void testLabelSelector() throws Exception {
        File file = getResourceFile("specs/label-selector/KVStore.yaml");
        MicroServiceSpec spec = readSapphireSpec(file);
        runTest(spec);
    }

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

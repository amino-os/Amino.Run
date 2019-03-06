package amino.run.app;

import amino.run.app.labelselector.Labels;
import amino.run.app.labelselector.Requirement;
import amino.run.app.labelselector.Selector;
import amino.run.common.AppObjectStub;
import amino.run.demo.KVStore;
import amino.run.kernel.IntegrationTestBase;
import amino.run.kernel.server.KernelServerImpl;
import java.io.File;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import org.junit.*;

/**
 * Test <strong>LabelSelector</strong> for Equal, IN, NotIN and Exists requirements with multiple kernel
 * servers are covered here.
 */
public class LabelSelector extends IntegrationTestBase {
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

    private void runTest(KVStore store) throws Exception {
        for (int i = 0; i < 10; i++) {
            String key = "k1_" + i;
            String value = "v1_" + i;
            store.set(key, value);
            waitForValue(store, key, value, -1);
            Assert.assertEquals(value, store.get(key));
        }
    }

    /**
     * Test MicroService specifications in <code>
     * src/integrationTest/resources/specs/label-selector
     * </code> directory.
     *
     * @throws Exception
     */
    @Test
    public void testLabelSelector() throws Exception {
        File file = getResourceFile("specs/label-selector/KVStore.yaml");
        MicroServiceSpec spec = readSapphireSpec(file);
        String labelKey = "UUID";
        String labelValue = UUID.randomUUID().toString();
        spec.getLabels().put(labelKey, labelValue);
        registry.create(spec.toString());
        // acquire AppObjectStub with labels defined in configuration
        ArrayList<AppObjectStub> stores = registry.acquireStub(spec.getLabels().asSelector());
        // only one MicroService app deployed
        Assert.assertEquals(1, stores.size());
        // get app stub
        KVStore store = (KVStore) stores.get(0);

        runTest(store);
    }

    /**
     * Test Label-Selector with IN selector
     *
     * @throws Exception
     */
    @Test
    public void testLabelSelectorWithINRequirement() throws Exception {
        File file = getResourceFile("specs/label-selector/KVStore.yaml");
        MicroServiceSpec spec = readSapphireSpec(file);
        String labelKey = "UUID";
        String labelValue = UUID.randomUUID().toString();
        spec.getLabels().put(labelKey, labelValue);
        registry.create(spec.toString());

        // generate IN selector
        Selector selector =
                new Selector()
                        .add(
                                new Requirement(
                                        labelKey,
                                        Requirement.In,
                                        new ArrayList<>(Collections.singletonList(labelValue))));
        // acquire AppObjectStub with labels defined in configuration
        ArrayList<AppObjectStub> stores = registry.acquireStub(selector);
        // only one MicroService app deployed
        Assert.assertEquals(1, stores.size());
        // get app stub
        KVStore store = (KVStore) stores.get(0);

        runTest(store);
    }

    /**
     * Test Label-Selector with NotIn selector
     *
     * @throws Exception
     */
    @Test
    public void testLabelSelectorWithNotINRequirement() throws Exception {
        File file = getResourceFile("specs/label-selector/KVStore.yaml");
        MicroServiceSpec spec = readSapphireSpec(file);
        Labels labels = spec.getLabels();
        String configLabelKey = "name";
        String configLabelValue = labels.get(configLabelKey);
        // update default config value
        String updatedValue = "UpdatedKVStore";
        spec.getLabels().put(configLabelKey, updatedValue);
        registry.create(spec.toString());

        // generate NotIN selector
        // As all other MicroService will get deployed with "name=KVStore",
        // This MicroService will get deployed with "name=UpdatedKVStore".
        // So "name NotIN {KVStore}" will always return MicroService deployed in this testcase
        Selector selector =
                new Selector()
                        .add(
                                new Requirement(
                                        configLabelKey,
                                        Requirement.NotIn,
                                        new ArrayList<>(Collections.singletonList(configLabelValue))));
        // acquire AppObjectStub with labels defined in configuration
        ArrayList<AppObjectStub> stores = registry.acquireStub(selector);
        // only one MicroService app deployed
        Assert.assertEquals(1, stores.size());
        // get app stub
        KVStore store = (KVStore) stores.get(0);

        runTest(store);
    }

    /**
     * Test Label-Selector with Exist selector
     *
     * @throws Exception
     */
    @Test
    public void testLabelSelectorWithExistRequirement() throws Exception {
        File file = getResourceFile("specs/label-selector/KVStore.yaml");
        MicroServiceSpec spec = readSapphireSpec(file);
        // generate random unique key
        String labelKey = UUID.randomUUID().toString();
        String labelValue = UUID.randomUUID().toString();
        spec.getLabels().put(labelKey, labelValue);
        registry.create(spec.toString());

        // generate Exists selector
        Selector selector =
                new Selector()
                        .add(
                                new Requirement(
                                        labelKey,
                                        Requirement.Exists,
                                        new ArrayList<>(Collections.emptyList())));
        // acquire AppObjectStub with labels defined in configuration
        ArrayList<AppObjectStub> stores = registry.acquireStub(selector);
        // only one MicroService app deployed
        Assert.assertEquals(1, stores.size());
        // get app stub
        KVStore store = (KVStore) stores.get(0);

        runTest(store);
    }



    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

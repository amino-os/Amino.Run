package amino.run.nodeselection;

import amino.run.app.*;
import amino.run.common.MicroServiceID;
import amino.run.demo.KVStore;
import amino.run.kernel.IntegrationTestBase;
import amino.run.kernel.server.KernelServerImpl;
import java.io.File;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.Collections;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/** Test <strong>node selection</strong> for MicroService deployment with different DMs */
public class NodeSelectionTestCases extends IntegrationTestBase {
    Registry registry;
    private static String regionName = "default-region";
    private static String kernelServerSpecificKey = "kernelServer";

    @BeforeClass
    public static void bootstrap() throws Exception {
        String labels = KernelServerImpl.REGION_KEY + "=" + regionName;

        // start OMS
        omsProcess = startOms(omsIp, omsPort);

        // start kernel servers
        for (int i = 0; i < ksPort.length; i++) {
            String kernelServerSpecificLabel = kernelServerSpecificKey + "=" + i;
            kernelServerProcess[i] =
                    startKernelServer(
                            ksIp,
                            ksPort[i],
                            omsIp,
                            omsPort,
                            String.join(",", labels, kernelServerSpecificLabel));
        }
    }

    @Before
    public void setUp() throws Exception {
        java.rmi.registry.Registry registry = LocateRegistry.getRegistry(omsIp, omsPort);
        this.registry = (Registry) registry.lookup("io.amino.run.oms");
        new KernelServerImpl(
                new InetSocketAddress(hostIp, hostPort), new InetSocketAddress(omsIp, omsPort));
    }

    private void runTest(MicroServiceSpec spec, boolean consensus) throws Exception {
        MicroServiceID microServiceId = registry.create(spec.toString());
        KVStore store = (KVStore) registry.acquireStub(microServiceId);
        // consensus DM needs some time to elect the leader other wise function call will fail
        if (consensus) {
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
     * Test microservice specifications in <code>src/integrationTest/resources/specs/multi-dm
     * </code> directory.
     *
     * @throws Exception
     */
    @Test
    public void testDHTNConsensusMultiDM() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHTNConsensus.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        spec.setNodeSelectorSpec(
                new NodeSelectorSpec()
                        .addRequireExpressions(
                                new NodeSelectorTerm()
                                        .add(
                                                new Requirement(
                                                        kernelServerSpecificKey,
                                                        Operator.Equal,
                                                        Collections.singletonList("0")))));
        runTest(spec, true);
    }

    /**
     * Test microservice specifications in <code>src/integrationTest/resources/specs/multi-dm
     * </code> directory.
     *
     * @throws Exception
     */
    @Test
    public void testDHTNMasterSlaveMultiDM() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHTNMasterSlave.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        spec.setNodeSelectorSpec(
                new NodeSelectorSpec()
                        .addRequireExpressions(
                                new NodeSelectorTerm()
                                        .add(
                                                new Requirement(
                                                        kernelServerSpecificKey,
                                                        Operator.Equal,
                                                        Collections.singletonList("0")))));
        runTest(spec, false);
    }

    /**
     * Test microservice specifications in <code>src/integrationTest/resources/specs/multi-dm
     * </code> directory.
     *
     * @throws Exception
     */
    @Test
    @Ignore("Test is ignored will be removed once the multi DM issues are resolved")
    public void testAtleastOnceRPCDHTNMasterSlaveMultiDM() throws Exception {
        File file = getResourceFile("specs/multi-dm/AtleastRPCDHTNMasterSlave.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        spec.setNodeSelectorSpec(
                new NodeSelectorSpec()
                        .addRequireExpressions(
                                new NodeSelectorTerm()
                                        .add(
                                                new Requirement(
                                                        kernelServerSpecificKey,
                                                        Operator.Equal,
                                                        Collections.singletonList("0")))));
        runTest(spec, false);
    }

    /**
     * Test microservice specifications in <code>src/integrationTest/resources/specs/multi-dm
     * </code> directory.
     *
     * @throws Exception
     */
    @Test
    @Ignore("Test is ignored will be removed once the multi DM issues are resolved")
    public void testAtleastOnceRPCDHTNConsensusMultiDM() throws Exception {
        File file = getResourceFile("specs/multi-dm/AtleastRPCDHTNConsensus.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        spec.setNodeSelectorSpec(
                new NodeSelectorSpec()
                        .addRequireExpressions(
                                new NodeSelectorTerm()
                                        .add(
                                                new Requirement(
                                                        kernelServerSpecificKey,
                                                        Operator.Equal,
                                                        Collections.singletonList("0")))));
        runTest(spec, true);
    }

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

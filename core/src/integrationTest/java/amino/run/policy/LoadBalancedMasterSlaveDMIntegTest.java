package amino.run.policy;

import static amino.run.kernel.IntegrationTestBase.getResourceFile;
import static amino.run.kernel.IntegrationTestBase.hostIp;
import static amino.run.kernel.IntegrationTestBase.hostPort;
import static amino.run.kernel.IntegrationTestBase.killOmsAndKernelServers;
import static amino.run.kernel.IntegrationTestBase.omsIp;
import static amino.run.kernel.IntegrationTestBase.omsPort;
import static amino.run.kernel.IntegrationTestBase.readMicroServiceSpec;
import static amino.run.kernel.IntegrationTestBase.startOmsAndKernelServers;

import amino.run.app.MicroServiceSpec;
import amino.run.app.Registry;
import amino.run.common.MicroServiceID;
import amino.run.demo.KVStore;
import amino.run.kernel.server.KernelServerImpl;
import java.io.File;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class LoadBalancedMasterSlaveDMIntegTest {
    Registry registry;
    MicroServiceID microServiceId = null;

    @BeforeClass
    public static void bootstrap() throws Exception {
        startOmsAndKernelServers(null);
    }

    @Before
    public void setUp() throws Exception {
        java.rmi.registry.Registry registry = LocateRegistry.getRegistry(omsIp, omsPort);
        this.registry = (Registry) registry.lookup("io.amino.run.oms");
        new KernelServerImpl(
                new InetSocketAddress(hostIp, hostPort), new InetSocketAddress(omsIp, omsPort));
    }

    private void runTest(MicroServiceSpec spec) throws Exception {
        microServiceId = registry.create(spec.toString());
        KVStore store = (KVStore) registry.acquireStub(microServiceId);
        for (int i = 0; i < 10; i++) {
            String key = "k1_" + i;
            String value = "v1_" + i;
            store.set(key, value);
            Assert.assertEquals(value, store.get(key));
        }
    }

    /**
     * Test microservice specifications in <code>src/test/resources/specs</code> directory.
     *
     * @throws Exception
     */
    @Test
    public void testMasterSlaveDM() throws Exception {
        File file = getResourceFile("specs/complex-dm/LoadBalanceMasterSlave.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec);
    }

    @After
    public void tearDown() throws Exception {
        if (microServiceId != null) {
            registry.delete(microServiceId);
            microServiceId = null;
        }
    }

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

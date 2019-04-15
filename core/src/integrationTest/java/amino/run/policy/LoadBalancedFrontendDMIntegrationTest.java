package amino.run.policy;

import static amino.run.kernel.IntegrationTestBase.*;

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

/**
 * Test <strong>complex</strong> deployment managers, e.g. LoadBalancedFrontend, that require
 * multiple kernel servers are covered here.
 */
public class LoadBalancedFrontendDMIntegrationTest {
    Registry registry;
    MicroServiceID microServiceId = null;

    @BeforeClass
    public static void bootstrap() throws Exception {
        String labels = KernelServerImpl.REGION_KEY + "=IND";
        startOmsAndKernelServers(labels);
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
            store.set(key, value);
            Assert.assertEquals(value, store.get(key));
        }
    }

    @Test
    public void testLoadBalancedFrontendDMs() throws Exception {
        File file = getResourceFile("specs/complex-dm/LoadBalancedFrontEnd.yaml");
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

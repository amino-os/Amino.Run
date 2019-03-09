package amino.run.policy;

import static amino.run.kernel.IntegrationTestBase.*;
import static java.lang.Thread.sleep;

import amino.run.app.MicroServiceSpec;
import amino.run.app.Registry;
import amino.run.common.MicroServiceID;
import amino.run.demo.KVStore;
import amino.run.kernel.server.KernelServerImpl;
import java.io.File;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test <strong>complex</strong> deployment managers, e.g. Consensus, that require multiple kernel
 * servers are covered here.
 */
public class ConsensusDMIntegrationTest {
    Registry registry;

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
        MicroServiceID microServiceId = registry.create(spec.toString());
        sleep(5000);
        KVStore store = (KVStore) registry.acquireStub(microServiceId);
        for (int i = 0; i < 10; i++) {
            String key = "k1_" + i;
            String value = "v1_" + i;
            store.set(key, value);
            sleep(500);
            Assert.assertEquals(value, store.get(key));
        }
    }

    /**
     * Test microservice specifications in <code>src/test/resources/specs</code> directory.
     *
     * @throws Exception
     */
    @Test
    public void testConsensusDM() throws Exception {
        File file = getResourceFile("specs/complex-dm/Consensus.yaml");
        MicroServiceSpec spec = readMicroServiceSpec(file);
        runTest(spec);
    }

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

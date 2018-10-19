package sapphire.policy;

import static sapphire.kernel.IntegrationTestBase.*;

import java.io.File;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.SapphireObjectID;
import sapphire.demo.KVStore;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

/**
 * Test <strong>complex</strong> deployment managers, e.g. LoadBalancedFrontend, that require
 * multiple kernel servers are covered here.
 */
public class LoadBalancedFrontendDMIntegrationTest {
    private static String region = "IND";
    OMSServer oms;

    @BeforeClass
    public static void bootstrap() throws Exception {
        startOmsAndKernelServers(region);
    }

    @Before
    public void setUp() throws Exception {
        Registry registry = LocateRegistry.getRegistry(omsIp, omsPort);
        oms = (OMSServer) registry.lookup("SapphireOMS");
        KernelServer ks =
                new KernelServerImpl(
                        new InetSocketAddress(hostIp, hostPort),
                        new InetSocketAddress(omsIp, omsPort));
        ((KernelServerImpl) ks).setRegion(region);
    }

    private void runTest(SapphireObjectSpec spec) throws Exception {
        SapphireObjectID sapphireObjId = oms.createSapphireObject(spec.toString());
        KVStore store = (KVStore) oms.acquireSapphireObjectStub(sapphireObjId);
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
        SapphireObjectSpec spec = readSapphireSpec(file);
        runTest(spec);
    }

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

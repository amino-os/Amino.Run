package sapphire.policy;

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
import sapphire.kernel.IntegrationTestBase;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

/**
 * Test <strong>complex</strong> deployment managers, e.g. LoadBalancedFrontend, that require
 * multiple kernel servers are covered here.
 */
public class LoadBalancedFrontendDMIntegrationTest extends IntegrationTestBase {

    private static int BASE_PORT = 23001; // Make sure that this does not overlap with other tests or processes running on the machine.

    int hostPort;

    public LoadBalancedFrontendDMIntegrationTest() throws Exception {
        super(BASE_PORT);
        hostPort = BASE_PORT-1;
    }

    @Before
    public void setup() {
        createEmbeddedKernelServer(hostPort);
    }

    private void runTest(SapphireObjectSpec spec) throws Exception {
        SapphireObjectID sapphireObjId = getOms().createSapphireObject(spec.toString());
        KVStore store = (KVStore) getOms().acquireSapphireObjectStub(sapphireObjId);
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
}

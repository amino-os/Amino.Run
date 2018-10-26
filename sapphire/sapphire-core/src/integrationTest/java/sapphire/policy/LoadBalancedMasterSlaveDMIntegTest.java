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
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

public class LoadBalancedMasterSlaveDMIntegTest extends IntegrationTestBase{

    private static int BASE_PORT = 24001; // Make sure that this does not overlap with other tests or processes running on the machine.

    int hostPort;

    public LoadBalancedMasterSlaveDMIntegTest() throws Exception {
        super(BASE_PORT);
        hostPort = BASE_PORT - 1;
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
            Assert.assertEquals(value, store.get(key));
        }
    }

    /**
     * Test sapphire object specifications in <code>src/test/resources/specs</code> directory.
     *
     * @throws Exception
     */
    @Test
    public void testMasterSlaveDM() throws Exception {
        File file = getResourceFile("specs/complex-dm/LoadBalanceMasterSlave.yaml");
        SapphireObjectSpec spec = readSapphireSpec(file);
        runTest(spec);
    }
}

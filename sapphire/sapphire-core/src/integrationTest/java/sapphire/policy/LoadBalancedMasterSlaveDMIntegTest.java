package sapphire.policy;

import static sapphire.kernel.IntegrationTestBase.getResourceFile;
import static sapphire.kernel.IntegrationTestBase.hostIp;
import static sapphire.kernel.IntegrationTestBase.hostPort;
import static sapphire.kernel.IntegrationTestBase.killOmsAndKernelServers;
import static sapphire.kernel.IntegrationTestBase.omsIp;
import static sapphire.kernel.IntegrationTestBase.omsPort;
import static sapphire.kernel.IntegrationTestBase.readSapphireSpec;
import static sapphire.kernel.IntegrationTestBase.startOmsAndKernelServers;

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
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

public class LoadBalancedMasterSlaveDMIntegTest {
    OMSServer oms;

    @BeforeClass
    public static void bootstrap() throws Exception {
        startOmsAndKernelServers("");
    }

    @Before
    public void setUp() throws Exception {
        Registry registry = LocateRegistry.getRegistry(omsIp, omsPort);
        oms = (OMSServer) registry.lookup("SapphireOMS");
        new KernelServerImpl(
                new InetSocketAddress(hostIp, hostPort), new InetSocketAddress(omsIp, omsPort));
    }

    private void runTest(SapphireObjectSpec spec) throws Exception {
        String key = "k1";
        String value = "v1";

        SapphireObjectID sapphireObjId = oms.createSapphireObject(spec.toString());
        KVStore store = (KVStore) oms.acquireSapphireObjectStub(sapphireObjId);
        store.set(key, value);
        Assert.assertEquals(value, store.get(key));
    }

    /**
     * Test sapphire object specifications in <code>src/test/resources/specs</code> directory.
     *
     * @throws Exception
     */
    @Test
    public void testMasterSlaveDM() throws Exception {
        try {
            File file = getResourceFile("specs/complex-dm/LoadBalanceMasterSlave.yaml");
            SapphireObjectSpec spec = readSapphireSpec(file);
            System.out.println("Running test for DM: " + spec.getDmList());
            runTest(spec);
            System.out.println("Test passed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

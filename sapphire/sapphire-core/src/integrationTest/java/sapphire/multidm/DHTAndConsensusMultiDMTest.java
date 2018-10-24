package sapphire.multidm;

import static java.lang.Thread.sleep;
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
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

/**
 * Test <strong>multi-dm</strong> deployment managers, DHT & Consensus require multiple kernel
 * servers are covered here.
 */
public class DHTAndConsensusMultiDMTest {
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
        SapphireObjectID sapphireObjId = oms.createSapphireObject(spec.toString());
        sleep(5000);
        KVStore store = (KVStore) oms.acquireSapphireObjectStub(sapphireObjId);
        for (int i = 0; i < 10; i++) {
            String key = "k1_" + i;
            String value = "v1_" + i;
            store.set(key, value);
            sleep(500);
            Assert.assertEquals(value, store.get(key));
        }
    }

    /**
     * Test sapphire object specifications in <code>src/test/resources/specs</code> directory.
     *
     * @throws Exception
     */
    @Test
    public void testDHTNConsensusMultiDM() throws Exception {
        File file = getResourceFile("specs/multi-dm/DHTNConsensus.yaml");
        SapphireObjectSpec spec = readSapphireSpec(file);
        runTest(spec);
    }

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

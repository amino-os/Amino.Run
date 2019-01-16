package amino.run.policy;

import static amino.run.kernel.IntegrationTestBase.*;
import static java.lang.Thread.sleep;

import amino.run.app.SapphireObjectServer;
import amino.run.app.SapphireObjectSpec;
import amino.run.common.SapphireObjectID;
import amino.run.demo.KVStore;
import amino.run.kernel.server.KernelServerImpl;
import java.io.File;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
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
    SapphireObjectServer sapphireObjectServer;

    @BeforeClass
    public static void bootstrap() throws Exception {
        startOmsAndKernelServers(null);
    }

    @Before
    public void setUp() throws Exception {
        Registry registry = LocateRegistry.getRegistry(omsIp, omsPort);
        sapphireObjectServer = (SapphireObjectServer) registry.lookup("SapphireOMS");
        new KernelServerImpl(
                new InetSocketAddress(hostIp, hostPort), new InetSocketAddress(omsIp, omsPort));
    }

    private void runTest(SapphireObjectSpec spec) throws Exception {
        SapphireObjectID sapphireObjId = sapphireObjectServer.createSapphireObject(spec.toString());
        sleep(5000);
        KVStore store = (KVStore) sapphireObjectServer.acquireSapphireObjectStub(sapphireObjId);
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
    public void testConsensusDM() throws Exception {
        File file = getResourceFile("specs/complex-dm/Consensus.yaml");
        SapphireObjectSpec spec = readSapphireSpec(file);
        runTest(spec);
    }

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

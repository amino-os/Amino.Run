package sapphire.policy;

import junit.framework.AssertionFailedError;

import static java.lang.System.out;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import sapphire.kernel.IntegrationTestBase;

import java.io.File;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.junit.AfterClass;
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
 * Test <strong>complex</strong> deployment managers, e.g. Consensus, that require multiple kernel
 * servers are covered here.
 */
public class ConsensusDMIntegrationTest extends IntegrationTestBase {

    private static int BASE_PORT = 26001; // Make sure that this does not overlap with other tests or processes running on the machine.

    int hostPort;

    public ConsensusDMIntegrationTest() throws Exception {
        super(BASE_PORT);
        hostPort = BASE_PORT - 1;
    }

    @Before
    public void setup() {
        createEmbeddedKernelServer(hostPort);
    }

    private void waitForSet(KVStore store, String key, String value) throws java.rmi.RemoteException, InterruptedException {
        final long WAIT_TIMEOUT_MS = 500;
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < WAIT_TIMEOUT_MS) {
            try {
                store.set(key, value);
            }
            catch (java.lang.RuntimeException e) {
                // e.printStackTrace();
                if (System.currentTimeMillis() - startTime < WAIT_TIMEOUT_MS) {
                    // Do nothing and try again
                    sleep(100);
                } else {
                    throw e;
                }
            }
            catch (java.lang.Exception e) {
                if (System.currentTimeMillis() - startTime < WAIT_TIMEOUT_MS) {
                    // Do nothing and try again
                    sleep(100);
                } else {
                    throw e;
                }
            }
        }
    }

    private void waitForValue(KVStore store, String key, String desiredValue) throws java.rmi.RemoteException, InterruptedException {
        final long WAIT_TIMEOUT_MS = 10000;
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < WAIT_TIMEOUT_MS) {
            String value = "";
            try {
                value = (String)store.get(key);
            } catch (java.lang.RuntimeException e) {
                if (System.currentTimeMillis() - startTime < WAIT_TIMEOUT_MS) {
                    out.printf("get failed (RuntimeException) %s\n", e.toString());
                    sleep(100) ;// Do nothing and try again
                    continue;
                }
                else {
                    // TODO: remove debug
                    e.printStackTrace();
                    throw e;
                }
            }
            catch (java.lang.Exception e) {
                if (System.currentTimeMillis() - startTime < WAIT_TIMEOUT_MS) {
                    // Do nothing and try again
                    out.printf("get failed (Exception) %s\n", e.toString());
                    sleep(100);
                    continue;
                }
                else {
                    // TODO: remove debug
                    e.printStackTrace();
                    throw e;
                }
            }
            try {
                assertEquals(value, desiredValue);
                out.printf("Matched value %s to desired Value %s\n", value, desiredValue);
            }
            catch (Exception e) {
                if (System.currentTimeMillis() - startTime < WAIT_TIMEOUT_MS) {
                    out.printf("Failed to match value %s to desired Value %s\n", value, desiredValue);
                    sleep(100) ;// Do nothing and try again
                    continue;
                } else {
                    // TODO: remove debug
                    e.printStackTrace();
                    throw e;
                }
            }
        }
    }

    private void runTest(SapphireObjectSpec spec) throws Exception {
        SapphireObjectID sapphireObjId = getOms().createSapphireObject(spec.toString());
        KVStore store = (KVStore) waitForObjectStub(sapphireObjId); // TODO: The fact that we have to wait indicates a bug.  Fix it.
        for (int i = 0; i < 10; i++) {
            String key = "k1_" + i;
            String value = "v1_" + i;
            waitForSet(store, key, value); // On the first one we need to wait for replicas to be created, and elect a leader.
            waitForValue(store, key, value); // TODO: Another bug - we should not have to wait here.  It's supposed to be a consistent store.
            assertEquals(value, store.get(key));
        }
    }

    /**
     * Test sapphire object specifications in <code>src/test/resources/specs</code> directory.
     *
     * @throws Exception
     */
    @Test
    public void testConsensusDM() throws Exception {
        String specFileName = "specs/complex-dm/Consensus.yaml";
        File file = getResourceFile(specFileName);
        SapphireObjectSpec spec = readSapphireSpec(file);
        assertNotNull("SapphireObjectSpec for " + specFileName + " must not be null", spec);
        runTest(spec);
    }
}

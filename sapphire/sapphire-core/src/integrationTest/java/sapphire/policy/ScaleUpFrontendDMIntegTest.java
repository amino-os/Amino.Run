package sapphire.policy;

import static sapphire.kernel.IntegrationTestBase.*;

import java.io.File;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.SapphireObjectID;
import sapphire.demo.KVStore;
import sapphire.kernel.common.KernelObjectMigratingException;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.policy.scalability.LoadBalancedFrontendPolicy;
import sapphire.policy.scalability.ScaleUpException;
import sapphire.policy.scalability.ServerOverLoadException;

public class ScaleUpFrontendDMIntegTest {
    private static final int TASK_COUNT = 50;
    private static final int PARALLEL_THREAD_COUNT = 5;
    private static final String regionName = "IND";
    private static final String[] labels = {regionName};
    OMSServer oms;

    @BeforeClass
    public static void bootstrap() throws Exception {
        startOmsAndKernelServers(regionName, labels);
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
        KVStore store = (KVStore) oms.acquireSapphireObjectStub(sapphireObjId);
        String key = "k";
        String value = "v";
        List<FutureTask<Object>> taskList = new ArrayList<FutureTask<Object>>();
        for (int i = 0; i < TASK_COUNT; i++) {
            int finalI = i;
            FutureTask<Object> task =
                    new FutureTask<Object>(
                            new Callable<Object>() {
                                @Override
                                public Object call() throws Exception {
                                    store.set(key + finalI, value + finalI);
                                    return null;
                                }
                            });
            taskList.add(task);
        }

        // Run tasks in parallel
        ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_THREAD_COUNT);
        for (FutureTask<Object> t : taskList) {
            executor.execute(t);
        }

        // wait for the tasks to complete
        // Due to scaling up, it may throw ServerOverLoadException or KernelObjectMigratingException
        // or ScaleUpException which has been caught here. So, exceptions other than these will be
        // thrown.
        for (int i = 0; i < taskList.size(); i++) {
            try {
                taskList.get(i).get();
            } catch (Exception e) {
                System.out.println("Exception caught in the task " + i);
                List<Class<? extends Exception>> expectedExceptions =
                        Arrays.asList(
                                ServerOverLoadException.class,
                                KernelObjectMigratingException.class,
                                ScaleUpException.class);
                if (expectedExceptions.contains(e.getCause().getCause().getClass())) {
                    System.out.println(e.toString());
                    // Exception caught during first task
                    if (i == 0) {
                        System.out.println("Exception caught during the first task only");
                        break;
                    } else {
                        // Getting values from  KVStore till the task in which
                        // ServerOverLoadException or KernelObjectMigratingException
                        // or ScaleUpException is thrown
                        for (int k = 0; k < i; k++) {
                            int replica_count = LoadBalancedFrontendPolicy.STATIC_REPLICA_COUNT;
                            // There is a possibility that set happens in one server and get from a
                            // different server.So,we are checking for the value in all the
                            // available servers.
                            while (replica_count != 0) {
                                Serializable getValue = store.get(key + k);
                                System.out.println("Value = " + getValue);
                                if (getValue == null) {
                                    replica_count--;
                                    System.out.println(
                                            "Value for key "
                                                    + (key + k)
                                                    + " is not set in this server.Continuing get in the next server");
                                } else {
                                    System.out.println(
                                            "Value for key "
                                                    + (key + k)
                                                    + " is set in this server");
                                    break;
                                }
                            }
                        }
                        break;
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    @Test
    public void testScaleUpFrontendDMs() throws Exception {
        File file = getResourceFile("specs/complex-dm/ScaleUpFrontend.yaml");
        SapphireObjectSpec spec = readSapphireSpec(file);
        runTest(spec);
    }

    @AfterClass
    public static void cleanUp() {
        killOmsAndKernelServers();
    }
}

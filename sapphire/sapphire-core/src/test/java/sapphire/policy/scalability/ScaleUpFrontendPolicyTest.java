package sapphire.policy.scalability;

import static org.junit.Assert.assertEquals;
import static sapphire.common.SapphireUtils.deleteSapphireObject;
import static sapphire.common.UtilsTest.extractFieldValueOnInstance;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import sapphire.app.SO;
import sapphire.app.stubs.SO_Stub;
import sapphire.common.AppObject;
import sapphire.common.BaseTest;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireUtils;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.SapphirePolicy;
import sapphire.runtime.Sapphire;
import sapphire.runtime.SapphireConfiguration;

/** ScaleupFrontend DM test cases */

/** Created by Venugopal Reddy K 00900280 on 16/4/18. */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
    KernelServerImpl.class,
    Sapphire.class,
    KernelObjectFactory.class,
    LocateRegistry.class,
    SapphireUtils.class
})
public class ScaleUpFrontendPolicyTest extends BaseTest {
    @Rule public ExpectedException thrown = ExpectedException.none();

    @ScaleUpFrontendPolicy.ScaleUpFrontendPolicyConfigAnnotation(
            replicationRateInMs = 20,
            loadbalanceConfig =
                    @LoadBalancedFrontendPolicy.LoadBalancedFrontendPolicyConfigAnnotation(
                            maxconcurrentReq = 2,
                            replicacount = 2))
    @SapphireConfiguration(Policies = "sapphire.policy.scalability.ScaleUpFrontendPolicy")
    public static class ScaleUpSO extends SO {}

    public static class Group_Stub extends ScaleUpFrontendPolicy.GroupPolicy
            implements KernelObjectStub {
        sapphire.kernel.common.KernelOID $__oid = null;
        java.net.InetSocketAddress $__hostname = null;
        int $__lastSeenTick = 0;
        AppObject $__appObject = null;
        SapphirePolicy.SapphireClientPolicy $__nextClientPolicy = null;

        public Group_Stub(sapphire.kernel.common.KernelOID oid) {
            this.$__oid = oid;
        }

        public sapphire.kernel.common.KernelOID $__getKernelOID() {
            return this.$__oid;
        }

        public java.net.InetSocketAddress $__getHostname() {
            return this.$__hostname;
        }

        public void $__updateHostname(java.net.InetSocketAddress hostname) {
            this.$__hostname = hostname;
        }

        public int $__getLastSeenTick() {
            return $__lastSeenTick;
        }

        public void $__setLastSeenTick(int lastSeenTick) {
            this.$__lastSeenTick = lastSeenTick;
        }

        public AppObject $__getAppObject() {
            return $__appObject;
        }

        public void $__setNextClientPolicy(SapphirePolicy.SapphireClientPolicy clientPolicy) {
            $__nextClientPolicy = clientPolicy;
        }
    }

    public static class Server_Stub extends ScaleUpFrontendPolicy.ServerPolicy
            implements KernelObjectStub {
        KernelOID $__oid = null;
        InetSocketAddress $__hostname = null;
        int $__lastSeenTick = 0;
        AppObject $__appObject = null;
        SapphirePolicy.SapphireClientPolicy $__nextClientPolicy = null;

        public Server_Stub(KernelOID oid) {
            this.$__oid = oid;
        }

        public KernelOID $__getKernelOID() {
            return $__oid;
        }

        public InetSocketAddress $__getHostname() {
            return $__hostname;
        }

        public void $__updateHostname(InetSocketAddress hostname) {
            this.$__hostname = hostname;
        }

        public int $__getLastSeenTick() {
            return $__lastSeenTick;
        }

        public void $__setLastSeenTick(int lastSeenTick) {
            this.$__lastSeenTick = lastSeenTick;
        }

        public AppObject $__getAppObject() {
            return $__appObject;
        }

        public void $__setNextClientPolicy(SapphirePolicy.SapphireClientPolicy clientPolicy) {
            $__nextClientPolicy = clientPolicy;
        }
    }

    @Before
    public void setUp() throws Exception {
        super.setUp(Server_Stub.class, Group_Stub.class);
        SapphireObjectID sapphireObjId =
                spiedOms.createSapphireObject(
                        "sapphire.policy.scalability.ScaleUpFrontendPolicyTest$ScaleUpSO");
        soStub = (SO_Stub) spiedOms.acquireSapphireObjectStub(sapphireObjId);
        client =
                (DefaultSapphirePolicy.DefaultClientPolicy)
                        extractFieldValueOnInstance(soStub, "$__client");
    }

    @Test
    public void clientTest() throws Exception {
        String methodName = "public java.lang.Integer sapphire.app.SO.getI()";
        ArrayList<Object> params = new ArrayList<Object>();

        AtomicInteger syncCtrCurr =
                (AtomicInteger) extractFieldValueOnInstance(this.client, "replicaListSyncCtr");
        assertEquals(0, syncCtrCurr.get());
        assertEquals(null, extractFieldValueOnInstance(this.client, "replicaList"));

        this.client.onRPC(methodName, params);
        syncCtrCurr =
                (AtomicInteger) extractFieldValueOnInstance(this.client, "replicaListSyncCtr");
        assertEquals(1, syncCtrCurr.get());

        ArrayList<SapphirePolicy.SapphireServerPolicy> replicas =
                (ArrayList<SapphirePolicy.SapphireServerPolicy>)
                        extractFieldValueOnInstance(this.client, "replicaList");
        ArrayList<SapphirePolicy.SapphireServerPolicy> expected = this.group.getServers();
        Assert.assertArrayEquals(replicas.toArray(), expected.toArray());
    }

    @Test
    public void serverScaleUpTest() throws Exception {
        final String methodName = "public java.lang.Integer sapphire.app.SO.getI()";
        final ArrayList<Object> params = new ArrayList<Object>();
        Integer max = (Integer) extractFieldValueOnInstance(this.server1, "maxConcurrentReq");

        List<FutureTask<Object>> taskList = new ArrayList<FutureTask<Object>>();
        for (int i = 0; i < 2 * max + 1; i++) {
            FutureTask<Object> task =
                    new FutureTask<Object>(
                            new Callable<Object>() {
                                @Override
                                public Object call() throws Exception {
                                    Object test = new String("test");
                                    try {
                                        test = client.onRPC(methodName, params);
                                    } catch (ServerOverLoadException e) {
                                    }
                                    return test;
                                }
                            });
            taskList.add(task);
        }

        // Run tasks in parallel
        ExecutorService executor = Executors.newFixedThreadPool(taskList.size());
        for (FutureTask<Object> t : taskList) {
            executor.execute(t);
        }

        for (int i = 0; i < taskList.size(); i++) {
            Object ret = taskList.get(i).get();
        }
    }

    @After
    public void tearDown() throws Exception {
        deleteSapphireObject(spiedOms, group.getSapphireObjId());
    }
}

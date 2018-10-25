package sapphire.policy.scalability;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static sapphire.common.SapphireUtils.deleteSapphireObject;
import static sapphire.common.UtilsTest.extractFieldValueOnInstance;
import static sapphire.common.UtilsTest.setFieldValueOnInstance;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import sapphire.app.*;
import sapphire.app.SO;
import sapphire.app.stubs.SO_Stub;
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

/** Created by Vishwajeet on 2/4/18. */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
    KernelServerImpl.class,
    Sapphire.class,
    KernelObjectFactory.class,
    LocateRegistry.class,
    SapphireUtils.class
})
public class LoadBalancedFrontendPolicyTest extends BaseTest {
    int exceptionExpected = 0;

    public static class LoadBalanceSO extends SO {}

    public static class Group_Stub extends LoadBalancedFrontendPolicy.GroupPolicy
            implements KernelObjectStub {
        sapphire.kernel.common.KernelOID $__oid = null;
        java.net.InetSocketAddress $__hostname = null;
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

        public void $__setNextClientPolicy(SapphirePolicy.SapphireClientPolicy clientPolicy) {
            $__nextClientPolicy = clientPolicy;
        }

        public SapphirePolicy.SapphireClientPolicy $__getNextClientPolicy() {
            return this.$__nextClientPolicy;
        }
    }

    public static class Server_Stub extends LoadBalancedFrontendPolicy.ServerPolicy
            implements KernelObjectStub {
        KernelOID $__oid = null;
        InetSocketAddress $__hostname = null;
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

        public void $__setNextClientPolicy(SapphirePolicy.SapphireClientPolicy clientPolicy) {
            $__nextClientPolicy = clientPolicy;
        }

        public SapphirePolicy.SapphireClientPolicy $__getNextClientPolicy() {
            return this.$__nextClientPolicy;
        }
    }

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {

        LoadBalancedFrontendPolicy.Config config = new LoadBalancedFrontendPolicy.Config();
        config.setMaxConcurrentReq(2);
        config.setReplicaCount(2);

        SapphireObjectSpec spec =
                SapphireObjectSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("sapphire.app.SO")
                        .addDMSpec(
                                DMSpec.newBuilder()
                                        .setName(LoadBalancedFrontendPolicy.class.getName())
                                        .addConfig(config)
                                        .create())
                        .create();
        super.setUp(spec, Server_Stub.class, Group_Stub.class);

        SapphireObjectID sapphireObjId = spiedOms.createSapphireObject(spec.toString());
        soStub = (SO_Stub) spiedOms.acquireSapphireObjectStub(sapphireObjId);
        client =
                (DefaultSapphirePolicy.DefaultClientPolicy)
                        extractFieldValueOnInstance(soStub, "$__client");
    }

    /**
     * Client side DM instance should randomise the order in which it performs round robin against
     * replicas. Here we have instantiated two servers. Anytime the client makes an onRPC call, the
     * first request is randomly assigned to any server. Subsequent onRPC call is always redirected
     * to the other server and so on.
     */
    @Test
    public void testRandomLoadBalance() throws Exception {
        String methodName = "public java.lang.Integer sapphire.app.SO.getIDelayed()";
        ArrayList<Object> params = new ArrayList<Object>();

        this.client.onRPC(methodName, params);
        this.client.onRPC(methodName, params);
        verify((this.server1), times(1)).onRPC(methodName, params);
        verify((this.server2), times(1)).onRPC(methodName, params);
    }

    /**
     * If the number of concurrent requests against a given replica exceeds the
     * MAX_CONCURRENT_REQUESTS, requests to that server replica should fail with a
     * ServerOverLoadException.
     */
    @Test
    public void testMaxConcurrentRequests() throws Exception {
        final String methodName = "public java.lang.Integer sapphire.app.SO.getIDelayed()";
        final ArrayList<Object> params = new ArrayList<Object>();
        Integer max = (Integer) extractFieldValueOnInstance(this.server1, "maxConcurrentReq");

        List<FutureTask<Object>> taskList = new ArrayList<FutureTask<Object>>();
        for (int i = 0; i < 5 * max; i++) {
            FutureTask<Object> task =
                    new FutureTask<Object>(
                            new Callable<Object>() {
                                @Override
                                public Object call() throws Exception {
                                    Object test = null;
                                    try {
                                        test = client.onRPC(methodName, params);
                                    } catch (ServerOverLoadException e) {
                                        exceptionExpected++;
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
        assertNotEquals("Passed", 0, exceptionExpected);
    }

    /**
     * If the created number of replicas is lesser than the configured number of replicas, it throws
     * an error.
     */
    @Test
    public void testStaticReplicaCount() throws Exception {
        LoadBalancedFrontendPolicy.GroupPolicy group1;
        group1 = spy(LoadBalancedFrontendPolicy.GroupPolicy.class);

        // Expecting error message- Configured replicas count: 5, created replica count : 2
        thrown.expectMessage("Configured replicas count: 5, created replica count : 2");
        setFieldValueOnInstance(group1, "replicaCount", 5);
        group1.onCreate("", this.server1, new HashMap<>());
    }

    @After
    public void tearDown() throws Exception {
        deleteSapphireObject(spiedOms, group.getSapphireObjId());
    }
}

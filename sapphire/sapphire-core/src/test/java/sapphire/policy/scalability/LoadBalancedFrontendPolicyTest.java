package sapphire.policy.scalability;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import sapphire.common.AppObject;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.server.KernelObject;
import sapphire.kernel.server.KernelObjectManager;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServerImpl;
import sapphire.policy.SapphirePolicy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static sapphire.common.SapphireUtils.addHost;
import static sapphire.common.SapphireUtils.startSpiedKernelServer;
import static sapphire.common.SapphireUtils.startSpiedOms;
import static sapphire.common.UtilsTest.extractFieldValueOnInstance;
import static sapphire.common.UtilsTest.setFieldValue;


/**
 * Created by Vishwajeet on 2/4/18.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({KernelObjectFactory.class})
public class LoadBalancedFrontendPolicyTest implements Serializable {
    LoadBalancedFrontendPolicy.ClientPolicy client;
    LoadBalancedFrontendPolicy.ServerPolicy server;
    LoadBalancedFrontendPolicy.ServerPolicy server2;
    LoadBalancedFrontendPolicy.GroupPolicy group;
    private AppObject appObject;
    private LoadBalancedFrontendPolicyTest so;
    int exceptionExpected = 0;

    @Override
    public String toString() {
	    /* Overridden toString to just add delay in the app object rpc call */
	    try {
            Thread.sleep(10);
        } catch(InterruptedException e) {

        }
        return "LoadBalancedFrontendPolicyTest";
    }

    public static class Server_Stub extends LoadBalancedFrontendPolicy.ServerPolicy implements KernelObjectStub  {
        KernelOID $__oid = null;
        InetSocketAddress $__hostname = null;
        public Server_Stub(KernelOID oid) {
            this.oid = oid;
            this.$__oid = oid;
        }
        public KernelOID $__getKernelOID() {return $__oid;}
        public InetSocketAddress $__getHostname() {return $__hostname;}
        public void $__updateHostname(InetSocketAddress hostname) {this.$__hostname = hostname;}
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    static Lock sequential = new ReentrantLock();

    @Before
    public void setUp() throws Exception {
    	// create a spied oms instance
        OMSServerImpl spiedOms = startSpiedOms("LoadBalancedFrontendPolicyTest");
        KernelServerImpl.oms = spiedOms;
		// create a spied kernel server instance
        KernelServerImpl spiedKs1 = startSpiedKernelServer(spiedOms, 10001, "IND");
        KernelServerImpl spiedKs2 = startSpiedKernelServer(spiedOms, 10002, "IND");
        // Set this instance of kernel server as local kernel server
        GlobalKernelReferences.nodeServer = spiedKs1;

        this.client = spy(LoadBalancedFrontendPolicy.ClientPolicy.class);
        so = new LoadBalancedFrontendPolicyTest();
        appObject = new AppObject(so);

        setFieldValue(Server_Stub.class, "MAX_CONCURRENT_REQUESTS", 2);

        this.server = (LoadBalancedFrontendPolicy.ServerPolicy) spy(KernelObjectFactory.create(Server_Stub.class.getName()));
        this.server.$__initialize(appObject);
        this.client.setServer(this.server);

        sequential.lock();
        this.group = spy(new LoadBalancedFrontendPolicy.GroupPolicy());
		KernelOID groupOid = spiedOms.registerKernelObject(spiedKs1.getLocalHost());
		this.group.$__setKernelOID(groupOid);
        KernelObjectManager objectManager = (KernelObjectManager) extractFieldValueOnInstance(GlobalKernelReferences.nodeServer, "objectManager");
        assert(objectManager != null);
		objectManager.addObject(groupOid, new KernelObject(this.group));

        this.client.onCreate(this.group);
        this.server.onCreate(this.group);

        // Stub the static factory create method to pass our test stub class name
        KernelObjectStub spiedReplicaServerStub = spy(KernelObjectFactory.create(Server_Stub.class.getName()));
        mockStatic(KernelObjectFactory.class);
        PowerMockito.when(KernelObjectFactory.create(anyString())).thenReturn(spiedReplicaServerStub);

        // Add all the hosts to the kernel client of local kernel server instance
        addHost(spiedKs2);

        this.group.onCreate(this.server);
        sequential.unlock();
        // Update the app objects in all the stubs created
        for (SapphirePolicy.SapphireServerPolicy stub : this.group.getServers()) {
            // Should update this.server2.. and so on based on the number of server stubs created
            if (spiedKs2.getLocalHost().equals(((Server_Stub)stub).$__getHostname())) {
                this.server2 = (LoadBalancedFrontendPolicy.ServerPolicy)stub;
            }
            stub.$__initialize(appObject);
        }
    }

    /**
     * Client side DM instance should randomise the order in which it performs round robin against replicas.
     * Here we have instantiated two servers.
     * Anytime the client makes an onRPC call, the first request is randomly assigned to any server.
     * Subsequent onRPC call is always redirected to the other server and so on.
     */
    @Test
    public void testRandomLoadBalance() throws Exception {
        String methodName = "public java.lang.String java.lang.Object.toString()";
        ArrayList<Object> params = new ArrayList<Object>();

        this.client.onRPC(methodName, params);
        int index = (Integer) extractFieldValueOnInstance(this.client, "index");
        if (0 == index) {
            verify((this.server), times(1)).onRPC(methodName, params);
            this.client.onRPC(methodName, params);
            index = (Integer) extractFieldValueOnInstance(this.client, "index");
            verify((this.server2), times(1)).onRPC(methodName, params);
            assertEquals(index, 1);
        } else {
            verify((this.server2), times(1)).onRPC(methodName, params);
            this.client.onRPC(methodName, params);
            index = (Integer) extractFieldValueOnInstance(this.client, "index");
            verify((this.server), times(1)).onRPC(methodName, params);
            assertEquals(index, 0);
        }
    }

    /**
     * If the number of concurrent requests against a given replica exceeds the MAX_CONCURRENT_REQUESTS, requests to that server
     * replica should fail with a ServerOverLoadException.
     */
    @Test
    public void testMaxConcurrentRequests() throws Exception {
        final String methodName = "public java.lang.String java.lang.Object.toString()";
        final ArrayList<Object> params = new ArrayList<Object>();
        Integer max = (Integer) extractFieldValueOnInstance(this.server, "MAX_CONCURRENT_REQUESTS");

        List<FutureTask<Object>> taskList = new ArrayList<FutureTask<Object>>();
        for (int i = 0; i < 3 * max; i++) {
            FutureTask<Object> task = new FutureTask<Object>(new Callable<Object>() {
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
     * If the created number of replicas is lesser than the configured number of replicas, it throws an error.
     */
    @Test
    public void testStaticReplicaCount() throws Exception {
        LoadBalancedFrontendPolicy.GroupPolicy group1;
        sequential.lock();
        group1 = spy(LoadBalancedFrontendPolicy.GroupPolicy.class);

        PowerMockito.when(KernelObjectFactory.create(anyString())).thenCallRealMethod();
        KernelObjectStub spiedReplicaServerStub = spy(KernelObjectFactory.create(Server_Stub.class.getName()));
        mockStatic(KernelObjectFactory.class);
        PowerMockito.when(KernelObjectFactory.create(anyString())).thenReturn(spiedReplicaServerStub);

        // Expecting error message- Configured replicas count: 5, created replica count : 1
        thrown.expectMessage("Configured replicas count: 5, created replica count : 1");
        setFieldValue(LoadBalancedFrontendPolicy.GroupPolicy.class, "STATIC_REPLICAS", 5);
        group1.onCreate(this.server);
        sequential.unlock();
    }

    @After
    public void tearDown() throws Exception {
        setFieldValue(LoadBalancedFrontendPolicy.GroupPolicy.class,"STATIC_REPLICAS", 2);
    }
}
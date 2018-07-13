package sapphire.policy.scalability;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static sapphire.common.SapphireUtils.addHost;
import static sapphire.common.SapphireUtils.startSpiedKernelServer;
import static sapphire.common.SapphireUtils.startSpiedOms;
import static sapphire.common.UtilsTest.extractFieldValueOnInstance;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.net.InetSocketAddress;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import sapphire.common.AppObject;
import sapphire.common.SapphireUtils;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServerImpl;
import sapphire.policy.SapphirePolicy;

/** Created by Venugopal Reddy K 00900280 on 16/4/18. */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
    KernelServerImpl.class,
    KernelObjectFactory.class,
    LocateRegistry.class,
    SapphireUtils.class
})
public class ScaleUpFrontendPolicyTest {
    ScaleUpFrontendPolicy.ClientPolicy client;
    ScaleUpFrontendPolicy.ServerPolicy server1;
    ScaleUpFrontendPolicy.ServerPolicy server2;
    ScaleUpFrontendPolicy.ServerPolicy server3;
    ScaleUpFrontendPolicy.GroupPolicy group;
    private AppObject appObject;
    private SO_Stub so;

    @Rule public ExpectedException thrown = ExpectedException.none();

    @ScaleUpFrontendPolicy.ScaleUpFrontendPolicyConfigAnnotation(
            replicationRateInMs = 20,
            loadbalanceConfig =
                    @LoadBalancedFrontendPolicy.LoadBalancedFrontendPolicyConfigAnnotation(
                            maxconcurrentReq = 2,
                            replicacount = 2))
    public static class SO implements Serializable {
        public int i = 0;

        public int getCallNumber() {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {

            }
            return ++i;
        }
    }

    public static class SO_Stub extends ScaleUpFrontendPolicyTest.SO {}

    public static class Server_Stub extends ScaleUpFrontendPolicy.ServerPolicy
            implements KernelObjectStub {
        KernelOID $__oid = null;
        InetSocketAddress $__hostname = null;

        public Server_Stub(KernelOID oid) {
            this.oid = oid;
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
    }

    @Before
    public void setUp() throws Exception {

        Registry test =
                new Registry() {
                    @Override
                    public Remote lookup(String s)
                            throws RemoteException, NotBoundException, AccessException {
                        return null;
                    }

                    @Override
                    public void bind(String s, Remote remote)
                            throws RemoteException, AlreadyBoundException, AccessException {}

                    @Override
                    public void unbind(String s)
                            throws RemoteException, NotBoundException, AccessException {}

                    @Override
                    public void rebind(String s, Remote remote)
                            throws RemoteException, AccessException {}

                    @Override
                    public String[] list() throws RemoteException, AccessException {
                        return new String[0];
                    }
                };

        PowerMockito.mockStatic(LocateRegistry.class);
        when(LocateRegistry.getRegistry(anyString(), anyInt())).thenReturn(test);

        // create a spied oms instance
        OMSServerImpl spiedOms = startSpiedOms("ScaleUpFrontendPolicyTest");
        KernelServerImpl.oms = spiedOms;
        // create a spied kernel server instance
        final KernelServerImpl spiedKs1 = startSpiedKernelServer(spiedOms, 10001, "IND");
        final KernelServerImpl spiedKs2 = startSpiedKernelServer(spiedOms, 10002, "IND");
        final KernelServerImpl spiedKs3 = startSpiedKernelServer(spiedOms, 10003, "IND");

        // Set this instance of kernel server as local kernel server
        GlobalKernelReferences.nodeServer = spiedKs1;

        this.client = spy(ScaleUpFrontendPolicy.ClientPolicy.class);
        so = new SO_Stub();
        appObject = new AppObject(so);

        // Create the spied server policy
        this.server1 =
                (ScaleUpFrontendPolicy.ServerPolicy)
                        PowerMockito.spy(KernelObjectFactory.create(Server_Stub.class.getName()));
        this.server1.$__initialize(appObject);

        this.group = spy(new ScaleUpFrontendPolicy.GroupPolicy());

        this.client.onCreate(this.group, new Annotation[] {});
        this.server1.onCreate(this.group, SO.class.getAnnotations());

        // Stub the static factory create method to pass our test stub class name
        final KernelObjectStub spiedReplicaServerStub2 =
                PowerMockito.spy(KernelObjectFactory.create(Server_Stub.class.getName()));
        final KernelObjectStub spiedReplicaServerStub3 =
                PowerMockito.spy(KernelObjectFactory.create(Server_Stub.class.getName()));
        mockStatic(KernelObjectFactory.class);
        PowerMockito.when(KernelObjectFactory.create(anyString()))
                .thenAnswer(
                        new Answer<KernelObjectStub>() {
                            int i = 0;

                            @Override
                            public KernelObjectStub answer(InvocationOnMock invocation)
                                    throws Throwable {
                                KernelObjectStub stub = null;
                                ++i;
                                if (1 == i) {
                                    stub = spiedReplicaServerStub2;

                                } else if (2 == i) {
                                    stub = spiedReplicaServerStub3;
                                }

                                ((SapphirePolicy.SapphireServerPolicy) stub)
                                        .$__initialize(appObject);
                                ((SapphirePolicy.SapphireServerPolicy) stub)
                                        .onCreate(group, SO.class.getAnnotations());
                                return stub;
                            }
                        });

        // Add all the hosts to the kernel client of local kernel server instance
        addHost(spiedKs2);
        addHost(spiedKs3);

        this.group.onCreate(this.server1, SO.class.getAnnotations());

        doNothing()
                .when((ScaleUpFrontendPolicy.ServerPolicy) spiedReplicaServerStub2)
                .sapphire_deleteKernelObject();
        doNothing()
                .when((ScaleUpFrontendPolicy.ServerPolicy) spiedReplicaServerStub3)
                .sapphire_deleteKernelObject();
    }

    @Test
    public void clientTest() throws Exception {
        String methodName =
                "public int sapphire.policy.scalability.ScaleUpFrontendPolicyTest$SO.getCallNumber()";
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
        final String methodName =
                "public int sapphire.policy.scalability.ScaleUpFrontendPolicyTest$SO.getCallNumber()";
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
}

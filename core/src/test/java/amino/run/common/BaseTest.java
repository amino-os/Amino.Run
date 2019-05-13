package amino.run.common;

import static amino.run.common.TestUtils.startSpiedKernelServer;
import static amino.run.common.TestUtils.startSpiedOms;
import static amino.run.common.UtilsTest.extractFieldValueOnInstance;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import amino.run.app.MicroServiceSpec;
import amino.run.app.Registry;
import amino.run.kernel.client.KernelClient;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.KernelObjectFactory;
import amino.run.kernel.common.KernelObjectStub;
import amino.run.kernel.server.KernelObject;
import amino.run.kernel.server.KernelObjectManager;
import amino.run.kernel.server.KernelServer;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.oms.KernelServerManager;
import amino.run.oms.OMSServer;
import amino.run.oms.OMSServerImpl;
import amino.run.policy.DefaultPolicy;
import amino.run.policy.Policy;
import amino.run.runtime.MicroService;
import amino.run.sampleSO.SO;
import java.net.InetSocketAddress;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;

/**
 * This is the base test class with a setup to create a spied instance of oms, 3 kernel servers. And
 * has the necessary mocking of few methods in creating/deleting/replicating microservice flow.
 * TestCase files can inherit this class and make their setup simple.
 */

/** Created by Venugopal Reddy K on 12/9/18. */
@PrepareForTest({
    KernelServerImpl.class,
    KernelClient.class,
    MicroService.class,
    KernelObjectFactory.class,
    KernelServerManager.class,
    LocateRegistry.class,
    TestUtils.class,
    OMSServerImpl.class,
    Utils.ObjectCloner.class
})
public class BaseTest {
    protected DefaultPolicy.DefaultClientPolicy client;
    protected DefaultPolicy.DefaultServerPolicy server1;
    protected DefaultPolicy.DefaultServerPolicy server2;
    protected DefaultPolicy.DefaultServerPolicy server3;
    protected DefaultPolicy.DefaultGroupPolicy group;
    protected SO soStub; // client side stub
    protected OMSServer spiedOms;
    protected Registry registry;
    protected KernelServer spiedksOnOms;
    protected KernelServer spiedKs1;
    protected KernelServer spiedKs2;
    protected KernelServer spiedKs3;
    protected int kernelServerCount = 0;
    protected String LOCAL_HOST = "localhost";
    protected String LOOP_BACK_IP_ADDR = "127.0.0.1";
    protected int omsPort = 10000;
    protected int kernelPort1 = 10001;
    protected int kernelPort2 = 10002;
    protected int kernelPort3 = 10003;

    protected boolean serversInSameRegion = true;

    public java.rmi.registry.Registry getNewRegistry(final int port) {
        return new java.rmi.registry.Registry() {
            @Override
            public Remote lookup(String s)
                    throws RemoteException, NotBoundException, AccessException {
                if (s.equals("io.amino.run.oms")) {
                    return spiedOms;
                } else if (s.equals("io.amino.run.kernelserver")) {
                    KernelServer localServer = GlobalKernelReferences.nodeServer;
                    if (port == omsPort) {
                        return spiedksOnOms;
                    } else if (port == kernelPort1) {
                        return spiedKs1 != null ? spiedKs1 : (spiedKs1 = spy(localServer));
                    } else if (port == kernelPort2) {
                        return spiedKs2 != null ? spiedKs2 : (spiedKs2 = spy(localServer));
                    } else if (port == kernelPort3) {
                        return spiedKs3 != null ? spiedKs3 : (spiedKs3 = spy(localServer));
                    }
                }

                return null;
            }

            @Override
            public void bind(String s, Remote remote)
                    throws RemoteException, AlreadyBoundException, AccessException {}

            @Override
            public void unbind(String s)
                    throws RemoteException, NotBoundException, AccessException {}

            @Override
            public void rebind(String s, Remote remote) throws RemoteException, AccessException {}

            @Override
            public String[] list() throws RemoteException, AccessException {
                return new String[0];
            }
        };
    }

    public void setUp(int serverCount, MicroServiceSpec spec) throws Exception {
        mockStatic(LocateRegistry.class);
        java.rmi.registry.Registry omsRegistry = getNewRegistry(omsPort);
        java.rmi.registry.Registry ks1Registry = getNewRegistry(kernelPort1);
        java.rmi.registry.Registry ks2Registry = getNewRegistry(kernelPort2);
        java.rmi.registry.Registry ks3Registry = getNewRegistry(kernelPort3);
        when(LocateRegistry.createRegistry(omsPort)).thenReturn(omsRegistry);
        when(LocateRegistry.createRegistry(kernelPort1)).thenReturn(ks1Registry);
        when(LocateRegistry.createRegistry(kernelPort2)).thenReturn(ks2Registry);
        when(LocateRegistry.createRegistry(kernelPort3)).thenReturn(ks3Registry);
        when(LocateRegistry.getRegistry(LOCAL_HOST, omsPort)).thenReturn(omsRegistry);
        when(LocateRegistry.getRegistry(LOCAL_HOST, kernelPort1)).thenReturn(ks1Registry);
        when(LocateRegistry.getRegistry(LOCAL_HOST, kernelPort2)).thenReturn(ks2Registry);
        when(LocateRegistry.getRegistry(LOCAL_HOST, kernelPort3)).thenReturn(ks3Registry);

        // create a spied oms instance
        final OMSServerImpl spiedOms = startSpiedOms(LOOP_BACK_IP_ADDR, omsPort);
        spiedksOnOms = spy(GlobalKernelReferences.nodeServer);
        KernelServerImpl.oms = spiedOms;
        this.spiedOms = spiedOms;
        registry = spiedOms;

        String[] regions;
        if (serversInSameRegion) {
            regions = new String[] {"IND", "IND", "IND"};
        } else {
            regions = new String[] {"IND", "CHN", "USA"};
        }

        assert (serverCount <= 3);
        kernelServerCount = serverCount;

        /* create configured number of spied kernel server instances. And populate the servers map in kernel clients of
        respective kernel servers so that each kernel server will be able to communicate with other servers without
        registry.lookup.
        */
        startSpiedKernelServer(
                LOOP_BACK_IP_ADDR, kernelPort1, LOOP_BACK_IP_ADDR, omsPort, regions[0]);
        if (serverCount > 1) {
            startSpiedKernelServer(
                    LOOP_BACK_IP_ADDR, kernelPort2, LOOP_BACK_IP_ADDR, omsPort, regions[1]);
        }
        if (serverCount > 2) {
            startSpiedKernelServer(
                    LOOP_BACK_IP_ADDR, kernelPort3, LOOP_BACK_IP_ADDR, omsPort, regions[2]);
        }

        GlobalKernelReferences.nodeServer = (KernelServerImpl) spiedKs1;

        // Stub static kernel object factory methods
        mockStatic(
                KernelObjectFactory.class,
                new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        if (!invocation.getMethod().getName().equals("delete")) {
                            return invocation.callRealMethod();
                        }

                        KernelOID oid = (KernelOID) invocation.getArguments()[0];
                        InetSocketAddress host = spiedOms.lookupKernelObject(oid);

                        KernelServer ks = null;
                        if (host.toString().contains(String.valueOf(omsPort))) {
                            ks = spiedksOnOms;
                        } else if (host.toString().contains(String.valueOf(kernelPort1))) {
                            ks = spiedKs1;
                        } else if (host.toString().contains(String.valueOf(kernelPort2))) {
                            ks = spiedKs2;
                        } else if (host.toString().contains(String.valueOf(kernelPort3))) {
                            ks = spiedKs3;
                        }

                        KernelServerImpl temp = GlobalKernelReferences.nodeServer;
                        GlobalKernelReferences.nodeServer = (KernelServerImpl) ks;
                        Object ret = invocation.callRealMethod();
                        GlobalKernelReferences.nodeServer = temp;
                        return ret;
                    }
                });

        // Stub static methods
        mockStatic(
                MicroService.class,
                new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        if ((invocation.getMethod().getName().equals("createGroupPolicy"))) {
                            /* Group policy objects are created on kernel server within OMS */
                            KernelServerImpl temp = GlobalKernelReferences.nodeServer;
                            GlobalKernelReferences.nodeServer = (KernelServerImpl) spiedksOnOms;
                            Object ret = invocation.callRealMethod();
                            GlobalKernelReferences.nodeServer = temp;
                            return ret;
                        }
                        return invocation.callRealMethod();
                    }
                });

        // TODO: SO is created here which tests the whole chain every time setUp is run. Is there a
        // need for it? (i.e., can we remove this part as this will be duplicate with tests that use
        // setUp?
        MicroServiceID microServiceId = registry.create(spec.toString());

        soStub = (SO) registry.acquireStub(microServiceId);
        client =
                (DefaultPolicy.DefaultClientPolicy)
                        extractFieldValueOnInstance(soStub, "$__client");
        getServerAndGroupPolicyObjects();
    }

    private KernelServer getKernelServerFromPolicyStub(KernelObjectStub stub) {
        KernelServer ks = null;
        if (stub.$__getHostname().toString().contains(String.valueOf(kernelPort1))) {
            ks = spiedKs1;
        } else if (stub.$__getHostname().toString().contains(String.valueOf(kernelPort2))) {
            ks = spiedKs2;
        } else if (stub.$__getHostname().toString().contains(String.valueOf(kernelPort3))) {
            ks = spiedKs3;
        }

        return ks;
    }

    private void getServerPolicyObjects(KernelServer kernelServer, KernelObject obj) {
        KernelServerImpl ks = (KernelServerImpl) kernelServer;
        if (ks.getLocalHost().toString().contains(String.valueOf(kernelPort1))) {
            server1 = (DefaultPolicy.DefaultServerPolicy) obj.getObject();
        } else if (ks.getLocalHost().toString().contains(String.valueOf(kernelPort2))) {
            server2 = (DefaultPolicy.DefaultServerPolicy) obj.getObject();
        } else if (ks.getLocalHost().toString().contains(String.valueOf(kernelPort3))) {
            server3 = (DefaultPolicy.DefaultServerPolicy) obj.getObject();
        }
    }

    private void getServerAndGroupPolicyObjects() throws Exception {
        ArrayList<Policy.ServerPolicy> servers = client.getGroup().getServers();
        KernelServer ks = spiedksOnOms;
        KernelObjectManager objMgr =
                (KernelObjectManager) extractFieldValueOnInstance(ks, "objectManager");
        KernelObject obj =
                objMgr.lookupObject(((KernelObjectStub) client.getGroup()).$__getKernelOID());
        group = (DefaultPolicy.DefaultGroupPolicy) obj.getObject();
        for (Policy.ServerPolicy server : servers) {
            ks = getKernelServerFromPolicyStub((KernelObjectStub) server);
            objMgr = (KernelObjectManager) extractFieldValueOnInstance(ks, "objectManager");
            obj = objMgr.lookupObject(((KernelObjectStub) server).$__getKernelOID());
            getServerPolicyObjects(ks, obj);
        }
    }

    public void tearDown() throws Exception {
        registry.delete(group.getMicroServiceId());
    }
}

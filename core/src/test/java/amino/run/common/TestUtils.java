package amino.run.common;

import static amino.run.common.UtilsTest.extractFieldValueOnInstance;
import static amino.run.common.UtilsTest.setFieldValueOnInstance;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.server.KernelServer;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.oms.InstanceManager;
import amino.run.oms.KernelServerManager;
import amino.run.oms.MicroServiceManager;
import amino.run.oms.OMSServer;
import amino.run.oms.OMSServerImpl;
import amino.run.policy.util.ResettableTimer;
import java.net.InetSocketAddress;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/** Created by Vishwajeet on 4/4/18. */
public class TestUtils {

    public static Registry dummyRegistry =
            new Registry() {
                @Override
                public Remote lookup(String s)
                        throws RemoteException, NotBoundException, AccessException {
                    if (s.equals("io.amino.run.oms")) {
                        return KernelServerImpl.oms;
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
                public void rebind(String s, Remote remote)
                        throws RemoteException, AccessException {}

                @Override
                public String[] list() throws RemoteException, AccessException {
                    return new String[0];
                }
            };

    public static OMSServerImpl startSpiedOms(String ipAddr, int omsPort) throws Exception {
        mockStatic(LocateRegistry.class);
        when(LocateRegistry.createRegistry(Matchers.anyInt())).thenReturn(dummyRegistry);
        mockStatic(
                UnicastRemoteObject.class,
                new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        if (invocation.getMethod().getName().equals("exportObject")) {
                            /* return the object to be exported as it is. */
                            return invocation.getArguments()[0];
                        }
                        return invocation.callRealMethod();
                    }
                });

        OMSServerImpl.main(
                new String[] {
                    OMSServerImpl.OMS_IP_OPT, ipAddr, OMSServerImpl.OMS_PORT_OPT, "" + omsPort
                });

        /* Get the oms instance created above from the local kernel server's static oms field */
        OMSServerImpl myOms = (OMSServerImpl) KernelServerImpl.oms;

        /* If needed, all the fields inside OMSServer can be spied as shown below in commented code */
        /*KernelServerManager serverManager = (KernelServerManager)extractFieldValueOnInstance(myOms, "serverManager");
        KernelServerManager spiedServerManager = spy(serverManager);
        setFieldValueOnInstance(myOms, "serverManager", spiedServerManager);*/

        OMSServerImpl spiedOms = spy(myOms);

        return spiedOms;
    }

    public static KernelServerImpl startSpiedKernelServer(
            String ipAddr, int port, String omsIpaddr, int omsPort, String region)
            throws Exception {
        mockStatic(LocateRegistry.class);
        when(LocateRegistry.createRegistry(Matchers.anyInt())).thenReturn(dummyRegistry);
        when(LocateRegistry.getRegistry(Matchers.anyString(), Matchers.anyInt()))
                .thenReturn(dummyRegistry);
        mockStatic(
                UnicastRemoteObject.class,
                new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        if (invocation.getMethod().getName().equals("exportObject")) {
                            /* return the object to be exported as it is. */
                            return invocation.getArguments()[0];
                        }
                        return invocation.callRealMethod();
                    }
                });

        String labels = KernelServerImpl.REGION_KEY + "=" + region;
        KernelServerImpl.main(
                new String[] {
                    KernelServerImpl.KERNEL_SERVER_IP_OPT,
                    ipAddr,
                    KernelServerImpl.KERNEL_SERVER_PORT_OPT,
                    "" + port,
                    OMSServerImpl.OMS_IP_OPT,
                    omsIpaddr,
                    OMSServerImpl.OMS_PORT_OPT,
                    "" + omsPort,
                    KernelServerImpl.LABEL_OPT,
                    labels
                });

        /* Get the kernel server instance created above from the GlobalKernelReferences nodeServer field */
        KernelServerImpl ks = GlobalKernelReferences.nodeServer;

        /* Stop heartbeat between kernel server and oms */
        ResettableTimer heartbeatTimer =
                (ResettableTimer) extractFieldValueOnInstance(ks, "ksHeartbeatSendTimer");
        heartbeatTimer.cancel();

        KernelServerManager kernelServerManager =
                (KernelServerManager)
                        extractFieldValueOnInstance(KernelServerImpl.oms, "serverManager");
        Map<InetSocketAddress, KernelServerManager.KernelServerInfo> servers =
                (Map<InetSocketAddress, KernelServerManager.KernelServerInfo>)
                        extractFieldValueOnInstance(kernelServerManager, "servers");
        KernelServerManager.KernelServerInfo kernelServerInfo = servers.get(ks.getLocalHost());

        ResettableTimer heartBeatTimer =
                (ResettableTimer) extractFieldValueOnInstance(kernelServerInfo, "heartBeatTimer");
        heartBeatTimer.cancel();

        /* If needed, all the fields inside KernelServer can be spied as shown below in commented code */
        /*KernelClient kernelClient = (KernelClient) extractFieldValueOnInstance(ks, "client");
        KernelClient spiedKernelClient = spy(kernelClient);
        setFieldValueOnInstance(ks, "client", spiedKernelClient);*/

        KernelServerImpl spiedKs = spy(GlobalKernelReferences.nodeServer);

        /* Populate the server map on OMS such that registry.lookup never happens on OMS to communicate with this
        kernel server */
        addHostOnOmsKernelServerManager(KernelServerImpl.oms, spiedKs);
        return spiedKs;
    }

    /* Populate the server map on kernel clients such that registry.lookup never happens for communication between
    this pair of kernel server */
    public static void addHostToKernelClient(KernelServer server1, KernelServer server2)
            throws Exception {
        /* Update servers map on server1's kernel client such that server1 can invoke method on server2
        [server1 -> server2] */
        Hashtable<InetSocketAddress, KernelServer> servers =
                (Hashtable<InetSocketAddress, KernelServer>)
                        extractFieldValueOnInstance(
                                ((KernelServerImpl) server1).getKernelClient(), "servers");
        servers.put(((KernelServerImpl) server2).getLocalHost(), server2);

        /* Update servers map on server2's kernel client such that server2 can invoke method on server1
        [server2 -> server1] */
        servers =
                (Hashtable<InetSocketAddress, KernelServer>)
                        extractFieldValueOnInstance(
                                ((KernelServerImpl) server2).getKernelClient(), "servers");
        servers.put(((KernelServerImpl) server1).getLocalHost(), server1);
    }

    public static void addHostOnOmsKernelServerManager(OMSServer oms, KernelServer ks)
            throws Exception {
        KernelServerManager kernelServerManager =
                (KernelServerManager) extractFieldValueOnInstance(oms, "serverManager");
        ConcurrentHashMap<InetSocketAddress, KernelServerManager.KernelServerInfo> servers =
                (ConcurrentHashMap<InetSocketAddress, KernelServerManager.KernelServerInfo>)
                        extractFieldValueOnInstance(kernelServerManager, "servers");
        KernelServerManager.KernelServerInfo kernelServerInfo =
                servers.get(((KernelServerImpl) ks).getLocalHost());
        setFieldValueOnInstance(kernelServerInfo, "remoteRef", ks);
    }

    public static InstanceManager getOmsMicroService(OMSServer oms, MicroServiceID microServiceId)
            throws Exception {
        MicroServiceManager mgr =
                (MicroServiceManager) extractFieldValueOnInstance(oms, "objectManager");
        ConcurrentHashMap<MicroServiceID, InstanceManager> microServices =
                (ConcurrentHashMap<MicroServiceID, InstanceManager>)
                        extractFieldValueOnInstance(mgr, "microServices");
        return microServices.get(microServiceId);
    }
}

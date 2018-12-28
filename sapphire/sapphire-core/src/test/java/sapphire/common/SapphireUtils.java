package sapphire.common;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static sapphire.common.UtilsTest.extractFieldValueOnInstance;

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
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.KernelServerManager;
import sapphire.oms.OMSServer;
import sapphire.oms.OMSServerImpl;
import sapphire.oms.SapphireInstanceManager;
import sapphire.oms.SapphireObjectManager;
import sapphire.policy.util.ResettableTimer;

/** Created by Vishwajeet on 4/4/18. */
public class SapphireUtils {
    public static Registry dummyRegistry =
            new Registry() {
                @Override
                public Remote lookup(String s)
                        throws RemoteException, NotBoundException, AccessException {
                    if (s.equals("SapphireOMS")) {
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

        OMSServerImpl.main(new String[] {ipAddr, "" + omsPort});

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

        KernelServerImpl.main(
                new String[] {ipAddr, "" + port, omsIpaddr, "" + omsPort, " " + region});

        /* Get the kernel server instance created above from the GlobalKernelReferences nodeServer field */
        KernelServerImpl ks = GlobalKernelReferences.nodeServer;

        /* Stop heartbeat between kernel server and oms */
        ResettableTimer heartbeatTimer =
                (ResettableTimer) extractFieldValueOnInstance(ks, "ksHeartbeatSendTimer");
        heartbeatTimer.cancel();

        KernelServerManager kernelServerManager =
                (KernelServerManager)
                        extractFieldValueOnInstance(KernelServerImpl.oms, "serverManager");
        Map<InetSocketAddress, ResettableTimer> heartbeatTimers =
                (Map<InetSocketAddress, ResettableTimer>)
                        extractFieldValueOnInstance(kernelServerManager, "ksHeartBeatTimers");
        ResettableTimer ksHeartBeatTimer = heartbeatTimers.get(ks.getLocalHost());
        ksHeartBeatTimer.cancel();

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

    public static void addHost(KernelServer ks) throws Exception {
        Hashtable<InetSocketAddress, KernelServer> servers =
                (Hashtable<InetSocketAddress, KernelServer>)
                        extractFieldValueOnInstance(
                                GlobalKernelReferences.nodeServer.getKernelClient(), "servers");
        servers.put(((KernelServerImpl) ks).getLocalHost(), ks);
    }

    public static void addHostOnOmsKernelServerManager(OMSServer oms, KernelServer ks)
            throws Exception {
        KernelServerManager kernelServerManager =
                (KernelServerManager) extractFieldValueOnInstance(oms, "serverManager");
        ConcurrentHashMap<InetSocketAddress, KernelServer> servers =
                (ConcurrentHashMap<InetSocketAddress, KernelServer>)
                        extractFieldValueOnInstance(kernelServerManager, "servers");
        servers.put(((KernelServerImpl) ks).getLocalHost(), ks);
    }

    public static SapphireInstanceManager getOmsSapphireInstance(
            OMSServer oms, SapphireObjectID sapphireObjId) throws Exception {
        SapphireObjectManager objMgr =
                (SapphireObjectManager) extractFieldValueOnInstance(oms, "objectManager");
        ConcurrentHashMap<SapphireObjectID, SapphireInstanceManager> sapphireObjects =
                (ConcurrentHashMap<SapphireObjectID, SapphireInstanceManager>)
                        extractFieldValueOnInstance(objMgr, "sapphireObjects");
        return sapphireObjects.get(sapphireObjId);
    }
}

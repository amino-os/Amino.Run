package sapphire.common;

import static org.mockito.Mockito.spy;
import static sapphire.common.UtilsTest.extractFieldValueOnInstance;
import static sapphire.compiler.GlobalStubConstants.POLICY_ONDESTROY_MTD_NAME_FORMAT;

import java.net.InetSocketAddress;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.ServerInfo;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.KernelServerManager;
import sapphire.oms.OMSServer;
import sapphire.oms.OMSServerImpl;
import sapphire.oms.SapphireInstanceManager;
import sapphire.oms.SapphireObjectManager;
import sapphire.runtime.EventHandler;

/** Created by Vishwajeet on 4/4/18. */
public class SapphireUtils {
    public static int omsPort = 10000;
    public static String LOOP_BACK_IP_ADDR = "127.0.0.1";

    public static Registry dummyRegistry =
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

    public static OMSServerImpl startSpiedOms() throws Exception {
        OMSServerImpl myOms = new OMSServerImpl();

        /* If needed, all the fields inside OMSServer can be spied as shown below in commented code */
        /*KernelServerManager serverManager = (KernelServerManager)extractFieldValueOnInstance(myOms, "serverManager");
        KernelServerManager spiedServerManager = spy(serverManager);
        setFieldValueOnInstance(myOms, "serverManager", spiedServerManager);*/

        OMSServerImpl spiedOms = spy(myOms);

        return spiedOms;
    }

    public static KernelServerImpl startSpiedKernelServer(
            OMSServerImpl spiedOms, int port, String region) throws Exception {
        KernelServerImpl ks =
                new KernelServerImpl(
                        new InetSocketAddress(LOOP_BACK_IP_ADDR, port),
                        new InetSocketAddress(LOOP_BACK_IP_ADDR, omsPort));
        ks.setRegion(region);
        KernelServerImpl.oms = spiedOms;

        /* If needed, all the fields inside KernelServer can be spied as shown below in commented code */
        /*KernelClient kernelClient = (KernelClient) extractFieldValueOnInstance(ks, "client");
        KernelClient spiedKernelClient = spy(kernelClient);
        setFieldValueOnInstance(ks, "client", spiedKernelClient);*/

        KernelServerImpl spiedKs = spy(ks);
        spiedOms.registerKernelServer(new ServerInfo(spiedKs.getLocalHost(), spiedKs.getRegion()));

        addHostOnOmsKernelServerManager(spiedOms, spiedKs);
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

    public static KernelServer getHostOnOmsKernelServerManager(
            OMSServer oms, InetSocketAddress host) throws Exception {
        KernelServerManager kernelServerManager =
                (KernelServerManager) extractFieldValueOnInstance(oms, "serverManager");
        ConcurrentHashMap<InetSocketAddress, KernelServer> servers =
                (ConcurrentHashMap<InetSocketAddress, KernelServer>)
                        extractFieldValueOnInstance(kernelServerManager, "servers");
        return servers.get(host);
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

    public static void deleteSapphireObject(OMSServer oms, SapphireObjectID sapphireObjId)
            throws Exception {
        EventHandler handler = oms.getSapphireObjectDispatcher(sapphireObjId);
        /* Handlers are made from mocked objects(server & group). Mocked objects will have all the
         * final methods. Need to remove the final keyword from onDestroy() */
        // "public final void
        // sapphire.policy.scalability.ScaleUpFrontendPolicyTest$Group_Stub$$EnhancerByMockitoWithCGLIB$$6faa2a3e.onDestroy() throws java.rmi.RemoteException"
        StringBuffer destroyName =
                new StringBuffer(
                        String.format(
                                POLICY_ONDESTROY_MTD_NAME_FORMAT,
                                handler.getObjects().get(0).getClass().getName()));
        String newDestroyName = destroyName.toString();
        destroyName.insert("public ".length(), "final ");
        Hashtable<String, Object> handlers =
                (Hashtable<String, Object>) extractFieldValueOnInstance(handler, "handlers");
        Object policyHandler = handlers.get(destroyName.toString());
        handlers.put(newDestroyName, policyHandler);
        oms.deleteSapphireObject(sapphireObjId);
    }
}

package sapphire.oms;

import static sapphire.runtime.Sapphire.getPolicyStub;
import static sapphire.runtime.Sapphire.initializeGroupPolicy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;
import org.json.JSONException;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireReplicaID;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectNotCreatedException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelServerNotFoundException;
import sapphire.kernel.common.ServerInfo;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.policy.SapphirePolicy.SapphireGroupPolicy;
import sapphire.runtime.EventHandler;

/**
 * OMSServer for tracking objects in Sapphire
 *
 * @author iyzhang
 */
public class OMSServerImpl implements OMSServer {
    private static Logger logger = Logger.getLogger("sapphire.oms.OMSServerImpl");

    private GlobalKernelObjectManager kernelObjectManager;
    private AppObjectStub appEntryPoint = null;
    private String appEntryClassName;
    private KernelServerManager serverManager;
    private SapphireObjectManager objectManager;

    /** CONSTRUCTOR * */
    // TODO Should receive a List of servers
    public OMSServerImpl(String appEntryClassName)
            throws IOException, NotBoundException, JSONException {
        kernelObjectManager = new GlobalKernelObjectManager();
        serverManager = new KernelServerManager();
        objectManager = new SapphireObjectManager();
        this.appEntryClassName = appEntryClassName;
    }

    /** KERNEL METHODS * */
    /**
     * Register new kernel object
     *
     * @return a new unique kernel object ID
     */
    public KernelOID registerKernelObject(InetSocketAddress host) throws RemoteException {
        KernelOID oid = kernelObjectManager.register(host);
        return oid;
    }

    /** Register a new host for this kernel object. Used to move a kernel object */
    public void registerKernelObject(KernelOID oid, InetSocketAddress host)
            throws RemoteException, KernelObjectNotFoundException {
        logger.info("Registering new host for " + oid.toString() + " on " + host.toString());
        kernelObjectManager.register(oid, host);
    }

    /**
     * UnRegister specified kernel object from the host
     *
     * @param oid
     * @param host
     * @throws RemoteException
     * @throws KernelObjectNotFoundException
     */
    public void unRegisterKernelObject(KernelOID oid, InetSocketAddress host)
            throws RemoteException, KernelObjectNotFoundException {
        logger.info("UnRegistering " + oid.toString() + " on host " + host.toString());
        kernelObjectManager.unRegister(oid, host);
    }

    /**
     * Find the host for a kernel object
     *
     * @return the host IP address
     */
    public InetSocketAddress lookupKernelObject(KernelOID oid)
            throws RemoteException, KernelObjectNotFoundException {
        logger.info(
                "Found host for " + oid.toString() + " host: " + kernelObjectManager.lookup(oid));
        return kernelObjectManager.lookup(oid);
    }

    @Override
    public void registerKernelServer(ServerInfo info) throws RemoteException, NotBoundException {
        serverManager.registerKernelServer(info);
    }

    @Override
    public void heartbeatKernelServer(ServerInfo srvinfo)
            throws RemoteException, NotBoundException, KernelServerNotFoundException {
        serverManager.heartbeatKernelServer(srvinfo);
    }

    /**
     * Gets the list servers in the system
     *
     * @throws RemoteException
     * @throws NumberFormatException
     * @throws NotBoundException
     */
    @Override
    public ArrayList<InetSocketAddress> getServers()
            throws NumberFormatException, RemoteException, NotBoundException {
        return serverManager.getServers();
    }

    /**
     * Gets the regions in the system
     *
     * @throws RemoteException
     */
    @Override
    public ArrayList<String> getRegions() throws RemoteException {
        // TODO Auto-generated method stub
        return serverManager.getRegions();
    }

    /**
     * Gets a server in the specified region
     *
     * @param region
     * @return
     * @throws RemoteException
     */
    @Override
    public InetSocketAddress getServerInRegion(String region) throws RemoteException {
        return serverManager.getServerInRegion(region);
    }

    /**
     * Gets all servers in the specified region
     *
     * @param region
     * @return
     * @throws RemoteException
     */
    @Override
    public ArrayList<InetSocketAddress> getServersInRegion(String region) throws RemoteException {
        return serverManager.getServersInRegion(region);
    }
    /** APP METHODS * */

    /**
     * Starts the app on one of the servers and returns the App Object Stub
     *
     * @throws RemoteException
     */
    @Override
    public AppObjectStub getAppEntryPoint() throws RemoteException {
        if (appEntryPoint != null) {
            return appEntryPoint;
        } else {
            InetSocketAddress host =
                    serverManager.getServerInRegion(serverManager.getRegions().get(0));
            KernelServer server = serverManager.getServer(host);
            appEntryPoint = server.startApp(appEntryClassName);
            return appEntryPoint;
        }
    }

    /**
     * Creates the group policy instance on the kernel server running within OMS and returns the
     * group policy Object Stub
     *
     * @throws RemoteException
     * @throws KernelObjectNotCreatedException
     * @throws ClassNotFoundException
     */
    @Override
    public SapphireGroupPolicy createGroupPolicy(Class<?> policyClass)
            throws RemoteException, KernelObjectNotCreatedException, ClassNotFoundException {
        SapphireGroupPolicy groupStub = (SapphireGroupPolicy) getPolicyStub(policyClass);
        try {
            initializeGroupPolicy(groupStub);
        } catch (KernelObjectNotFoundException e) {
            logger.severe(
                    "Failed to find the group kernel object created just before it. Exception info: "
                            + e.toString());
            throw new KernelObjectNotCreatedException("Failed to find the kernel object", e);
        }

        return groupStub;
    }

    public static void main(String args[]) {
        if (args.length != 3) {
            System.out.println("Invalid arguments to OMS.");
            System.out.println("[IP] [port] [AppClassName]");
            return;
        }

        int port = 1099;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid arguments to OMS.");
            System.out.println("[IP] [port] [AppClassName]");
            return;
        }

        System.setProperty("java.rmi.server.hostname", args[0]);
        try {
            OMSServerImpl oms = new OMSServerImpl(args[2]);
            OMSServer omsStub = (OMSServer) UnicastRemoteObject.exportObject(oms, 0);
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("SapphireOMS", omsStub);

            /* Create an instance of kernel server and export kernel server service */
            KernelServer localKernelServer =
                    new KernelServerImpl(new InetSocketAddress(args[0], port), oms);
            KernelServer localKernelServerStub =
                    (KernelServer) UnicastRemoteObject.exportObject(localKernelServer, 0);
            registry.rebind("SapphireKernelServer", localKernelServerStub);

            logger.info("OMS ready");
            for (Iterator<InetSocketAddress> it = oms.getServers().iterator(); it.hasNext(); ) {
                InetSocketAddress address = it.next();
                logger.fine("   " + address.getHostName().toString() + ":" + address.getPort());
            }
        } catch (Exception e) {
            logger.severe("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Registers a sapphire object
     *
     * @return Returns sapphire object id
     * @throws RemoteException
     */
    @Override
    public SapphireObjectID registerSapphireObject() throws RemoteException {
        return objectManager.add(null);
    }

    /**
     * Register a sapphire replica of a given sapphire object
     *
     * @param sapphireObjId
     * @return Return sapphire replica id
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     */
    @Override
    public SapphireReplicaID registerSapphireReplica(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException {
        return objectManager.add(sapphireObjId, null);
    }

    /**
     * Sets the event handler of sapphire object
     *
     * @param sapphireObjId
     * @param dispatcher
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     */
    @Override
    public void setSapphireObjectDispatcher(SapphireObjectID sapphireObjId, EventHandler dispatcher)
            throws RemoteException, SapphireObjectNotFoundException {
        objectManager.set(sapphireObjId, dispatcher);
    }

    /**
     * Sets the event handler of sapphire replica
     *
     * @param replicaId
     * @param dispatcher
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     */
    @Override
    public void setSapphireReplicaDispatcher(SapphireReplicaID replicaId, EventHandler dispatcher)
            throws RemoteException, SapphireObjectNotFoundException {
        objectManager.set(replicaId, dispatcher);
    }

    /**
     * Gets the event handler of sapphire object
     *
     * @param sapphireObjId
     * @return
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     */
    @Override
    public EventHandler getSapphireObjectDispatcher(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException {
        return objectManager.get(sapphireObjId);
    }

    /**
     * Gets the event handler of sapphire replica
     *
     * @param replicaId
     * @return
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     */
    @Override
    public EventHandler getSapphireReplicaDispatcher(SapphireReplicaID replicaId)
            throws RemoteException, SapphireObjectNotFoundException {
        return objectManager.get(replicaId);
    }
}

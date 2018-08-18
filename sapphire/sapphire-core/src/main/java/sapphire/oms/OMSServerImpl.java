package sapphire.oms;

import static sapphire.runtime.Sapphire.getPolicyStub;
import static sapphire.runtime.Sapphire.initializeGroupPolicy;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import org.json.JSONException;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectCreationException;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNameModificationException;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.common.SapphireReplicaID;
import sapphire.compiler.GlobalStubConstants;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectNotCreatedException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelServerNotFoundException;
import sapphire.kernel.common.ServerInfo;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.policy.SapphirePolicy.SapphireClientPolicy;
import sapphire.policy.SapphirePolicy.SapphireGroupPolicy;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;
import sapphire.runtime.EventHandler;

/**
 * OMSServer for tracking objects in Sapphire
 *
 * @author iyzhang
 */
public class OMSServerImpl implements OMSServer {
    private static Logger logger = Logger.getLogger("sapphire.oms.OMSServerImpl");

    private GlobalKernelObjectManager kernelObjectManager;
    private KernelServerManager serverManager;
    private SapphireObjectManager objectManager;

    /** CONSTRUCTOR * */
    // TODO Should receive a List of servers
    public OMSServerImpl() throws IOException, NotBoundException, JSONException {
        kernelObjectManager = new GlobalKernelObjectManager();
        serverManager = new KernelServerManager();
        objectManager = new SapphireObjectManager();
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

    /**
     * Deletes sapphire object replica of given replica id
     *
     * @param replicaId
     * @return Returns true on success and false on failure
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     * @throws SapphireObjectReplicaNotFoundException
     */
    @Override
    public boolean deleteSapphireObjectReplica(SapphireReplicaID replicaId)
            throws RemoteException, SapphireObjectNotFoundException,
                    SapphireObjectReplicaNotFoundException {
        KernelServer server;
        EventHandler handler;
        SapphireInstanceManager instance =
                objectManager.getSapphireInstanceById(replicaId.getOID());
        synchronized (instance) {
            handler = getSapphireReplicaDispatcher(replicaId);
            if (handler == null) {
                throw new SapphireObjectReplicaNotFoundException(
                        "Failed to delete sapphire object replica.");
            }
            unRegisterSapphireReplica(replicaId);
        }

        /* Get the kernel server stub */
        server = serverManager.getServer(handler.getHost());
        if (server == null) {
            // Logged inside getServer call
            return true;
        }

        /* Delete the server policy object on the kernel server */
        try {
            server.deleteSapphireReplicaHandler(replicaId, handler);
        } catch (RemoteException e) {
            logger.warning(
                    "Kernel server " + handler.getHost() + " is not reachable. Exception: " + e);
        }

        return true;
    }

    /**
     * Starts the app on one of the servers and returns the App Object Stub
     *
     * @throws RemoteException
     */
    /*@Override
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
    }*/

    /**
     * Create the sapphire object of given class
     *
     * @param absoluteSapphireClassName
     * @param args
     * @return Returns sapphire object id
     * @throws RemoteException
     * @throws SapphireObjectCreationException
     */
    @Override
    public SapphireObjectID createSapphireObject(String absoluteSapphireClassName, Object... args)
            throws RemoteException, SapphireObjectCreationException {
        /* Get a random server in the first region */
        InetSocketAddress host = serverManager.getServerInRegion(serverManager.getRegions().get(0));
        if (host == null) {
            throw new SapphireObjectCreationException(
                    "Failed to create sapphire object. Kernel server is not available in the region");
        }

        /* Get the kernel server stub */
        KernelServer server = serverManager.getServer(host);
        if (server == null) {
            throw new SapphireObjectCreationException(
                    "Failed to create sapphire object. Kernel server not found.");
        }

        /* Invoke create sapphire object on the kernel server */
        try {
            AppObjectStub appObjStub = server.createSapphireObject(absoluteSapphireClassName, args);
            Field field =
                    appObjStub
                            .getClass()
                            .getDeclaredField(GlobalStubConstants.APPSTUB_POLICY_CLIENT_FIELD_NAME);
            field.setAccessible(true);
            SapphireClientPolicy clientPolicy = (SapphireClientPolicy) field.get(appObjStub);
            return clientPolicy.getGroup().getSapphireObjId();
        } catch (Exception e) {
            throw new SapphireObjectCreationException(
                    "Failed to create sapphire object. Exception occurred at kernel server.", e);
        }
    }

    /**
     * Gets the sapphire object stub of given sapphire object id
     *
     * @param sapphireObjId
     * @return Returns sapphire object stub
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     */
    @Override
    public AppObjectStub acquireSapphireObjectStub(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException {
        EventHandler policyHandler = null;
        SapphireInstanceManager instance = objectManager.getSapphireInstanceById(sapphireObjId);

        /* Instead of getting appobject always from the first replica handler, get it from a
        random replica and return it. It ensure that even if first replica handler is not reachable,
         acquire appobject will eventually succeed later if not in this call */
        synchronized (instance) {
            if (!instance.getReplicaMap().isEmpty()) {
                Object[] handlers = instance.getReplicaMap().values().toArray();
                Object randomHandler = handlers[new Random().nextInt(handlers.length)];
                policyHandler = (EventHandler) randomHandler;
            }

            if (policyHandler == null) {
                throw new SapphireObjectNotFoundException();
            }
        }

        SapphireServerPolicy serverPolicy =
                (SapphireServerPolicy) policyHandler.getObjects().get(0);
        return (AppObjectStub) serverPolicy.sapphire_getAppObject().getObject();
    }

    /**
     * Assigns the name to given sapphire object
     *
     * @param sapphireObjId
     * @param sapphireObjName
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     * @throws SapphireObjectNameModificationException
     */
    @Override
    public void setSapphireObjectName(SapphireObjectID sapphireObjId, String sapphireObjName)
            throws RemoteException, SapphireObjectNotFoundException,
                    SapphireObjectNameModificationException {
        objectManager.setName(sapphireObjId, sapphireObjName);
    }

    /**
     * Attach to the given sapphire object
     *
     * @param sapphireObjName
     * @return Returns sapphire object stub
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     */
    @Override
    public AppObjectStub attachToSapphireObject(String sapphireObjName)
            throws RemoteException, SapphireObjectNotFoundException {
        AppObjectStub appObjStub;
        SapphireInstanceManager instance = objectManager.getSapphireInstanceByName(sapphireObjName);
        synchronized (instance) {
            appObjStub = acquireSapphireObjectStub(instance.getOid());
            instance.incrRefCountAndGet();
        }
        return appObjStub;
    }

    /**
     * Detach from the given sapphire object
     *
     * @param sapphireObjName
     * @return Returns true on success and false on failure
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     */
    @Override
    public boolean detachFromSapphireObject(String sapphireObjName)
            throws RemoteException, SapphireObjectNotFoundException {
        SapphireInstanceManager instance = objectManager.getSapphireInstanceByName(sapphireObjName);
        return deleteSapphireObject(instance.getOid());
    }

    /**
     * Delete the sapphire object of given sapphire object id
     *
     * @param sapphireObjId
     * @return Returns true on success and false on failure
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     */
    @Override
    public boolean deleteSapphireObject(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException {
        KernelServer server;
        Map<SapphireReplicaID, EventHandler> replicaMap;
        EventHandler handler;
        SapphireInstanceManager instance = objectManager.getSapphireInstanceById(sapphireObjId);
        synchronized (instance) {
            if (0 != instance.decrRefCountAndGet()) {
                return true;
            }

            replicaMap = instance.getReplicaMap();

            /* Get all the server policy handler and delete sapphire object replicas from respective
            kernel servers */
            for (Iterator<Map.Entry<SapphireReplicaID, EventHandler>> itr =
                            replicaMap.entrySet().iterator();
                    itr.hasNext(); ) {
                Map.Entry<SapphireReplicaID, EventHandler> entry = itr.next();
                SapphireReplicaID replicaId = entry.getKey();
                handler = entry.getValue();

                /* Get the kernel server stub */
                server = serverManager.getServer(handler.getHost());
                if (server == null) {
                    // Logged inside getServer call
                    continue;
                }

                /* Delete the server policy object on the kernel server */
                try {
                    server.deleteSapphireReplicaHandler(replicaId, handler);
                } catch (RemoteException e) {
                    logger.warning(
                            "Kernel server "
                                    + handler.getHost()
                                    + " is not reachable. Exception: "
                                    + e);
                }
            }

            handler = instance.getInstanceDispatcher();
            unRegisterSapphireObject(sapphireObjId);
        }

        /* Get the kernel server stub for the deleting group policy object */
        server = serverManager.getServer(handler.getHost());
        if (server == null) {
            // Logged inside getServer call
            return true;
        }

        /* Delete the group policy object on the kernel server */
        try {
            server.deleteSapphireObjectHandler(sapphireObjId, handler);
        } catch (RemoteException e) {
            logger.warning(
                    "Kernel server " + handler.getHost() + " is not reachable. Exception: " + e);
        }

        return true;
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
    public SapphireGroupPolicy createGroupPolicy(
            Class<?> policyClass, SapphireObjectID sapphireObjId)
            throws RemoteException, ClassNotFoundException, KernelObjectNotCreatedException,
                    SapphireObjectNotFoundException {
        SapphireGroupPolicy groupPolicyStub = (SapphireGroupPolicy) getPolicyStub(policyClass);
        try {
            SapphireGroupPolicy groupPolicy = initializeGroupPolicy(groupPolicyStub);
            groupPolicyStub.setSapphireObjId(sapphireObjId);
            groupPolicy.setSapphireObjId(sapphireObjId);

            EventHandler sapphireHandler =
                    new EventHandler(
                            GlobalKernelReferences.nodeServer.getLocalHost(),
                            new ArrayList() {
                                {
                                    add(groupPolicyStub);
                                }
                            });

            /* Register the handler for the sapphire object */
            setSapphireObjectDispatcher(sapphireObjId, sapphireHandler);
        } catch (KernelObjectNotFoundException e) {
            logger.severe(
                    "Failed to find the group kernel object created just before it. Exception info: "
                            + e);
            throw new KernelObjectNotCreatedException("Failed to find the kernel object", e);
        } catch (SapphireObjectNotFoundException e) {
            logger.warning("Failed to find sapphire object. Exception info: " + e);
            KernelObjectFactory.delete(groupPolicyStub.$__getKernelOID());
            throw e;
        }

        return groupPolicyStub;
    }

    public static void main(String args[]) {
        if (args.length != 2) {
            System.out.println("Invalid arguments to OMS.");
            System.out.println("[IP] [port]");
            return;
        }

        int port = 1099;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid arguments to OMS.");
            System.out.println("[IP] [port]");
            return;
        }

        System.setProperty("java.rmi.server.hostname", args[0]);
        try {
            OMSServerImpl oms = new OMSServerImpl();
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

    /**
     * Unregister the sapphire object
     *
     * @param sapphireObjId
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     */
    public void unRegisterSapphireObject(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException {
        objectManager.remove(sapphireObjId);
    }

    /**
     * Unregister the replica of sapphire object
     *
     * @param replicaId
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     */
    public void unRegisterSapphireReplica(SapphireReplicaID replicaId)
            throws RemoteException, SapphireObjectNotFoundException {
        objectManager.remove(replicaId);
    }
}

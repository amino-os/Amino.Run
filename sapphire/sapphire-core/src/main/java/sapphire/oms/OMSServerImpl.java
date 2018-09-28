package sapphire.oms;

import static sapphire.compiler.GlobalStubConstants.POLICY_ONDESTROY_MTD_NAME_FORMAT;

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
import sapphire.app.DMSpec;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectCreationException;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNameModificationException;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.common.SapphireReplicaID;
import sapphire.compiler.GlobalStubConstants;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectNotCreatedException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelServerNotFoundException;
import sapphire.kernel.common.ServerInfo;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.policy.SapphirePolicy;
import sapphire.runtime.EventHandler;
import sapphire.runtime.Sapphire;

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
        logger.info("Registering " + oid.toString() + " on host " + host.toString());
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
        return serverManager.getFirstServerInRegion(region);
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
     * Extracts sapphire client policy from app object stub
     *
     * @param appObjStub
     * @return Returns sapphire client policy
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private SapphirePolicy.SapphireClientPolicy extractClientPolicy(AppObjectStub appObjStub)
            throws NoSuchFieldException, IllegalAccessException {
        Field field =
                appObjStub
                        .getClass()
                        .getDeclaredField(GlobalStubConstants.APPSTUB_POLICY_CLIENT_FIELD_NAME);
        field.setAccessible(true);
        return (SapphirePolicy.SapphireClientPolicy) field.get(appObjStub);
    }

    /**
     * Create the sapphire object of given class on one of the servers
     *
     * @param sapphireObjectSpec sapphire object specification in YAML
     * @param args parameters to sapphire object constructor
     * @return Returns sapphire object id
     * @throws RemoteException
     * @throws SapphireObjectCreationException
     */
    @Override
    public SapphireObjectID createSapphireObject(String sapphireObjectSpec, Object... args)
            throws RemoteException, SapphireObjectCreationException {
        /* Get a random server in the first region */
        InetSocketAddress host =
                serverManager.getRandomServerInRegion(serverManager.getRegions().get(0));
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

        // TODO(multi-lang): Store spec together with object ID in objectManager
        // SapphireObjectSpec spec = SapphireObjectSpec.fromYaml(sapphireObjectSpec);

        /* Invoke create sapphire object on the kernel server */
        try {
            AppObjectStub appObjStub = server.createSapphireObject(sapphireObjectSpec, args);
            SapphirePolicy.SapphireClientPolicy clientPolicy = extractClientPolicy(appObjStub);
            objectManager.setInstanceObjectStub(
                    clientPolicy.getGroup().getSapphireObjId(), appObjStub);
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
        EventHandler[] handlers = objectManager.getSapphireReplicasById(sapphireObjId);

        /* Instead of getting appobject always from the first replica handler, get it from a
        random replica and return it. It ensure that even if first replica handler is not reachable,
         acquire appobject will eventually succeed later if not in this call */
        if (handlers.length != 0) {
            policyHandler = handlers[new Random().nextInt(handlers.length)];
        }

        if (policyHandler == null) {
            throw new SapphireObjectNotFoundException("Failed to get sapphire object.");
        }

        AppObjectStub appObjStub = null;
        try {
            AppObjectStub localObjStub = objectManager.getInstanceObjectStub(sapphireObjId);
            SapphirePolicy.SapphireClientPolicy clientPolicy = extractClientPolicy(localObjStub);
            SapphirePolicy.SapphireServerPolicy serverPolicy =
                    (SapphirePolicy.SapphireServerPolicy) policyHandler.getObjects().get(0);
            appObjStub = (AppObjectStub) serverPolicy.sapphire_getRemoteAppObject().getObject();
            appObjStub.$__initialize(false);
            SapphirePolicy.SapphireClientPolicy client = clientPolicy.getClass().newInstance();
            client.onCreate(
                    clientPolicy.getGroup(), clientPolicy.getGroup().getAppConfigAnnotation());
            client.setServer(serverPolicy);
            appObjStub.$__initialize(client);
        } catch (Exception e) {
            logger.warning("Exception occurred : " + e);
            throw new SapphireObjectNotFoundException(
                    "Failed to get object. Exception occurred.", e);
        }

        return appObjStub;
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
        objectManager.setInstanceName(sapphireObjId, sapphireObjName);
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
        SapphireObjectID sapphireObjId = objectManager.getSapphireInstanceIdByName(sapphireObjName);
        AppObjectStub appObjStub = acquireSapphireObjectStub(sapphireObjId);
        objectManager.incrRefCountAndGet(sapphireObjId);
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
        return deleteSapphireObject(objectManager.getSapphireInstanceIdByName(sapphireObjName));
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

        if (objectManager.decrRefCountAndGet(sapphireObjId) != 0) {
            return true;
        }

        EventHandler handler = getSapphireObjectDispatcher(sapphireObjId);
        if (handler == null) {
            logger.warning("Sapphire object handler is null");
            return false;
        }

        /* Invoke onDestroy method on group policy object */
        try {
            handler.invoke(
                    String.format(
                            POLICY_ONDESTROY_MTD_NAME_FORMAT,
                            handler.getObjects().get(0).getClass().getName()),
                    new ArrayList());
        } catch (Exception e) {
            logger.warning("Exception occurred : " + e);
        }

        return true;
    }

    /**
     * Creates the group policy instance on the kernel server running within OMS and returns group
     * policy object Stub
     *
     * @param policyClass
     * @param sapphireObjId
     * @return Returns group policy object stub
     * @throws RemoteException
     * @throws ClassNotFoundException
     * @throws KernelObjectNotCreatedException
     * @throws SapphireObjectNotFoundException
     */
    @Override
    public SapphirePolicy.SapphireGroupPolicy createGroupPolicy(
            Class<?> policyClass, SapphireObjectID sapphireObjId, Map<String, DMSpec> dmSpecMap)
            throws RemoteException, ClassNotFoundException, KernelObjectNotCreatedException,
                    SapphireObjectNotFoundException {
        return Sapphire.createGroupPolicy(policyClass, sapphireObjId, dmSpecMap);
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
        return objectManager.addInstance(null);
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
        return objectManager.addReplica(sapphireObjId, null);
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
        objectManager.setInstanceDispatcher(sapphireObjId, dispatcher);
    }

    /**
     * Sets the event handler of sapphire replica
     *
     * @param replicaId
     * @param dispatcher
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     * @throws SapphireObjectReplicaNotFoundException
     */
    @Override
    public void setSapphireReplicaDispatcher(SapphireReplicaID replicaId, EventHandler dispatcher)
            throws RemoteException, SapphireObjectNotFoundException,
                    SapphireObjectReplicaNotFoundException {
        objectManager.setReplicaDispatcher(replicaId, dispatcher);
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
        return objectManager.getInstanceDispatcher(sapphireObjId);
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
            throws RemoteException, SapphireObjectNotFoundException,
                    SapphireObjectReplicaNotFoundException {
        return objectManager.getReplicaDispatcher(replicaId);
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
        objectManager.removeInstance(sapphireObjId);
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
        objectManager.removeReplica(replicaId);
    }
}

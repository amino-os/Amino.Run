package amino.run.oms;

import amino.run.app.MicroServiceSpec;
import amino.run.app.NodeSelectorSpec;
import amino.run.app.Registry;
import amino.run.common.AppObjectStub;
import amino.run.common.MicroServiceCreationException;
import amino.run.common.SapphireObjectID;
import amino.run.common.MicroServiceNameModificationException;
import amino.run.common.SapphireObjectNotFoundException;
import amino.run.common.SapphireObjectReplicaNotFoundException;
import amino.run.common.SapphireReplicaID;
import amino.run.compiler.GlobalStubConstants;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.KernelObjectNotCreatedException;
import amino.run.kernel.common.KernelObjectNotFoundException;
import amino.run.kernel.common.KernelServerNotFoundException;
import amino.run.kernel.common.ServerInfo;
import amino.run.kernel.server.KernelServer;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.policy.Policy;
import amino.run.runtime.EventHandler;
import amino.run.runtime.Sapphire;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;

/**
 * OMSServer for tracking objects in Sapphire
 *
 * @author iyzhang
 */
public class OMSServerImpl implements OMSServer, Registry {

    private static Logger logger = Logger.getLogger(OMSServerImpl.class.getName());
    private static String SERVICE_PORT = "--servicePort";

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
            throws KernelObjectNotFoundException {
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
     * Gets all servers matching the specified node selector
     *
     * @param spec
     * @return
     * @throws RemoteException
     */
    public List<InetSocketAddress> getServers(NodeSelectorSpec spec) throws RemoteException {
        return serverManager.getServers(spec);
    }

    /**
     * Extracts sapphire client policy from app object stub
     *
     * @param appObjStub
     * @return Returns sapphire client policy
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private Policy.ClientPolicy extractClientPolicy(AppObjectStub appObjStub)
            throws NoSuchFieldException, IllegalAccessException {
        Field field =
                appObjStub
                        .getClass()
                        .getDeclaredField(GlobalStubConstants.APPSTUB_POLICY_CLIENT_FIELD_NAME);
        field.setAccessible(true);
        return (Policy.ClientPolicy) field.get(appObjStub);
    }

    @Override
    public SapphireObjectID create(String microServiceSpec, Object... args)
            throws RemoteException, MicroServiceCreationException {

        MicroServiceSpec spec = MicroServiceSpec.fromYaml(microServiceSpec);
        /* Get a best server from the given spec */
        InetSocketAddress host = serverManager.getBestSuitableServer(spec);
        if (host == null) {
            throw new MicroServiceCreationException(
                    "Failed to create sapphire object. Kernel server is not available  with the given requirements");
        }

        /* Get the kernel server stub */
        KernelServer server = serverManager.getServer(host);
        if (server == null) {
            throw new MicroServiceCreationException(
                    "Failed to create sapphire object. Kernel server not found.");
        }

        // TODO(multi-lang): Store spec together with object ID in objectManager
        // MicroServiceSpec spec = MicroServiceSpec.fromYaml(sapphireObjectSpec);

        /* Invoke create sapphire object on the kernel server */
        try {
            AppObjectStub appObjStub = server.createSapphireObject(microServiceSpec, args);
            assert appObjStub != null;
            Policy.ClientPolicy clientPolicy = extractClientPolicy(appObjStub);
            SapphireObjectID sid = clientPolicy.getGroup().getSapphireObjId();
            objectManager.setInstanceObjectStub(sid, appObjStub);
            objectManager.setRootGroupPolicy(sid, clientPolicy.getGroup());
            return clientPolicy.getGroup().getSapphireObjId();
        } catch (Exception e) {
            throw new MicroServiceCreationException(
                    "Failed to create sapphire object. Exception occurred at kernel server.", e);
        }
    }

    @Override
    public AppObjectStub acquireStub(SapphireObjectID id) throws SapphireObjectNotFoundException {

        try {
            AppObjectStub appObjStub = objectManager.getInstanceObjectStub(id);
            appObjStub.$__initialize(false);
            return appObjStub;
        } catch (Exception e) {
            throw new SapphireObjectNotFoundException(
                    "Failed to acquire stub for sapphire object " + id, e);
        }
    }

    @Override
    public void setName(SapphireObjectID id, String name)
            throws RemoteException, SapphireObjectNotFoundException,
            MicroServiceNameModificationException {
        objectManager.setInstanceName(id, name);
    }

    @Override
    public AppObjectStub attachTo(String name)
            throws RemoteException, SapphireObjectNotFoundException {
        SapphireObjectID sapphireObjId = objectManager.getSapphireInstanceIdByName(name);
        AppObjectStub appObjStub = acquireStub(sapphireObjId);
        objectManager.incrRefCountAndGet(sapphireObjId);
        return appObjStub;
    }

    @Override
    public boolean detachFrom(String name) throws RemoteException, SapphireObjectNotFoundException {
        return delete(objectManager.getSapphireInstanceIdByName(name));
    }

    @Override
    public boolean delete(SapphireObjectID id) throws SapphireObjectNotFoundException {

        if (objectManager.decrRefCountAndGet(id) != 0) {
            return true;
        }

        boolean successfullyRemoved = true;
        try {
            objectManager.removeInstance(id);
            logger.log(
                    Level.FINE,
                    String.format("Successfully removed sapphire object with oid %s", id));
        } catch (Exception e) {
            logger.log(
                    Level.SEVERE,
                    String.format("Failed to remove sapphire object with oid %s", id),
                    e);
            successfullyRemoved = false;
        }
        return successfullyRemoved;
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
    public Policy.GroupPolicy createGroupPolicy(
            Class<?> policyClass, SapphireObjectID sapphireObjId)
            throws RemoteException, ClassNotFoundException, KernelObjectNotCreatedException,
                    SapphireObjectNotFoundException {
        Policy.GroupPolicy group = Sapphire.createGroupPolicy(policyClass, sapphireObjId);

        /* TODO: This rootGroupPolicy is used in sapphire object deletion. Need to handle for multiDM case. In case of
        multiDM, multiple group policy objects are created in DM chain establishment. Currently, just ensuring not to
        overwrite the outermost DM's group policy reference(i.e., first created group policy in chain).So that deletion
        works for single DM case.
         */
        if (objectManager.getRootGroupPolicy(sapphireObjId) == null) {
            objectManager.setRootGroupPolicy(sapphireObjId, group);
        }

        return group;
    }

    public static void main(String args[]) {
        if (args.length < 2) {
            System.out.println("Invalid arguments to OMS.");
            System.out.println("[IP] [port] [servicePort]");
            return;
        }

        int port = 1099;
        int servicePort = 0;
        try {
            port = Integer.parseInt(args[1]);
            if (args.length > 2) {
                servicePort = Integer.parseInt(parseServicePort(args[2]));
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid arguments to OMS.");
            System.out.println("[IP] [port] [servicePort]");
            return;
        }

        System.setProperty("java.rmi.server.hostname", args[0]);
        try {
            OMSServerImpl oms = new OMSServerImpl();
            OMSServer omsStub = (OMSServer) UnicastRemoteObject.exportObject(oms, servicePort);
            java.rmi.registry.Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("SapphireOMS", omsStub);

            /* Create an instance of kernel server and export kernel server service */
            KernelServer localKernelServer =
                    new KernelServerImpl(new InetSocketAddress(args[0], port), oms);
            KernelServer localKernelServerStub =
                    (KernelServer) UnicastRemoteObject.exportObject(localKernelServer, servicePort);
            registry.rebind("SapphireKernelServer", localKernelServerStub);

            logger.info("OMS ready");
            // to get all the kernel server's addresses passing null in oms.getServers
            for (Iterator<InetSocketAddress> it = oms.getServers(null).iterator(); it.hasNext(); ) {
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
     * Unregister the sapphire object
     *
     * @param sapphireObjId
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException:w
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

    /**
     * get all the sapphire objects in the system
     *
     * @return Returns ArrayList<SapphireObjectID>
     * @throws RemoteException
     */
    public ArrayList<SapphireObjectID> getAllSapphireObjects() throws RemoteException {
        ArrayList<SapphireObjectID> arr = objectManager.getAllSapphireObjects();
        return arr;
    }

    /**
     * get all the Replicas of a SapphireObject
     *
     * @return Returns ArrayList<EventHandler>
     * @throws RemoteException
     */
    public EventHandler[] getSapphireReplicasById(SapphireObjectID oid)
            throws SapphireObjectNotFoundException {
        return objectManager.getSapphireReplicasById(oid);
    }

    /**
     * get all the kernel object Ids in the oms system
     *
     * @return Returns ArrayList<KernelOID>
     * @throws RemoteException
     */
    public ArrayList<KernelOID> getAllKernelObjects() throws RemoteException {
        return kernelObjectManager.getAllKernelObjects();
    }

    private static String parseServicePort(String servicePort) {
        String port = null;
        if (servicePort != null) {
            port = servicePort.substring(SERVICE_PORT.length() + 1);
        }
        return port;
    }
}

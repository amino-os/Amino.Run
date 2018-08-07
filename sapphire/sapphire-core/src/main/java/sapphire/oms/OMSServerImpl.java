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
import java.util.Map;
import java.util.logging.Logger;
import org.json.JSONException;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectCreationException;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireReplicaID;
import sapphire.common.SapphireSoStub;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectNotCreatedException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelServerNotFoundException;
import sapphire.kernel.common.ServerInfo;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.policy.SapphirePolicy;
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
    /** APP METHODS * */

    /**
     * Starts the app on one of the servers and returns the App Object Stub
     *
     * @throws RemoteException
     */
    /*       @Override
    public AppObjectStub getAppEntryPoint() throws RemoteException {
     if (appEntryPoint != null) {
      return appEntryPoint;
     } else {
      	InetSocketAddress host = serverManager.getServerInRegion(serverManager.getRegions().get(0));
    KernelServer server = serverManager.getServer(host);
    appEntryPoint = server.startApp(appEntryClassName);
      	return appEntryPoint;
     }
    }*/

    @Override
    public AppObjectStub acquireSapphireObjectStub(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException {
        SapphireInstanceManager instance = objectManager.getSapphireInstanceManager(sapphireObjId);
        SapphireSoStub soStub = instance.getObjectStub();
        return soStub.getAppObjectStub();
    }

    @Override
    public SapphireClientInfo acquireSapphireObjectStub(
            SapphireObjectID sapphireObjId, InetSocketAddress clientPolicyKernelServerHost)
            throws RemoteException, SapphireObjectNotFoundException, ClassNotFoundException,
                    KernelObjectNotCreatedException, InstantiationException,
                    IllegalAccessException {
        SapphireInstanceManager instance = objectManager.getSapphireInstanceManager(sapphireObjId);
        SapphireSoStub soStub = instance.getObjectStub();

        KernelServer ks =
                GlobalKernelReferences.nodeServer
                        .getKernelClient()
                        .getServer(clientPolicyKernelServerHost);

        /* Set the server policy in the appstub's client before returning the appstub object */
        Map.Entry<SapphireReplicaID, EventHandler> entry =
                instance.getReplicaMap().entrySet().iterator().next();

        SapphirePolicy.SapphireClientPolicy client =
                ks.createSapphireClientPolicy(
                        soStub.getClientPolicyName(),
                        (SapphireServerPolicy) entry.getValue().getObjects().get(0),
                        soStub.getGroupPolicy(),
                        soStub.getDmAnnotations());

        instance.addClientId(client.$__getKernelOID());

        /* byte stream, client id and sapphire id */
        return new SapphireClientInfo(
                client.$__getKernelOID().getID(), sapphireObjId.getID(), soStub.getOpaqueObject());
    }

    @Override
    public void setSapphireObjectName(SapphireObjectID sapphireObjectID, String sapphireObjName)
            throws RemoteException, SapphireObjectNotFoundException {
        objectManager.setName(sapphireObjectID, sapphireObjName);
    }

    @Override
    public SapphireObjectID createSapphireObject(String absoluteSapphireClassName, Object... args)
            throws RemoteException, SapphireObjectCreationException, ClassNotFoundException {
        InetSocketAddress host = serverManager.getServerInRegion(serverManager.getRegions().get(0));
        KernelServer server = serverManager.getServer(host);
        SapphireSoStub soStub = server.createSapphireObject(absoluteSapphireClassName, args);
        try {
            objectManager
                    .getSapphireInstanceManager(soStub.getSapphireObjId())
                    .setObjectStub(soStub);
        } catch (SapphireObjectNotFoundException e) {
            throw new SapphireObjectCreationException(
                    "Failed to create sapphire object : "
                            + absoluteSapphireClassName
                            + " : Trace : "
                            + e);
        }

        return soStub.getSapphireObjId();
    }

    @Override
    public SapphireObjectID createSapphireObject(
            String absoluteSapphireClassName,
            String runtimeType,
            String constructorName,
            byte[] args)
            throws RemoteException, SapphireObjectCreationException, ClassNotFoundException,
                    KernelObjectNotCreatedException, InstantiationException,
                    IllegalAccessException {
        SapphireSoStub soStub;
        InetSocketAddress host = serverManager.getServerInRegion(serverManager.getRegions().get(0));
        KernelServer server = serverManager.getServer(host);

        try {
            soStub =
                    server.createSapphireObject(
                            absoluteSapphireClassName, runtimeType, constructorName, args);
            objectManager
                    .getSapphireInstanceManager(soStub.getSapphireObjId())
                    .setObjectStub(soStub);
        } catch (SapphireObjectNotFoundException e) {
            throw new SapphireObjectCreationException(
                    "Failed to create sapphire object : "
                            + absoluteSapphireClassName
                            + " : Trace : "
                            + e);
        } catch (KernelObjectNotFoundException e) {
            throw new SapphireObjectCreationException(
                    "Failed to create sapphire object : "
                            + absoluteSapphireClassName
                            + " : Trace : "
                            + e);
        }

        return soStub.getSapphireObjId();
    }

    @Override
    public boolean deleteSapphireObject(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException {
        KernelServer server;
        SapphireInstanceManager instance = objectManager.getSapphireInstanceManager(sapphireObjId);

        Map<SapphireReplicaID, EventHandler> replicaMap = instance.getReplicaMap();

        /* Delete all the sapphire object replicas from respective kernel server */
        for (Map.Entry<SapphireReplicaID, EventHandler> entry : replicaMap.entrySet()) {
            SapphireReplicaID rid = entry.getKey();
            EventHandler handler = entry.getValue();

            /* Delete the object from the host */
            server = serverManager.getServer(handler.getHost());
            try {
                server.deleteSapphireReplica(rid, handler);
            } catch (KernelObjectNotFoundException e) {
                logger.warning(
                        "Kernel object not found "
                                + " on "
                                + handler.getHost()
                                + ". Exception: "
                                + e);
            }
        }

        /* Delete all the client policies on all the client kernel servers */
        Iterator<KernelOID> itr = instance.getClientList().iterator();
        while (itr.hasNext()) {
            KernelOID clientId = itr.next();
            try {
                serverManager
                        .getServer(lookupKernelObject(clientId))
                        .deleteSapphireClientPolicy(clientId);
            } catch (KernelObjectNotFoundException e) {
                e.printStackTrace();
            }
        }

        /* Update the parent-child relationship */
        if (0 != instance.getParentSapphireObjId().getID()) {
            SapphireInstanceManager parentInstance =
                    objectManager.getSapphireInstanceManager(instance.getParentSapphireObjId());
            parentInstance.removeChildSapphireObjId(sapphireObjId);
        }

        /* Delete all the childs */
        Iterator<SapphireObjectID> iterator = instance.getChildSapphireObjIds().iterator();
        while (iterator.hasNext()) {
            /* Ideally, When the root SO object is being deleted, application logic should have taken care of deleting all the child SOs under it. When application do not trigger deletion of child SO, we delete it explicitly */
            /* TODO: Recursion call. Need to check on avoiding it. */
            deleteSapphireObject(iterator.next());
        }

        /* Delete the sapphire object from the kernel server */
        EventHandler handler = instance.getInstanceDispatcher();

        /* Delete the object from the host */
        server = serverManager.getServer(handler.getHost());
        server.deleteSapphireObject(sapphireObjId, handler);

        return true;
    }

    @Override
    public boolean releaseSapphireObjectStub(SapphireObjectID sapphireObjId)
            throws SapphireObjectNotFoundException, RemoteException {
        SapphireInstanceManager instance = objectManager.getSapphireInstanceManager(sapphireObjId);
        return true;
    }

    @Override
    public boolean releaseSapphireObjectStub(SapphireObjectID sapphireObjId, int clientId)
            throws RemoteException, SapphireObjectNotFoundException, KernelObjectNotFoundException {
        SapphireInstanceManager instance = objectManager.getSapphireInstanceManager(sapphireObjId);

        KernelOID clientOid = new KernelOID(clientId);
        serverManager
                .getServer(lookupKernelObject(clientOid))
                .deleteSapphireClientPolicy(clientOid);
        instance.removeClientId(clientOid);
        return true;
    }

    @Override
    public boolean detachFromSapphireObject(String sapphireObjName)
            throws RemoteException, SapphireObjectNotFoundException {
        SapphireObjectID sapphireObjId = objectManager.get(sapphireObjName);
        releaseSapphireObjectStub(sapphireObjId);
        if (0 != objectManager.getSapphireInstanceManager(sapphireObjId).decrRefCountAndGet()) {
            return true;
        }

        return deleteSapphireObject(sapphireObjId);
    }

    @Override
    public boolean detachFromSapphireObject(SapphireObjectID sapphireObjId, int clientId)
            throws RemoteException, SapphireObjectNotFoundException, KernelObjectNotFoundException {
        releaseSapphireObjectStub(sapphireObjId, clientId);
        if (0 != objectManager.getSapphireInstanceManager(sapphireObjId).decrRefCountAndGet()) {
            return true;
        }

        return deleteSapphireObject(sapphireObjId);
    }

    @Override
    public AppObjectStub attachToSapphireObject(String sapphireObjName)
            throws RemoteException, SapphireObjectNotFoundException {
        SapphireObjectID sapphireObjId = objectManager.get(sapphireObjName);
        AppObjectStub appObjStub = acquireSapphireObjectStub(sapphireObjId);
        objectManager.getSapphireInstanceManager(sapphireObjId).incrRefCountAndGet();
        return appObjStub;
    }

    @Override
    public SapphireClientInfo attachToSapphireObject(
            String sapphireObjName, InetSocketAddress clientPolicyKernelServerHost)
            throws RemoteException, SapphireObjectNotFoundException, ClassNotFoundException,
                    KernelObjectNotCreatedException, InstantiationException,
                    IllegalAccessException {
        SapphireObjectID sapphireObjId = objectManager.get(sapphireObjName);

        /* bytestream and clientId */
        SapphireClientInfo clientInfo =
                acquireSapphireObjectStub(sapphireObjId, clientPolicyKernelServerHost);
        objectManager.getSapphireInstanceManager(sapphireObjId).incrRefCountAndGet();
        return clientInfo;
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
            throws RemoteException, KernelObjectNotCreatedException, KernelObjectNotFoundException,
                    ClassNotFoundException, SapphireObjectNotFoundException {
        SapphireInstanceManager instance = objectManager.getSapphireInstanceManager(sapphireObjId);
        final SapphireGroupPolicy groupStub = (SapphireGroupPolicy) getPolicyStub(policyClass);
        initializeGroupPolicy(groupStub);
        EventHandler sapphireHandler =
                new EventHandler(
                        GlobalKernelReferences.nodeServer.getLocalHost(),
                        new ArrayList() {
                            {
                                add(groupStub);
                            }
                        });
        GlobalKernelReferences.nodeServer.oms.setSapphireObjectDispatcher(
                sapphireObjId, sapphireHandler);
        return groupStub;
    }

    @Override
    public void setInnerSapphireObject(SapphireSoStub soStub)
            throws SapphireObjectNotFoundException {
        SapphireInstanceManager instance =
                objectManager.getSapphireInstanceManager(soStub.getSapphireObjId());
        instance.setObjectStub(soStub);
        instance.setParentSapphireObjId(soStub.getParentSapphireObjId());
        instance = objectManager.getSapphireInstanceManager(instance.getParentSapphireObjId());
        instance.addChildSapphireObjId(soStub.getSapphireObjId());
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

        OMSServerImpl oms = null;

        try {
            oms = new OMSServerImpl();
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
     * Sets the event handler of the sapphire object
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
     * Sets the event handler of the sapphire replica
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
     * gets the event handler of the given sapphire object
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
     * Gets the event handler of the given sapphire replica
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

    @Override
    public void unRegisterSapphireObject(SapphireObjectID oid)
            throws RemoteException, SapphireObjectNotFoundException {
        objectManager.remove(oid);
    }

    @Override
    public void unRegisterSapphireReplica(SapphireReplicaID oid)
            throws RemoteException, SapphireObjectNotFoundException {
        objectManager.remove(oid);
    }
}

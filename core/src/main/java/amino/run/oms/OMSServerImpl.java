package amino.run.oms;

import amino.run.app.MicroServiceSpec;
import amino.run.app.NodeSelectorSpec;
import amino.run.app.Registry;
import amino.run.common.AppObjectStub;
import amino.run.common.ArgumentParser.OMSArgumentParser;
import amino.run.common.MicroServiceCreationException;
import amino.run.common.MicroServiceID;
import amino.run.common.MicroServiceNameModificationException;
import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.common.ReplicaID;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.KernelObjectNotCreatedException;
import amino.run.kernel.common.KernelObjectNotFoundException;
import amino.run.kernel.common.KernelServerNotFoundException;
import amino.run.kernel.common.ServerInfo;
import amino.run.kernel.metric.RPCMetric;
import amino.run.kernel.server.KernelServer;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.oms.migrationdecision.MetricWatcher;
import amino.run.policy.Policy;
import amino.run.runtime.EventHandler;
import amino.run.runtime.MicroService;
import com.google.devtools.common.options.OptionsParser;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;

/**
 * OMSServer for tracking objects in MicroService
 *
 * @author iyzhang
 */
public class OMSServerImpl implements OMSServer, Registry {

    private static final Logger logger = Logger.getLogger(OMSServerImpl.class.getName());
    private GlobalKernelObjectManager kernelObjectManager;
    private KernelServerManager serverManager;
    private MicroServiceManager objectManager;
    private MetricWatcher metricWatcher;

    public static String OMS_IP_OPT = "--oms-ip";
    public static String OMS_PORT_OPT = "--oms-port";
    public static String SERVICE_PORT = "--service-port";

    /** CONSTRUCTOR * */
    // TODO Should receive a List of servers
    public OMSServerImpl() throws JSONException {
        kernelObjectManager = new GlobalKernelObjectManager();
        serverManager = new KernelServerManager();
        objectManager = new MicroServiceManager();
        metricWatcher = new MetricWatcher(serverManager, objectManager);
    }

    /** KERNEL METHODS * */
    /**
     * Register new kernel object
     *
     * @return a new unique kernel object ID
     */
    public KernelOID registerKernelObject(InetSocketAddress host) throws RemoteException {
        KernelOID oid = kernelObjectManager.register(host);
        logger.info("[OMS] Registering " + oid.toString() + " on host " + host.toString());
        return oid;
    }

    /** Register a new host for this kernel object. Used to move a kernel object */
    public void registerKernelObject(KernelOID oid, InetSocketAddress host)
            throws KernelObjectNotFoundException {
        logger.info("[OMS] Registering new host for " + oid.toString() + " on " + host.toString());
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
            throws KernelObjectNotFoundException {
        logger.info("[OMS] UnRegistering " + oid.toString() + " on host " + host.toString());
        kernelObjectManager.unRegister(oid, host);
    }

    /**
     * Find the host for a kernel object
     *
     * @return the host IP address
     */
    public InetSocketAddress lookupKernelObject(KernelOID oid)
            throws KernelObjectNotFoundException {
        InetSocketAddress ko = kernelObjectManager.lookup(oid);
        logger.info("[OMS] Found host for " + oid.toString() + " host: " + ko);
        return ko;
    }

    @Override
    public void registerKernelServer(ServerInfo info) throws RemoteException, NotBoundException {
        serverManager.registerKernelServer(info);

        /* After registering the new kernel server, notify all the kernel servers about all the available kernel servers
        in a separate thread */
        new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                notifyAvailableKernelServers();
                            }
                        })
                .start();
    }

    @Override
    public void receiveHeartBeat(ServerInfo srvinfo)
            throws RemoteException, KernelServerNotFoundException {
        serverManager.receiveHeartBeat(srvinfo);
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
     */
    public List<InetSocketAddress> getServers(NodeSelectorSpec spec) {
        return serverManager.getServers(spec);
    }

    /** Notify all the kernel servers about all the available kernel servers */
    private void notifyAvailableKernelServers() {
        List<InetSocketAddress> servers = getServers(null);
        for (InetSocketAddress host : servers) {
            try {
                KernelServer server = serverManager.getServer(host);
                if (server != null) {
                    server.updateAvailableKernelServers(servers);
                }
            } catch (RemoteException e) {
                /* Log warning and continue to update for other servers */
                logger.severe(
                        String.format(
                                "Failed to update available kernel server list to host %s", host));
            }
        }
    }

    @Override
    public MicroServiceID create(String microServiceSpec, Object... args)
            throws MicroServiceCreationException {

        MicroServiceSpec spec = MicroServiceSpec.fromYaml(microServiceSpec);
        // validate Micro service spec
        spec.validate();

        /* Get a best server from the given spec */
        InetSocketAddress host = serverManager.getBestSuitableServer(spec);
        if (host == null) {
            throw new MicroServiceCreationException(
                    "[OMS] Failed to create microservice. Kernel server with the given requirements is not available");
        }

        /* Get the kernel server stub */
        KernelServer server = serverManager.getServer(host);
        if (server == null) {
            throw new MicroServiceCreationException(
                    "[OMS] Failed to create microservice. Kernel server not found.");
        }

        // TODO(multi-lang): Store spec together with object ID in objectManager
        // MicroServiceSpec spec = MicroServiceSpec.fromYaml(microserviceSpec);

        /* Invoke create microservice on the kernel server */
        try {
            AppObjectStub appObjStub = server.createMicroService(spec, args);
            assert appObjStub != null;
            objectManager.setInstanceObjectStub(appObjStub.$__getMicroServiceId(), appObjStub);
            return appObjStub.$__getMicroServiceId();
        } catch (Exception e) {
            throw new MicroServiceCreationException(
                    "[OMS] Failed to create microservice. Exception occurred at kernel server.", e);
        }
    }

    @Override
    public AppObjectStub acquireStub(MicroServiceID id) throws MicroServiceNotFoundException {

        try {
            AppObjectStub appObjStub = objectManager.getInstanceObjectStub(id);
            appObjStub.$__initialize(false);
            return appObjStub;
        } catch (Exception e) {
            throw new MicroServiceNotFoundException(
                    "[OMS] Failed to acquire stub for microservice " + id, e);
        }
    }

    @Override
    public void setName(MicroServiceID id, String name)
            throws MicroServiceNotFoundException, MicroServiceNameModificationException {
        objectManager.setInstanceName(id, name);
    }

    @Override
    public AppObjectStub attachTo(String name) throws MicroServiceNotFoundException {
        MicroServiceID microServiceId = objectManager.getMicroServiceByName(name);
        AppObjectStub appObjStub = acquireStub(microServiceId);
        objectManager.incrRefCountAndGet(microServiceId);
        return appObjStub;
    }

    @Override
    public boolean detachFrom(String name) throws MicroServiceNotFoundException {
        return delete(objectManager.getMicroServiceByName(name));
    }

    @Override
    public boolean delete(MicroServiceID microServiceId) throws MicroServiceNotFoundException {

        if (objectManager.decrRefCountAndGet(microServiceId) != 0) {
            return true;
        }

        boolean successfullyRemoved = true;
        try {
            /* Get the kernel object Id of root group policy and delete the group policy object. This leads to
            successive deletion of complete multiDM tree. Deletion includes all the server policy objects of all group
            policy objects in the DM chain and finally those group policy objects too. */
            KernelOID groupOid = objectManager.getRootGroupId(microServiceId);
            deleteGroupPolicy(microServiceId, groupOid);
            objectManager.removeInstance(microServiceId);
            logger.log(
                    Level.FINE, String.format("Removed microservice with oid %s", microServiceId));
        } catch (Exception e) {
            logger.log(
                    Level.SEVERE,
                    String.format("Failed to remove microservice with oid %s", microServiceId),
                    e);
            successfullyRemoved = false;
        }
        return successfullyRemoved;
    }

    /**
     * Updates the microservice metrics received for the given replica
     *
     * @param replicaId
     * @param metrics
     * @throws MicroServiceNotFoundException
     * @throws MicroServiceReplicaNotFoundException
     */
    @Override
    public void updateMetric(ReplicaID replicaId, Map<UUID, RPCMetric> metrics)
            throws MicroServiceNotFoundException, MicroServiceReplicaNotFoundException {
        objectManager.updateMetric(replicaId, metrics);
    }

    /**
     * Creates the group policy instance for microservice on the kernel server running within OMS
     *
     * @param policyClass
     * @param microServiceId
     * @param spec
     * @return Returns group policy object stub
     * @throws ClassNotFoundException
     * @throws KernelObjectNotCreatedException
     * @throws MicroServiceNotFoundException
     */
    @Override
    public Policy.GroupPolicy createGroupPolicy(
            Class<?> policyClass, MicroServiceID microServiceId, MicroServiceSpec spec)
            throws ClassNotFoundException, KernelObjectNotCreatedException,
                    MicroServiceNotFoundException {
        final Policy.GroupPolicy group =
                MicroService.createGroupPolicy(policyClass, microServiceId, spec);
        EventHandler groupHandler =
                new EventHandler(
                        GlobalKernelReferences.nodeServer.getLocalHost(),
                        new ArrayList() {
                            {
                                add(group);
                            }
                        });
        objectManager.addGroupDispatcher(microServiceId, group.$__getKernelOID(), groupHandler);
        return group;
    }

    /**
     * Deletes the group policy instance for microservice
     *
     * @param microServiceId
     * @param groupOid
     * @throws MicroServiceNotFoundException
     */
    public void deleteGroupPolicy(MicroServiceID microServiceId, KernelOID groupOid)
            throws MicroServiceNotFoundException {
        objectManager.removeGroupDispatcher(microServiceId, groupOid);
        MicroService.deleteGroupPolicy(groupOid);
        return;
    }

    public static void main(String args[]) {
        OptionsParser parser = OptionsParser.newOptionsParser(OMSArgumentParser.class);
        if (args.length < 4) {
            System.out.println("Incorrect arguments to the program");
            printUsage(parser);
            return;
        }

        try {
            parser.parse(args);
        } catch (Exception e) {
            System.out.println(e.getMessage() + System.lineSeparator());
            printUsage(parser);
            return;
        }

        OMSArgumentParser omsArgs = parser.getOptions(OMSArgumentParser.class);
        System.setProperty("java.rmi.server.hostname", omsArgs.omsIP);
        OMSServerImpl oms = null;
        try {
            oms = new OMSServerImpl();
            OMSServer omsStub =
                    (OMSServer) UnicastRemoteObject.exportObject(oms, omsArgs.servicePort);
            java.rmi.registry.Registry registry = LocateRegistry.createRegistry(omsArgs.omsPort);
            registry.rebind("io.amino.run.oms", omsStub);

            /* Create an instance of kernel server and export kernel server service */
            InetSocketAddress host = new InetSocketAddress(omsArgs.omsIP, omsArgs.omsPort);
            KernelServer localKernelServer = new KernelServerImpl(host, host, oms);
            KernelServer localKernelServerStub =
                    (KernelServer)
                            UnicastRemoteObject.exportObject(
                                    localKernelServer, omsArgs.servicePort);
            registry.rebind("io.amino.run.kernelserver", localKernelServerStub);

            // start metric watcher
            oms.metricWatcher.start();

            // Log being used in examples gradle task "run", hence modify accordingly.
            logger.info(String.format("OMS ready at port (%s)!", omsArgs.omsPort));

            // to get all the kernel server's addresses passing null in oms.getServers
            for (Iterator<InetSocketAddress> it = oms.getServers(null).iterator(); it.hasNext(); ) {
                InetSocketAddress address = it.next();
                logger.fine("   " + address.getHostName() + ":" + address.getPort());
            }
        } catch (Exception e) {
            // stop metric watcher time
            if (oms != null && oms.metricWatcher != null) {
                oms.metricWatcher.stop();
            }
            logger.severe("OMS server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Registers a microservice
     *
     * @return Returns microservice id
     * @throws RemoteException
     */
    @Override
    public MicroServiceID registerMicroService() throws RemoteException {
        return objectManager.addInstance(null);
    }

    /**
     * Register a replica of a given microservice
     *
     * @param microServiceId
     * @return Return replica id
     * @throws RemoteException
     * @throws MicroServiceNotFoundException
     */
    @Override
    public ReplicaID registerReplica(MicroServiceID microServiceId)
            throws MicroServiceNotFoundException {
        return objectManager.addReplica(microServiceId, null);
    }

    /**
     * Set the event handler of a microservice replica
     *
     * @param replicaId
     * @param dispatcher
     * @throws RemoteException
     * @throws MicroServiceNotFoundException
     * @throws MicroServiceReplicaNotFoundException
     */
    @Override
    public void setReplicaDispatcher(ReplicaID replicaId, EventHandler dispatcher)
            throws RemoteException, MicroServiceNotFoundException,
                    MicroServiceReplicaNotFoundException {
        objectManager.setReplicaDispatcher(replicaId, dispatcher);
    }

    /**
     * Unregister the microservice
     *
     * @param microServiceId
     * @throws RemoteException
     * @throws MicroServiceNotFoundException :w
     */
    public void unRegisterMicroService(MicroServiceID microServiceId)
            throws RemoteException, MicroServiceNotFoundException {
        objectManager.removeInstance(microServiceId);
    }

    /**
     * Unregister the replica of microservice
     *
     * @param replicaId
     * @throws RemoteException
     * @throws MicroServiceNotFoundException
     */
    public void unRegisterReplica(ReplicaID replicaId) throws MicroServiceNotFoundException {
        objectManager.removeReplica(replicaId);
    }

    /**
     * get all the microservices in the system
     *
     * @return Returns ArrayList<MicroServiceID>
     * @throws RemoteException
     */
    public ArrayList<MicroServiceID> getAllMicroServices() throws RemoteException {
        ArrayList<MicroServiceID> arr = objectManager.getAllMicroServices();
        return arr;
    }

    /**
     * get all the Replicas of a MicroService
     *
     * @return Returns ArrayList<EventHandler>
     * @throws RemoteException
     */
    public EventHandler[] getReplicasById(MicroServiceID oid) throws MicroServiceNotFoundException {
        return objectManager.getReplicasById(oid);
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

    private static void printUsage(OptionsParser parser) {
        System.out.println(
                "Usage: java -cp <classpath> "
                        + OMSServerImpl.class.getSimpleName()
                        + System.lineSeparator()
                        + parser.describeOptions(
                                Collections.<String, String>emptyMap(),
                                OptionsParser.HelpVerbosity.LONG));
    }
}

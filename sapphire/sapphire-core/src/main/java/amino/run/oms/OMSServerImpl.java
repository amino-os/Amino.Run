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
import amino.run.app.SapphireObjectServer;
import amino.run.app.labelselector.Selector;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;

/**
 * OMSServer for tracking objects in MicroService
 *
 * @author iyzhang
 */
public class OMSServerImpl implements OMSServer, Registry {

    private static Logger logger = Logger.getLogger(OMSServerImpl.class.getName());
    private GlobalKernelObjectManager kernelObjectManager;
    private KernelServerManager serverManager;
    private MicroServiceManager objectManager;

    public static String OMS_IP_OPT = "--oms-ip";
    public static String OMS_PORT_OPT = "--oms-port";
    public static String SERVICE_PORT = "--service-port";

    /** CONSTRUCTOR * */
    // TODO Should receive a List of servers
    public OMSServerImpl() throws JSONException {
        kernelObjectManager = new GlobalKernelObjectManager();
        serverManager = new KernelServerManager();
        objectManager = new MicroServiceManager();
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
            throws KernelObjectNotFoundException {
        logger.info("UnRegistering " + oid.toString() + " on host " + host.toString());
        kernelObjectManager.unRegister(oid, host);
    }

    /**
     * Find the host for a kernel object
     *
     * @return the host IP address
     */
    public InetSocketAddress lookupKernelObject(KernelOID oid)
            throws KernelObjectNotFoundException {
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

    @Override
    public MicroServiceID create(String microServiceSpec, Object... args)
            throws MicroServiceCreationException {

        MicroServiceSpec spec = MicroServiceSpec.fromYaml(microServiceSpec);
        /* Get a best server from the given spec */
        InetSocketAddress host = serverManager.getBestSuitableServer(spec);
        if (host == null) {
            throw new MicroServiceCreationException(
                    "Failed to create microservice. Kernel server with the given requirements is not available");
        }

        /* Get the kernel server stub */
        KernelServer server = serverManager.getServer(host);
        if (server == null) {
            throw new MicroServiceCreationException(
                    "Failed to create microservice. Kernel server not found.");
        }

        // TODO(multi-lang): Store spec together with object ID in objectManager
        // MicroServiceSpec spec = MicroServiceSpec.fromYaml(microserviceSpec);

        /* Invoke create microservice on the kernel server */
        try {
            AppObjectStub appObjStub = server.createMicroService(microServiceSpec, args);
            assert appObjStub != null;
	    MicroServiceID mid = appObjStub.$__getMicroServiceId();
            objectManager.setInstanceObjectStub(mid, appObjStub);
            objectManager.setSapphireObjectSpec(mid, spec);
            return mid;
        } catch (Exception e) {
            throw new MicroServiceCreationException(
                    "Failed to create microservice. Exception occurred at kernel server.", e);
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
                    "Failed to acquire stub for microservice " + id, e);
        }
    }

    /**
     * Gets the sapphire object stub of given sapphire object id
     *
     * @param selector
     * @return Returns list of sapphire object stub
     * @throws SapphireObjectNotFoundException
     */
    @Override
    public ArrayList<AppObjectStub> acquireSapphireObjectStub(Selector selector)
            throws SapphireObjectNotFoundException {

        try {
            ArrayList<AppObjectStub> appObjStubs = objectManager.getInstanceObjectStub(selector);
            for (AppObjectStub appObjStub : appObjStubs) {
                appObjStub.$__initialize(false);
            }
            return appObjStubs;
        } catch (Exception e) {
            e.printStackTrace();
            throw new SapphireObjectNotFoundException(
                    String.format(
                            "Failed to acquire sapphire object stub with selector %s : %s",
                            selector, e.getMessage()));
        }
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
    public boolean detachFrom(String name) throws RemoteException, MicroServiceNotFoundException {
        return delete(objectManager.getMicroServiceByName(name));
    }

    @Override
    public boolean delete(MicroServiceID id) throws MicroServiceNotFoundException {

        if (objectManager.decrRefCountAndGet(id) != 0) {
            return true;
        }

        boolean successfullyRemoved = true;
        try {
            objectManager.removeInstance(id);
            logger.log(
                    Level.FINE, String.format("Successfully removed microservice with oid %s", id));
        } catch (Exception e) {
            logger.log(
                    Level.SEVERE,
                    String.format("Failed to remove microservice with oid %s", id),
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
     * @param microServiceId
     * @return Returns group policy object stub
     * @throws RemoteException
     * @throws ClassNotFoundException
     * @throws KernelObjectNotCreatedException
     * @throws MicroServiceNotFoundException
     */
    @Override
    public Policy.GroupPolicy createGroupPolicy(Class<?> policyClass, MicroServiceID microServiceId)
            throws ClassNotFoundException, KernelObjectNotCreatedException,
                    MicroServiceNotFoundException {
        Policy.GroupPolicy group = MicroService.createGroupPolicy(policyClass, microServiceId);

        /* TODO: This rootGroupPolicy is used in microservice deletion. Need to handle for multiDM case. In case of
        multiDM, multiple group policy objects are created in DM chain establishment. Currently, just ensuring not to
        overwrite the outermost DM's group policy reference(i.e., first created group policy in chain).So that deletion
        works for single DM case.
         */
        if (objectManager.getRootGroupPolicy(microServiceId) == null) {
            objectManager.setRootGroupPolicy(microServiceId, group);
        }

        return group;
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

        try {
            OMSServerImpl oms = new OMSServerImpl();
            OMSServer omsStub =
                    (OMSServer) UnicastRemoteObject.exportObject(oms, omsArgs.servicePort);
            java.rmi.registry.Registry registry = LocateRegistry.createRegistry(omsArgs.omsPort);
            registry.rebind("io.amino.run.oms", omsStub);

            /* Create an instance of kernel server and export kernel server service */
            KernelServer localKernelServer =
                    new KernelServerImpl(
                            new InetSocketAddress(omsArgs.omsIP, omsArgs.omsPort), oms);
            KernelServer localKernelServerStub =
                    (KernelServer)
                            UnicastRemoteObject.exportObject(
                                    localKernelServer, omsArgs.servicePort);
            registry.rebind("io.amino.run.kernelserver", localKernelServerStub);

            // Log being used in examples gradle task "run", hence modify accordingly.
            logger.info(String.format("OMS ready at port (%s)!", omsArgs.omsPort));

            // to get all the kernel server's addresses passing null in oms.getServers
            for (Iterator<InetSocketAddress> it = oms.getServers(null).iterator(); it.hasNext(); ) {
                InetSocketAddress address = it.next();
                logger.fine("   " + address.getHostName() + ":" + address.getPort());
            }
        } catch (Exception e) {
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

package amino.run.kernel.server;

import amino.run.app.MicroServiceSpec;
import amino.run.common.AppObjectStub;
import amino.run.common.MicroServiceCreationException;
import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.kernel.client.KernelClient;
import amino.run.kernel.common.*;
import amino.run.oms.OMSServer;
import amino.run.policy.Library;
import amino.run.policy.Policy;
import amino.run.policy.PolicyContainer;
import amino.run.policy.util.ResettableTimer;
import amino.run.runtime.EventHandler;
import amino.run.runtime.Sapphire;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sapphire Kernel Server. Runs on every Sapphire node, knows how to talk to the OMS, handles RPCs
 * and has a client for making RPCs.
 *
 * @author iyzhang
 */
public class KernelServerImpl implements KernelServer {
    private static Logger logger = Logger.getLogger(KernelServerImpl.class.getName());
    public static String LABEL_OPT = "--labels";
    public static String OPT_SEPARATOR = "=";
    public static String LABEL_SEPARATOR = ",";
    public static String SERVICE_PORT = "--servicePort";
    public static String DEFAULT_REGION = "default-region";
    public static String REGION_KEY = "region";

    private InetSocketAddress host;
    private String region;
    /** manager for kernel objects that live on this server */
    private KernelObjectManager objectManager;
    /** stub for the OMS */
    public static OMSServer oms;
    /** local kernel client for making RPCs */
    private KernelClient client;
    // heartbeat period is 1/3of the heartbeat timeout period
    static final long KS_HEARTBEAT_PERIOD = OMSServer.KS_HEARTBEAT_TIMEOUT / 3;

    // heartbeat timer
    private ResettableTimer ksHeartbeatSendTimer;

    public KernelServerImpl(InetSocketAddress host, InetSocketAddress omsHost) {
        OMSServer oms = null;
        try {
            Registry registry =
                    LocateRegistry.getRegistry(omsHost.getHostName(), omsHost.getPort());
            oms = (OMSServer) registry.lookup("SapphireOMS");
        } catch (Exception e) {
            logger.severe("Could not find OMS: " + e.toString());
        }
        init(host, oms);
    }

    public KernelServerImpl(InetSocketAddress host, OMSServer oms) {
        init(host, oms);
    }

    private void init(InetSocketAddress host, OMSServer oms) {
        this.oms = oms;
        this.host = host;
        objectManager = new KernelObjectManager();
        client = new KernelClient(oms);
        GlobalKernelReferences.nodeServer = this;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getRegion() {
        return this.region;
    }

    /** RPC INTERFACES * */

    /**
     * Invoke an RPC on this kernel server. This is a public RMI interface.
     *
     * @param rpc All of the information about the RPC, the object id, the method and arguments
     * @return the return value from the method invocation
     */
    @Override
    public Object makeKernelRPC(KernelRPC rpc)
            throws RemoteException, KernelObjectNotFoundException, KernelObjectMigratingException,
                    KernelRPCException {
        KernelObject object = null;
        object = objectManager.lookupObject(rpc.getOID());

        logger.log(
                Level.FINE,
                "Invoking RPC on Kernel Object with OID: "
                        + rpc.getOID()
                        + "with rpc:"
                        + rpc.getMethod()
                        + " params: "
                        + rpc.getParams().toString());

        Object ret = null;
        try {
            ret = object.invoke(rpc.getMethod(), rpc.getParams());
        } catch (Exception e) {
            throw new KernelRPCException(e);
        }
        return ret;
    }

    /**
     * Move a kernel object to this server.
     *
     * @param oid the kernel object id
     * @param object the kernel object to be stored on this server
     */
    public void copyKernelObject(KernelOID oid, KernelObject object)
            throws RemoteException, KernelObjectNotFoundException,
                    KernelObjectStubNotCreatedException, MicroServiceNotFoundException,
                    MicroServiceReplicaNotFoundException {
        // To add Kernel Object to local object manager
        Serializable realObj = object.getObject();

        if (!(realObj instanceof Library.ServerPolicyLibrary)) {
            logger.log(Level.WARNING, "Added " + oid.getID() + " as unknown type.");
            return;
        }

        Library.ServerPolicyLibrary firstServerPolicy = (Library.ServerPolicyLibrary) realObj;
        List<PolicyContainer> spContainers = firstServerPolicy.getProcessedPolicies();

        logger.log(
                Level.INFO,
                String.format(
                        "Started adding %d objects. First OID: %s", spContainers.size(), oid));

        List<String> serverPolicies = new ArrayList<>();

        for (PolicyContainer spContainer : firstServerPolicy.getProcessedPolicies()) {
            // Add Server Policy object in the same order as client side has created.
            Library.ServerPolicyLibrary serverPolicy = spContainer.getServerPolicy();

            // Added for setting the ReplicaId and registering handler for this replica to OMS.
            Policy.ServerPolicy serverPolicyStub =
                    (Policy.ServerPolicy) spContainer.getServerPolicyStub();
            ArrayList<Object> policyObjList = new ArrayList<>();
            EventHandler policyHandler = new EventHandler(host, policyObjList);
            policyObjList.add(serverPolicyStub);
            serverPolicyStub.setReplicaId(serverPolicy.getReplicaId());
            oms.setSapphireReplicaDispatcher(serverPolicy.getReplicaId(), policyHandler);

            KernelOID koid = serverPolicy.$__getKernelOID();

            objectManager.addObject(koid, new KernelObject(serverPolicy));
            oms.registerKernelObject(koid, host);

            serverPolicies.add(serverPolicy.toString());

            try {
                serverPolicy.onCreate(serverPolicy.getGroup(), serverPolicy.getMicroServiceSpec());
            } catch (Exception e) {
                String exceptionMsg =
                        "Initialization failed at copyKernelObject for KernelObject("
                                + koid.getID()
                                + ").  ";
                logger.severe(exceptionMsg);
                throw new RemoteException(exceptionMsg, e);
            }
        }

        logger.log(
                Level.INFO,
                String.format(
                        "Finished adding chain for %s with %s",
                        oid, String.join(", ", serverPolicies)));
    }

    /** LOCAL INTERFACES * */
    /**
     * Create a new kernel object locally on this server.
     *
     * @param cl
     * @param args
     */
    public KernelOID newKernelObject(Class<?> cl, Object... args)
            throws KernelObjectNotCreatedException {
        KernelOID oid = null;
        // get OID
        try {
            oid = oms.registerKernelObject(host);
        } catch (RemoteException e) {
            throw new KernelObjectNotCreatedException("Error making RPC to OMS: " + e);
        }

        // Create the actual kernel object stored in the object manager
        objectManager.newObject(oid, cl, args);
        logger.fine("Created new Kernel Object on host: " + host + " with OID: " + oid.getID());

        return oid;
    }

    /**
     * Move object from this server to host.
     *
     * @param serverPolicy
     * @param host
     * @throws RemoteException
     * @throws KernelObjectNotFoundException
     * @throws MicroServiceNotFoundException
     * @throws MicroServiceReplicaNotFoundException
     */
    public void moveKernelObjectToServer(Policy.ServerPolicy serverPolicy, InetSocketAddress host)
            throws RemoteException, KernelObjectNotFoundException, MicroServiceNotFoundException,
                    MicroServiceReplicaNotFoundException {

        if (host.equals(this.host)) {
            return;
        }

        KernelOID oid = serverPolicy.$__getKernelOID();

        /**
         * The associated ServerPolicy KernelObjects will be moved to the new Server when the first
         * KernelObject is moved. The associated KernelObjects in the local KernelServer should be
         * explicitly removed. These KernelObjects with new KernelServer address are registered with
         * the OMS.
         */
        KernelObject object = objectManager.lookupObject(oid);

        /* Coalesce all the server policies in chain before moving them */
        object.coalesce();
        Policy.ServerPolicy nextPolicy = serverPolicy;

        /* Below objectStub variable temporarily holds
        Either AppObjectStub(in case of the last mile server policy to SO).
        Or KernelObjectStub(in case of multiDM, all intermediate DMs except the last mile server policy to SO).
        If the AppObject is pointing to KernelObjectStub, i.e. having intermediate DMs, need to get those kernel objects
        and coalesce them. */
        Object objectStub;
        while ((nextPolicy.sapphire_getAppObject() != null)
                && ((objectStub = nextPolicy.sapphire_getAppObject().getObject()) != null)
                && (objectStub instanceof KernelObjectStub)) {
            nextPolicy = (Policy.ServerPolicy) objectStub;
            try {
                objectManager.lookupObject(nextPolicy.$__getKernelOID()).coalesce();
            } catch (KernelObjectNotFoundException e) {
                logger.warning(
                        "Could not find object in this server. Oid:"
                                + nextPolicy.$__getKernelOID().getID());
            }
        }

        logger.fine("Moving object " + oid.toString() + " to " + host.toString());

        try {
            client.copyObjectToServer(host, oid, object);
        } catch (RemoteException e) {
            String msg =
                    String.format(
                            "Failed to copy object to server oid:%d, target host:%s",
                            oid.getID(), host.getHostName());
            logger.severe(msg);
            throw new RemoteException(msg, e);
        } catch (KernelObjectStubNotCreatedException e) {
            String msg =
                    String.format(
                            "Failed to create policy stub object on destination server. oid:%d, target host:%s",
                            oid.getID(), host.getHostName());
            logger.severe(msg);
            throw new RemoteException(
                    "Failed to create policy stub object on destination server.", e);
        }

        // Remove the associated KernelObjects from the local KernelServer.
        objectStub = serverPolicy;
        do {
            serverPolicy = (Policy.ServerPolicy) objectStub;
            try {
                objectManager.removeObject(serverPolicy.$__getKernelOID());
                serverPolicy.onDestroy();
            } catch (KernelObjectNotFoundException e) {
                String msg =
                        "Could not find object to remove in this server. Oid:"
                                + serverPolicy.$__getKernelOID().getID();
                logger.warning(msg);
            }
        } while ((serverPolicy.sapphire_getAppObject() != null)
                && ((objectStub = serverPolicy.sapphire_getAppObject().getObject()) != null)
                && (objectStub instanceof KernelObjectStub));
    }

    /**
     * Delete kernel object on this server
     *
     * @param oid
     * @throws RemoteException
     * @throws KernelObjectNotFoundException
     */
    public void deleteKernelObject(KernelOID oid)
            throws RemoteException, KernelObjectNotFoundException {
        KernelObject object = objectManager.lookupObject(oid);

        if (object.getObject() instanceof Policy.ServerPolicy) {
            /* De-initialize dynamic data of server policy object(i.e., timers, executors, sockets etc) */
            ((Policy.ServerPolicy) object.getObject()).onDestroy();
        }

        oms.unRegisterKernelObject(oid, host);
        objectManager.removeObject(oid);
    }

    public Serializable getObject(KernelOID oid) throws KernelObjectNotFoundException {
        KernelObject object = objectManager.lookupObject(oid);
        return object.getObject();
    }

    public KernelObject getKernelObject(KernelOID oid) throws KernelObjectNotFoundException {
        KernelObject object = objectManager.lookupObject(oid);
        return object;
    }

    /**
     * Updates the health status of kernel object
     *
     * @param oid
     * @param status
     * @throws KernelObjectNotFoundException
     */
    public void updateObjectStatus(KernelOID oid, boolean status)
            throws KernelObjectNotFoundException {
        KernelObject object = objectManager.lookupObject(oid);
        object.setStatus(status);
    }

    /**
     * Get the local hostname
     *
     * @return IP address of host that this server is running on
     */
    public InetSocketAddress getLocalHost() {
        return host;
    }

    /**
     * Get the kernel client for making RPCs
     *
     * @return the kernel client in this server
     */
    public KernelClient getKernelClient() {
        return client;
    }

    @Override
    public AppObjectStub createSapphireObject(String soSpecYaml, Object... args)
            throws MicroServiceCreationException {
        logger.log(
                Level.INFO,
                String.format(
                        "Got request to create sapphire object with spec '%s' and %d parameters.",
                        soSpecYaml, args.length));
        MicroServiceSpec spec = MicroServiceSpec.fromYaml(soSpecYaml);
        return (AppObjectStub) Sapphire.new_(spec, args);
    }

    public class MemoryStatThread extends Thread {
        public void run() {
            while (true) {
                try {
                    Thread.sleep(100000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(
                        "Total memory: " + Runtime.getRuntime().totalMemory() + " Bytes");
                System.out.println("Free memory: " + Runtime.getRuntime().freeMemory() + " Bytes");
                logger.fine("objectManager.getAllKernelObjectOids()");
                KernelOID[] Oids = objectManager.getAllKernelObjectOids();
                for (KernelOID oid : Oids) {
                    logger.fine("oid:" + oid.toString());
                }
            }
        }
    }

    public MemoryStatThread getMemoryStatThread() {
        return new MemoryStatThread();
    }

    /** Send heartbeats to OMS. */
    private void startheartbeat(ServerInfo srvinfo) {
        logger.fine("heartbeat KernelServer" + srvinfo);
        try {
            oms.heartbeatKernelServer(srvinfo);
        } catch (Exception e) {
            logger.severe("Cannot heartbeat KernelServer" + srvinfo);
            e.printStackTrace();
        }
        ksHeartbeatSendTimer.reset();
    }
    /**
     * At startup, contact the OMS.
     *
     * @param args
     */
    public static void main(String args[]) {
        try {
            if (args.length < 4) {
                printUsage();
                System.exit(1);
            }

            InetSocketAddress host, omsHost;
            host = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
            omsHost = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
            System.setProperty("java.rmi.server.hostname", host.getAddress().getHostAddress());

            int servicePort = 0;
            String labelStr = "";
            if (args.length > 4) {
                for (int i = 4; i < args.length; i++) {
                    if (args[i].startsWith(LABEL_OPT)) {
                        labelStr = args[i];
                    } else if (args[i].startsWith(SERVICE_PORT)) {
                        servicePort = Integer.parseInt(parseServicePort(args[i]));
                    }
                }
            }
            // Bind server in registry
            KernelServerImpl server = new KernelServerImpl(host, omsHost);
            KernelServer stub =
                    (KernelServer) UnicastRemoteObject.exportObject(server, servicePort);
            Registry registry = LocateRegistry.createRegistry(Integer.parseInt(args[1]));
            registry.rebind("SapphireKernelServer", stub);

            // Register against OMS
            ServerInfo srvInfo = createServerInfo(host, labelStr);
            oms.registerKernelServer(srvInfo);
            server.setRegion(srvInfo.getRegion());

            // Start heartbeat timer
            server.startHeartbeats(srvInfo);

            // Start a thread that print memory stats
            server.getMemoryStatThread().start();
            System.out.println("Server ready!");
        } catch (Exception e) {
            System.err.println("Failed to start kernel server: " + e.getMessage());
            printUsage();
        }
    }

    private void startHeartbeats(ServerInfo srvInfo)
            throws RemoteException, NotBoundException, KernelServerNotFoundException {
        oms.heartbeatKernelServer(srvInfo);
        ksHeartbeatSendTimer =
                new ResettableTimer(
                        new TimerTask() {
                            public void run() {
                                startheartbeat(srvInfo);
                            }
                        },
                        KS_HEARTBEAT_PERIOD);
        ksHeartbeatSendTimer.start();
    }

    public static ServerInfo createServerInfo(InetSocketAddress host, String labelStr) {
        Map<String, String> labels = parseLabel(labelStr);
        if (!labels.containsKey(KernelServerImpl.REGION_KEY)) {
            labels.put(KernelServerImpl.REGION_KEY, KernelServerImpl.DEFAULT_REGION);
        }
        ServerInfo srvInfo = new ServerInfo(host);
        srvInfo.addLabels(labels);
        return srvInfo;
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println(
                String.format(
                        "java -cp <classpath> %s hostIp hostPort omsIp omsPort [--labels comma separated key value as key1=val1,key2=val2] [--servicePort=portnumber]",
                        KernelServerImpl.class.getName()));
    }

    private static Map<String, String> parseLabel(String data) {
        Map<String, String> labels = new HashMap<String, String>();
        if ((data != null) && data.startsWith(KernelServerImpl.LABEL_OPT)) {
            String actualdata = data.substring(KernelServerImpl.LABEL_OPT.length());
            String[] maps = actualdata.split(KernelServerImpl.LABEL_SEPARATOR);
            // not allowed empty values
            for (int i = 0; i < maps.length; i++) {
                String[] kv = maps[i].split(KernelServerImpl.OPT_SEPARATOR);
                if (kv.length % 2 != 0) {
                    logger.warning("something wrong in the labels");
                    continue;
                }
                for (int j = 0; j < kv.length; j += 2) {
                    labels.put(kv[j].trim(), kv[j + 1].trim());
                }
            }
        }
        return labels;
    }

    private static String parseServicePort(String servicePort) {
        String port = null;
        if (servicePort != null) {
            port = servicePort.substring(SERVICE_PORT.length() + 1);
        }
        return port;
    }
}

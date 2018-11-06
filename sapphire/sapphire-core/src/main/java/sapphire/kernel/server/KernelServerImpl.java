package sapphire.kernel.server;

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
import sapphire.app.SapphireObjectSpec;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectCreationException;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.kernel.client.KernelClient;
import sapphire.kernel.common.*;
import sapphire.oms.OMSServer;
import sapphire.policy.SapphirePolicy;
import sapphire.policy.SapphirePolicyContainer;
import sapphire.policy.SapphirePolicyLibrary.*;
import sapphire.policy.util.ResettableTimer;
import sapphire.runtime.EventHandler;
import sapphire.runtime.Sapphire;

/**
 * Sapphire Kernel Server. Runs on every Sapphire node, knows how to talk to the OMS, handles RPCs
 * and has a client for making RPCs.
 *
 * @author iyzhang
 */
public class KernelServerImpl implements KernelServer {
    private static Logger logger = Logger.getLogger("sapphire.kernel.server.KernelServerImpl");
    private static String LABEL_OPT = "--label";
    private static String OPT_SEPARATOR = "=";
    private static String LABEL_SEPARATOR = ",";

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
    static ResettableTimer ksHeartbeatSendTimer;

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
                    KernelObjectStubNotCreatedException, SapphireObjectNotFoundException,
                    SapphireObjectReplicaNotFoundException {
        logger.log(Level.INFO, "Adding object " + oid);
        if (object.getObject() instanceof SapphirePolicy.SapphireServerPolicy) {
            /* Set the policy object handlers of new host */
            SapphirePolicy.SapphireServerPolicy serverPolicy =
                    (SapphirePolicy.SapphireServerPolicy) object.getObject();
            SapphirePolicy.SapphireServerPolicy serverPolicyStub =
                    (SapphirePolicy.SapphireServerPolicy)
                            Sapphire.getPolicyStub(serverPolicy.getClass(), oid);
            ArrayList<Object> policyObjList = new ArrayList<>();
            EventHandler policyHandler = new EventHandler(host, policyObjList);
            policyObjList.add(serverPolicyStub);

            serverPolicyStub.setReplicaId(serverPolicy.getReplicaId());
            oms.setSapphireReplicaDispatcher(serverPolicy.getReplicaId(), policyHandler);

            serverPolicy.onCreate(serverPolicy.getGroup(), serverPolicy.getSapphireObjectSpec());
        }

        // To add Kernel Object to local object manager
        Serializable realObj = object.getObject();

        if (realObj instanceof SapphireServerPolicyLibrary) {
            SapphireServerPolicyLibrary firstServerPolicy = (SapphireServerPolicyLibrary) realObj;

            for (SapphirePolicyContainer spContainer : firstServerPolicy.getProcessedPolicies()) {
                // Add Server Policy object in the same order as client side has created.
                SapphireServerPolicyLibrary serverPolicy = spContainer.getServerPolicy();

                // Added for setting the ReplicaId and registering handler for this replica to OMS.
                SapphirePolicy.SapphireServerPolicy serverPolicyStub =
                        (SapphirePolicy.SapphireServerPolicy) spContainer.getServerPolicyStub();
                ArrayList<Object> policyObjList = new ArrayList<>();
                EventHandler policyHandler = new EventHandler(host, policyObjList);
                policyObjList.add(serverPolicyStub);
                serverPolicyStub.setReplicaId(serverPolicy.getReplicaId());
                oms.setSapphireReplicaDispatcher(serverPolicy.getReplicaId(), policyHandler);

                KernelOID koid = serverPolicy.$__getKernelOID();
                KernelObject newko = new KernelObject(serverPolicy);

                objectManager.addObject(koid, newko);
                newko.uncoalesce();
                oms.registerKernelObject(koid, host);
                logger.log(Level.INFO, "Added " + koid.getID() + " as SapphireServerPolicyLibrary");

                try {
                    serverPolicy.initialize();
                } catch (Exception e) {
                    String exceptionMsg =
                            "Initialization failed at copyKernelObject for KernelObject("
                                    + koid.getID()
                                    + ").  ";
                    logger.severe(exceptionMsg);
                    throw new RemoteException(exceptionMsg, e);
                }
            }
        } else {
            logger.log(Level.WARNING, "Added " + oid.getID() + " as unknown type.");
        }
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
     * @throws SapphireObjectNotFoundException
     * @throws SapphireObjectReplicaNotFoundException
     */
    public void moveKernelObjectToServer(
            SapphirePolicy.SapphireServerPolicy serverPolicy, InetSocketAddress host)
            throws RemoteException, KernelObjectNotFoundException, SapphireObjectNotFoundException,
                    SapphireObjectReplicaNotFoundException {

        if (host.equals(this.host)) {
            return;
        }

        List<SapphirePolicy.SapphireServerPolicy> serverPoliciesToRemove =
                new ArrayList<SapphirePolicy.SapphireServerPolicy>();
        KernelOID oid = serverPolicy.$__getKernelOID();

        /**
         * Create a list of ServerPolicy and associated ServerPolicies in the chain, which needs to
         * be explicitly removed from the local KernelServer. The associated ServerPolicy
         * KernelObjects will be moved to the new Server when the first KernelObject is moved. The
         * remaining KernelObject in the local KernelServer should be explicitly removed. The new
         * KernelServer address needs to be registered with the OMS explicitly for these associated
         * KernelObjects.
         */
        // Add the firstServerPolicy to the list.
        serverPoliciesToRemove.add(serverPolicy);
        // Add the ServerPolicies in the chain to the list, so that the associated
        // KernelObjects are removed from the local KernelServer.
        while (serverPolicy.getNextServerPolicy() != null) {
            serverPolicy = serverPolicy.getNextServerPolicy();
            serverPoliciesToRemove.add(serverPolicy);
        }

        KernelObject object = objectManager.lookupObject(oid);
        object.coalesce();

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

        // Register the moved associated KernelObjects to OMS with the new KernelServer address.
        // Then, remove the associated KernelObjects from the local KernelServer.
        for (SapphirePolicy.SapphireServerPolicy serverPolicyToRemove : serverPoliciesToRemove) {
            try {
                serverPolicyToRemove.onDestroy();
                objectManager.removeObject(serverPolicyToRemove.$__getKernelOID());
            } catch (KernelObjectNotFoundException e) {
                String msg =
                        "Could not find object to remove in this server. Oid:"
                                + serverPolicyToRemove.$__getKernelOID().getID();
                logger.warning(msg);
            }
        }
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

        if (object.getObject() instanceof SapphirePolicy.SapphireServerPolicy) {
            /* De-initialize dynamic data of server policy object(i.e., timers, executors, sockets etc) */
            ((SapphirePolicy.SapphireServerPolicy) object.getObject()).onDestroy();
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
            throws SapphireObjectCreationException {
        logger.log(
                Level.INFO,
                String.format(
                        "Got request to create sapphire object with spec '%s' and %d parameters.",
                        soSpecYaml, args.length));
        SapphireObjectSpec spec = SapphireObjectSpec.fromYaml(soSpecYaml);
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
    static void startheartbeat(ServerInfo srvinfo) {
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

            // TODO: remove region argument
            // Keep it for the time being to maintain backward compatible
            String region = host.toString();
            String labelStr = "";
            if (args.length > 4) {
                if (!args[4].startsWith(LABEL_OPT)) {
                    region = args[4];
                } else {
                    labelStr = args[4];
                }
            }

            if (args.length > 5) {
                labelStr = args[5];
            }

            // Bind server in registry
            KernelServerImpl server = new KernelServerImpl(host, omsHost);
            KernelServer stub = (KernelServer) UnicastRemoteObject.exportObject(server, 0);
            Registry registry = LocateRegistry.createRegistry(Integer.parseInt(args[1]));
            registry.rebind("SapphireKernelServer", stub);
            server.setRegion(region);

            // Register against OMS
            ServerInfo srvInfo = createServerInfo(host, region, labelStr);
            oms.registerKernelServer(srvInfo);

            // Start heartbeat timer
            startHeartbeats(srvInfo);

            // Start a thread that print memory stats
            server.getMemoryStatThread().start();
            System.out.println("Server ready!");
        } catch (Exception e) {
            System.err.println("Failed to start kernel server: " + e.getMessage());
            printUsage();
        }
    }

    private static void startHeartbeats(ServerInfo srvInfo)
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

    private static ServerInfo createServerInfo(
            InetSocketAddress host, String region, String labelStr) {
        ServerInfo srvInfo = new ServerInfo(host, region);
        Set<String> labels = parseLabel(labelStr);
        labels.add(region);
        srvInfo.addLabels(labels);
        return srvInfo;
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println(
                String.format(
                        "java -cp <classpath> %s hostIp hostPort omsIp omsPort [region] [--labels=comma,separated,labels",
                        KernelServerImpl.class.getName()));
    }

    private static Set<String> parseLabel(String labelStr) {
        Set<String> labels = new HashSet<>();
        if (labelStr != null) {
            String[] kv = labelStr.split(OPT_SEPARATOR);
            if (kv.length > 1) {
                labels.addAll(Arrays.asList(kv[1].split(LABEL_SEPARATOR)));
            }
        }
        return labels;
    }
}

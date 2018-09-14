package sapphire.kernel.server;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectCreationException;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.kernel.client.KernelClient;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectMigratingException;
import sapphire.kernel.common.KernelObjectNotCreatedException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelObjectStubNotCreatedException;
import sapphire.kernel.common.KernelRPC;
import sapphire.kernel.common.KernelRPCException;
import sapphire.kernel.common.ServerInfo;
import sapphire.oms.OMSServer;
import sapphire.policy.SapphirePolicy;
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

            /* Initialize dynamic data of server policy object(i.e., timers, executors, sockets etc)
            on new host */
            Class<?> c =
                    serverPolicy.sapphire_getAppObject().getObject().getClass().getSuperclass();
            serverPolicy.onCreate(serverPolicy.getGroup(), c.getAnnotations());
        }

        objectManager.addObject(oid, object);
        object.uncoalesce();
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
     * @param host
     * @param oid
     * @throws RemoteException
     * @throws KernelObjectNotFoundException
     */
    public void moveKernelObjectToServer(InetSocketAddress host, KernelOID oid)
            throws RemoteException, KernelObjectNotFoundException, SapphireObjectNotFoundException,
                    SapphireObjectReplicaNotFoundException {
        if (host.equals(this.host)) {
            return;
        }

        KernelObject object = objectManager.lookupObject(oid);
        object.coalesce();

        logger.fine("Moving object " + oid.toString() + " to " + host.toString());

        try {
            client.copyObjectToServer(host, oid, object);
        } catch (RemoteException e) {
            e.printStackTrace();
            throw new RemoteException("Could not contact destination server.");
        } catch (KernelObjectStubNotCreatedException e) {
            throw new RemoteException(
                    "Failed to create policy stub object on destination server.", e);
        }

        try {
            oms.registerKernelObject(oid, host);
        } catch (RemoteException e) {
            throw new RemoteException("Could not contact oms to update kernel object host.");
        }

        if (object.getObject() instanceof SapphirePolicy.SapphireServerPolicy) {
            /* De-initialize dynamic data of server policy object(i.e., timers, executors, sockets etc) */
            ((SapphirePolicy.SapphireServerPolicy) object.getObject()).onDestroy();
        }

        objectManager.removeObject(oid);
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

    /**
     * Create the sapphire object
     *
     * @param className
     * @param args
     * @return
     * @throws RemoteException
     * @throws SapphireObjectCreationException
     * @throws ClassNotFoundException
     */
    @Override
    public AppObjectStub createSapphireObject(String className, Object... args)
            throws RemoteException, SapphireObjectCreationException, ClassNotFoundException {
        return (AppObjectStub) Sapphire.new_(Class.forName(className), args);
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
        // Time Being for backward compatibility Region is optional in the configuration
        if (args.length < 4) {
            System.out.println("Incorrect arguments to the kernel server");
            System.out.println("[host ip] [host port] [oms ip] [oms port] [region]");
            return;
        }

        InetSocketAddress host, omsHost;

        try {
            host = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
            omsHost = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
        } catch (NumberFormatException e) {
            System.out.println("Incorrect arguments to the kernel server");
            System.out.println("[host ip] [host port] [oms ip] [oms port]");
            return;
        }

        System.setProperty("java.rmi.server.hostname", host.getAddress().getHostAddress());

        try {
            KernelServerImpl server = new KernelServerImpl(host, omsHost);
            KernelServer stub = (KernelServer) UnicastRemoteObject.exportObject(server, 0);
            Registry registry = LocateRegistry.createRegistry(Integer.parseInt(args[1]));
            registry.rebind("SapphireKernelServer", stub);

            if (args.length > 4) {
                server.setRegion(args[4]);
            } else {
                // server.setRegion("default");
                // TODO once we are sure we can comment below line & uncomment above line
                server.setRegion(host.toString());
            }
            final ServerInfo srvinfo = new ServerInfo(host, server.getRegion());
            oms.registerKernelServer(srvinfo);
            logger.info("Server ready!");
            System.out.println("Server ready!");

            ksHeartbeatSendTimer =
                    new ResettableTimer(
                            new TimerTask() {
                                public void run() {
                                    startheartbeat(srvinfo);
                                }
                            },
                            KS_HEARTBEAT_PERIOD);

            oms.heartbeatKernelServer(srvinfo);
            ksHeartbeatSendTimer.start();
            /* Start a thread that print memory stats */
            server.getMemoryStatThread().start();

        } catch (Exception e) {
            logger.severe("Cannot start Sapphire Kernel Server");
            e.printStackTrace();
        }
    }
}

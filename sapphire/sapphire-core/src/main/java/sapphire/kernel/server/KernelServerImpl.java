package sapphire.kernel.server;

import com.google.protobuf.ByteString;

import static sapphire.runtime.Sapphire.createInnerSo;
import static sapphire.runtime.Sapphire.createSo;
import static sapphire.runtime.Sapphire.delete_;
import static sapphire.runtime.Sapphire.new_;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectCreationException;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireReplicaID;
import sapphire.common.SapphireSoStub;
import sapphire.kernel.client.KernelClient;
import sapphire.kernel.common.DMConfigInfo;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectMigratingException;
import sapphire.kernel.common.KernelObjectNotCreatedException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelRPC;
import sapphire.kernel.common.KernelRPCException;
import sapphire.kernel.common.ServerInfo;
import sapphire.oms.OMSServer;
import sapphire.policy.SapphirePolicy;
import sapphire.policy.util.ResettableTimer;
import sapphire.runtime.EventHandler;

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

    private int role = ServerInfo.ROLE_KERNEL_SERVER;
    private KernelGrpcServer grpcServerToRuntime;
    private KernelGrpcServer grpcServerToApp;
    private KernelGrpcClient grpcClientToJavaRuntime;

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

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public void setGrpcServerToRuntime(KernelGrpcServer grpcServer) {
        grpcServerToRuntime = grpcServer;
    }

    public KernelGrpcServer getGrpcServerToRuntime() {
        return grpcServerToRuntime;
    }

    public KernelGrpcServer getGrpcServerToApp() {
        return grpcServerToApp;
    }

    public void setGrpcServerToApp(KernelGrpcServer grpcServerToApp) {
        this.grpcServerToApp = grpcServerToApp;
    }

    public void setGrpcClientToJavaRuntime(KernelGrpcClient grpcClient) {
        grpcClientToJavaRuntime = grpcClient;
    }

    public KernelGrpcClient getGrpcClientToJavaRuntime() {
        return grpcClientToJavaRuntime;
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

    public ByteString genericInvoke(String clientId, String methodName, ArrayList<Object> params)
            throws KernelObjectNotFoundException, KernelRPCException {
        KernelOID clientOid = new KernelOID(Integer.parseInt(clientId));
        KernelObject object = null;
        object = objectManager.lookupObject(clientOid);
        Object ret = null;
        try {
            Method method = object.getObject().getClass().getMethod("onRPC", methodName.getClass(), new ArrayList<Object>().getClass());
            ret = method.invoke(object.getObject(), methodName, params);
        } catch (Exception e) {
            e.printStackTrace();
            throw new KernelRPCException(e);
        }

        return (ByteString)ret;
    }

    /**
     * Move a kernel object to this server.
     *
     * @param oid the kernel object id
     * @param object the kernel object to be stored on this server
     */
    public void copyKernelObject(KernelOID oid, KernelObject object)
            throws RemoteException, KernelObjectNotFoundException {
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
            throws RemoteException, KernelObjectNotFoundException {
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
        }

        try {
            oms.registerKernelObject(oid, host);
        } catch (RemoteException e) {
            throw new RemoteException("Could not contact oms to update kernel object host.");
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

    /** Start the first server-side app object */
    /*@Override
    public AppObjectStub startApp(String className) throws RemoteException {
    	AppObjectStub appEntryPoint = null;
    	try {
    		AppEntryPoint entryPoint =  (AppEntryPoint) Class.forName(className).newInstance();
               appEntryPoint = entryPoint.start();
    	} catch (Exception e) {
    		logger.severe("Could not start app");
    		e.printStackTrace();
    	}
    	return appEntryPoint;
    }*/

    /** ********** Begin: multilanguage related methods ************** */
    /** Create the sapphire object on runtime. */
    @Override
    public SapphireSoStub createSapphireObject(String className, Object... args)
            throws RemoteException, SapphireObjectCreationException, ClassNotFoundException {
        SapphireSoStub soStub = null;
        Class<?> cls = Class.forName(className);
        AppObjectStub appObjStub = (AppObjectStub) new_(cls, args);
        soStub = new SapphireSoStub.SapphireSoStubBuilder().setSapphireObjId(appObjStub.$__getSapphireClientPolicy().getServer().getReplicaId().getOID())
                    .setParentSapphireObjId(null)
                    .setAppObjectStub(appObjStub)
                    .create();
        return soStub;
    }

    @Override
    public SapphireSoStub createSapphireObject(
            String className, String runtimeType, String constructorName, byte[] args)
            throws RemoteException, ClassNotFoundException, KernelObjectNotCreatedException,
            InstantiationException, KernelObjectNotFoundException,
            SapphireObjectNotFoundException, IllegalAccessException {
        return createSo(className, runtimeType, constructorName, args);
    }

    /* Delete sapphire Object handlers created on this server */
    @Override
    public void deleteSapphireObject(SapphireObjectID sapphireObjId, EventHandler handler)
            throws RemoteException, SapphireObjectNotFoundException {
        delete_(sapphireObjId, handler);
    }

    /* Delete sapphire Object replica handlers created on this server and also delete the SO copy on runtime */
    @Override
    public void deleteSapphireReplica(SapphireReplicaID sapphireReplicaId, EventHandler handler)
            throws RemoteException, SapphireObjectNotFoundException, KernelObjectNotFoundException {
        /* Delete SO copy on runtime */
        SapphirePolicy.SapphireServerPolicy serverPolicy =
                getServerPolicyObjectReplicaId(sapphireReplicaId, handler);

        if (null != serverPolicy.sapphire_getAppObject().getRuntime()) {
            if (serverPolicy.sapphire_getAppObject().getRuntime().equalsIgnoreCase("java")) {
                GlobalKernelReferences.nodeServer
                        .getGrpcClientToJavaRuntime()
                        .deleteSapphireReplica(sapphireReplicaId);
            } else if (serverPolicy.sapphire_getAppObject().getRuntime().equalsIgnoreCase("go")) {
                // TODO: Need to call the go runtime
            }
        }

        delete_(sapphireReplicaId, handler);
    }

    /* Create sapphire client policy object on this server */
    @Override
    public SapphirePolicy.SapphireClientPolicy createSapphireClientPolicy(
            String sapphireClientPolicy,
            SapphirePolicy.SapphireServerPolicy serverPolicy,
            SapphirePolicy.SapphireGroupPolicy groupPolicy,
            Annotation[] annotations)
            throws RemoteException, IllegalAccessException, InstantiationException,
                    ClassNotFoundException, KernelObjectNotCreatedException {
        /* Create the Client Policy Object on the remote kernel server */
        Class<?> sapphireClientPolicyClass = Class.forName(sapphireClientPolicy);
        KernelOID oid =
                GlobalKernelReferences.nodeServer.newKernelObject(sapphireClientPolicyClass);
        SapphirePolicy.SapphireClientPolicy clientPolicy;
        try {
            clientPolicy =
                    (SapphirePolicy.SapphireClientPolicy)
                            objectManager.lookupObject(oid).getObject();
        } catch (KernelObjectNotFoundException e) {
            throw new KernelObjectNotCreatedException(
                    "Failed to create kernel object for client policy");
        }

        clientPolicy.$__setKernelOID(oid);

        /* Link everything together */
        clientPolicy.setServer(serverPolicy);
        clientPolicy.onCreate(groupPolicy, annotations);
        logger.info("Created sapphire client policy object. Oid : " + oid.toString());
        return clientPolicy;
    }

    @Override
    public void deleteSapphireClientPolicy(KernelOID oid)
            throws RemoteException, KernelObjectNotFoundException {
        deleteKernelObject(oid);
    }

    public SapphirePolicy.SapphireServerPolicy getServerPolicyObjectReplicaId(
            SapphireReplicaID replicaId, EventHandler handler)
            throws RemoteException, SapphireObjectNotFoundException, KernelObjectNotFoundException {
        List<Object> policies = handler.getObjects();
        KernelOID oid;

        for (Object policy : policies) {
            oid = null;
            if (policy instanceof SapphirePolicy.SapphireServerPolicy) {
                oid = ((SapphirePolicy.SapphireServerPolicy) policy).$__getKernelOID();
            } else if (policy instanceof SapphirePolicy.SapphireGroupPolicy) {
                oid = ((SapphirePolicy.SapphireGroupPolicy) policy).$__getKernelOID();
            } else {
                logger.warning("unknown object instance");
                continue;
            }

            if (null == oid) {
                logger.warning("oid is null");
                continue;
            }

            KernelObject object = objectManager.lookupObject(oid);
            SapphirePolicy.SapphireServerPolicy serverPolicy =
                    (SapphirePolicy.SapphireServerPolicy) object.getObject();
            if (serverPolicy.getReplicaId().equals(replicaId)) {
                return serverPolicy;
            }
        }

        throw new KernelObjectNotFoundException("Kernel Object not found for oid");
    }

    /* Method called from grpc server (invoked by runtime process  */
    public SapphireReplicaID createInnerSapphireObject(
            String className,
            DMConfigInfo dmConfig,
            SapphireObjectID parentSapphireObjId,
            byte[] objectStream)
            throws RemoteException, SapphireObjectNotFoundException, KernelObjectNotFoundException,
                    ClassNotFoundException, KernelObjectNotCreatedException, InstantiationException,
                    IllegalAccessException, SapphireObjectCreationException {

        try {
            return createInnerSo(className, "java", dmConfig, parentSapphireObjId, objectStream);
        } catch (SapphireObjectNotFoundException e) {
            throw new SapphireObjectCreationException(
                    "Failed to create sapphire object : " + className + " : Trace : " + e);
        } catch (KernelObjectNotFoundException e) {
            throw new SapphireObjectCreationException(
                    "Failed to create sapphire object : " + className + " : Trace : " + e);
        }
    }

    /* Method called from grpc server(invoked by runtime process ) */
    public boolean deleteInnerSapphireObject(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException {
        GlobalKernelReferences.nodeServer.oms.deleteSapphireObject(sapphireObjId);
        return true;
    }

    /** ********** End: multilanguage related methods ************** */
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
        logger.info("heartbeat KernelServer" + srvinfo);
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
            System.out.println("[host ip] [host port] [oms ip] [oms port] [ [grpc-servingip] [port] ] [ [java-grpc-serverip] [port] ][region]");
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

        KernelServerImpl server = null;

        try {
            server = new KernelServerImpl(host, omsHost);
            KernelServer stub = (KernelServer) UnicastRemoteObject.exportObject(server, 0);
            Registry registry = LocateRegistry.createRegistry(Integer.parseInt(args[1]));
            registry.rebind("SapphireKernelServer", stub);

            if (args.length > 4) {
                //server.setRegion(args[args.length - 1]);
                server.setRegion(host.toString());
            } else {
                // server.setRegion("default");
                // TODO once we are sure we can comment below line & uncomment above line
                server.setRegion(host.toString());
            }

            if (args.length > 5) {
                int role = Integer.parseInt(args[args.length - 1]);
                if (1 == role) {
                    /* Role is client */
                    server.setRole(role);
                    server.setGrpcServerToApp(
                            new KernelGrpcServer(
                                    new InetSocketAddress(args[4], Integer.parseInt(args[5])), server, ServerInfo.ROLE_KERNEL_CLIENT));
                    server.getGrpcServerToApp().start();

                } else {
                    /* Role is server */
                    server.setGrpcServerToRuntime(
                            new KernelGrpcServer(
                                    new InetSocketAddress(args[4], Integer.parseInt(args[5])), server, ServerInfo.ROLE_KERNEL_SERVER));
                    server.getGrpcServerToRuntime().start();

                    if (args.length > 7) {
                    /* Start client for each runtime */
                        server.setGrpcClientToJavaRuntime(
                                new KernelGrpcClient(args[6], Integer.parseInt(args[7])));
                    }
                }
            }

            final ServerInfo srvinfo = new ServerInfo(host, server.getRegion(), server.getRole());
            oms.registerKernelServer(srvinfo);
            logger.info("Server ready!");
            System.out.println("Server ready!");

            /*
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
            */
            /* Start a thread that print memory stats */
            server.getMemoryStatThread().start();

        } catch (Exception e) {
            logger.severe("Cannot start Sapphire Kernel Server");
            e.printStackTrace();

            if (null != server.getGrpcServerToRuntime()) {
                server.getGrpcServerToRuntime().stop();
            }

            if (null != server.getGrpcServerToApp()) {
                server.getGrpcServerToApp().stop();
            }

            try {
                if (null != server.getGrpcClientToJavaRuntime()) {
                    server.getGrpcClientToJavaRuntime().shutdown();
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            //ksHeartbeatSendTimer.cancel();
        }
    }
}

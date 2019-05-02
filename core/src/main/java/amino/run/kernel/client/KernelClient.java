package amino.run.kernel.client;

import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.KernelObjectMigratingException;
import amino.run.kernel.common.KernelObjectNotFoundException;
import amino.run.kernel.common.KernelObjectStub;
import amino.run.kernel.common.KernelObjectStubNotCreatedException;
import amino.run.kernel.common.KernelRPC;
import amino.run.kernel.common.KernelRPCException;
import amino.run.kernel.common.metric.NodeMetric;
import amino.run.kernel.server.KernelObject;
import amino.run.kernel.server.KernelServer;
import amino.run.oms.OMSServer;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client-side object for making MicroService kernel RPCs.
 *
 * @author iyzhang
 */
public class KernelClient {
    /* Random bytes to measure the data transfer time */
    byte[] randomBytes = new byte[2048];

    /** Stub for the OMS */
    private OMSServer oms;
    /** List of hostnames matched to kernel server stubs */
    private ConcurrentHashMap<InetSocketAddress, KernelServer> servers;

    private ConcurrentHashMap<InetSocketAddress, NodeMetric> serverMetrics;

    private static final Logger logger = Logger.getLogger(KernelClient.class.getName());

    public ConcurrentHashMap<InetSocketAddress, NodeMetric> getServerMetrics() {
        return serverMetrics;
    }

    /**
     * Add a host to the list of hosts that we've contacted
     *
     * @param host
     */
    private KernelServer addHost(InetSocketAddress host) {
        try {
            Registry registry = LocateRegistry.getRegistry(host.getHostName(), host.getPort());
            KernelServer server = (KernelServer) registry.lookup("io.amino.run.kernelserver");
            servers.put(host, server);
            serverMetrics.put(host, new NodeMetric());
            return server;
        } catch (Exception e) {
            logger.severe(
                    String.format(
                            "Could not find kernel server on host: %s. Exception: %s", host, e));
        }
        return null;
    }

    private KernelServer getServer(InetSocketAddress host) {
        KernelServer server = servers.get(host);
        if (server == null) {
            server = addHost(host);
        }
        return server;
    }

    public void updateAvailableKernelServers(List<InetSocketAddress> kernelServers) {
        for (InetSocketAddress host : servers.keySet()) {
            if (!kernelServers.contains(host)) {
                servers.remove(host);
                serverMetrics.remove(host);
            }
        }

        for (InetSocketAddress host : kernelServers) {
            if (GlobalKernelReferences.nodeServer.getLocalHost().equals(host)) {
                continue;
            }
            KernelServer server = servers.get(host);
            if (server == null) {
                addHost(host);
            }
        }
    }

    public KernelClient(OMSServer oms) {
        this.oms = oms;
        servers = new ConcurrentHashMap<InetSocketAddress, KernelServer>();
        serverMetrics = new ConcurrentHashMap<InetSocketAddress, NodeMetric>();
        new Random().nextBytes(randomBytes);
    }

    private Object tryMakeKernelRPC(KernelServer server, KernelRPC rpc)
            throws KernelObjectNotFoundException, Exception {
        Object ret = null;
        try {
            ret = server.makeKernelRPC(rpc);
        } catch (KernelRPCException e) {
            if (!(e.getException() instanceof InvocationTargetException)) {
                /* Not an invocation exception */
                throw e.getException();
            }

            /* Invocation target exception wraps exception thrown by an invoked method or constructor */
            /* If invocation exception is with any exception,including runtime, app exceptions, unwrap it
            and throw. Else it is invocation exception with error. Throw the invocation exception as is */
            Throwable cause = e.getException().getCause();
            if (cause instanceof InvocationTargetException) {
                cause = cause.getCause();
            }

            throw (cause instanceof Exception) ? ((Exception) cause) : e.getException();

        } catch (KernelObjectMigratingException e) {
            Thread.sleep(100);
            throw new KernelObjectNotFoundException(
                    "Kernel object was migrating. Try again later.");
        }
        return ret;
    }

    private Object lookupAndTryMakeKernelRPC(KernelObjectStub stub, KernelRPC rpc)
            throws KernelObjectNotFoundException, Exception {
        InetSocketAddress host, oldHost = stub.$__getHostname();

        try {
            host = oms.lookupKernelObject(stub.$__getKernelOID());
        } catch (RemoteException e) {
            throw new KernelObjectNotFoundException("Could not find oms.");
        } catch (KernelObjectNotFoundException e) {
            throw new KernelObjectNotFoundException("This object does not exist!");
        }

        if (host.equals(oldHost)) {
            throw new Error("Kernel object should be on the server!");
        }

        stub.$__updateHostname(host);
        return tryMakeKernelRPC(getServer(host), rpc);
    }

    /**
     * Make an RPC to the kernel server.
     *
     * @param stub
     * @param rpc
     * @return
     * @throws RemoteException when kernel server cannot be contacted
     * @throws KernelObjectNotFoundException when kernel server cannot find object
     */
    public Object makeKernelRPC(KernelObjectStub stub, KernelRPC rpc)
            throws KernelObjectNotFoundException, Exception {
        InetSocketAddress host = stub.$__getHostname();
        logger.log(Level.FINE, "Making RPC to " + host.toString() + " RPC: " + rpc.toString());

        // Check whether this object is local.
        KernelServer server;
        if (host.equals(GlobalKernelReferences.nodeServer.getLocalHost())) {
            server = GlobalKernelReferences.nodeServer;
        } else {
            server = getServer(host);
        }

        // Call the server
        try {
            return tryMakeKernelRPC(server, rpc);
        } catch (KernelObjectNotFoundException e) {
            logger.warning(
                    String.format(
                            "Object was not found at the target location. Host:%s oid:%d method:%s",
                            host.toString(), rpc.getOID().getID(), rpc.getMethod()));
            return lookupAndTryMakeKernelRPC(stub, rpc);
        }
    }

    public void copyObjectToServer(InetSocketAddress host, KernelOID oid, KernelObject object)
            throws RemoteException, KernelObjectNotFoundException,
                    KernelObjectStubNotCreatedException, MicroServiceNotFoundException,
                    MicroServiceReplicaNotFoundException {
        getServer(host).copyKernelObject(oid, object);
    }

    /** Measure the server metrics */
    public void measureServerMetrics() {
        for (Map.Entry<InetSocketAddress, NodeMetric> entry : serverMetrics.entrySet()) {
            NodeMetric metric = entry.getValue();
            try {
                KernelServer server = getServer(entry.getKey());
                if (server == null) {
                    continue;
                }

                // TODO: Need to improve it further.
                /* Forward time = Request Serialization + Network Delay + Deserialization + Object/call Dispatch time
                + processing time + Optionally connection establishment time */
                /* Return time = Response Serialization + Network Delay + Deserialization + Get current nano time at end */
                /* Forward time would be comparatively higher than return time. But to avoid the clock sync issues
                between client and server, took the round trip time and half of it considered as latency
                 */
                long t1 = System.nanoTime();
                server.receiveHeartBeat();
                long t2 = System.nanoTime();

                // Measure data transfer rate by sending some arbitrary data
                server.receiveHeartBeat(randomBytes);
                long t3 = System.nanoTime();
                metric.latency = (t2 - t1) / 2;
                // TODO: This is data transfer time. Need to calculate the rate
                metric.rate = (t3 - t2) - (t2 - t1);

                logger.info(
                        String.format(
                                "To host: %s: T1 = %d, T2 = %d, T3 = %d, T2-T1=%d, T3-T2 = %d, metric.latency=%d",
                                entry.getKey(), t1, t2, t3, t2 - t1, t3 - t2, metric.latency));
            } catch (RemoteException e) {
                logger.warning(
                        String.format("Remote kernel server %s is not reachable", entry.getKey()));
            }
        }
    }
}

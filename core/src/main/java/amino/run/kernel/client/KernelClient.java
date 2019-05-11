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
import amino.run.kernel.metric.NodeMetric;
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
    private static final int RANDOM_DATA_SIZE = 2 * 1024;
    private static final byte[] randomBytes = new byte[RANDOM_DATA_SIZE];

    static {
        new Random().nextBytes(randomBytes);
    }

    /** Stub for the OMS */
    private OMSServer oms;

    /* Global reference count used to remove stale kernel servers from the servers and metrics maps. It is incremented
    at each metric measurement. Removes a server when it misses MAX_HEARTBEAT_MISS_ALLOWED continuously */
    private int referenceCount;
    private static final int MAX_HEARTBEAT_MISS_TIMES = 3;

    /* Class to hold remote kernel server info. This class is accessible only within this outer class(KernelClient) */
    private final class KernelServerInfo {
        private KernelServer remoteRef; /* Remote reference to the kernel server */
        /* Holds the current global reference count when metric measurement is successful for the server */
        private int referenceCount;

        private KernelServerInfo(KernelServer remoteRef) {
            this.remoteRef = remoteRef;
        }
    }

    /** Map of remote kernel server hostname to their kernel server info */
    private ConcurrentHashMap<InetSocketAddress, KernelServerInfo> servers;

    /* Map of remote kernel server hostname to their metrics. It is maintained independent from servers map, so that
    this map can be serialized and sent to OMS as is */
    private ConcurrentHashMap<InetSocketAddress, NodeMetric> metrics;

    private Logger logger = Logger.getLogger(KernelClient.class.getName());

    public ConcurrentHashMap<InetSocketAddress, NodeMetric> getMetrics() {
        return metrics;
    }

    /**
     * Adds remote kernel server information to cache for the given host
     *
     * @param host
     */
    private KernelServerInfo addHost(InetSocketAddress host) {
        try {
            Registry registry = LocateRegistry.getRegistry(host.getHostName(), host.getPort());
            KernelServer server = (KernelServer) registry.lookup("io.amino.run.kernelserver");
            KernelServerInfo serverInfo = new KernelServerInfo(server);
            serverInfo.referenceCount = referenceCount;
            servers.put(host, serverInfo);
            metrics.put(host, new NodeMetric());
            return serverInfo;
        } catch (Exception e) {
            logger.severe(
                    String.format(
                            "Could not find kernel server on host: %s. Exception: %s", host, e));
        }
        return null;
    }

    /**
     * Removes cached remote kernel server information for the given host, if it has missed
     * heartbeats for {@link amino.run.kernel.client.KernelClient#MAX_HEARTBEAT_MISS_TIMES} times
     *
     * @param host
     */
    private void removeHost(InetSocketAddress host) {
        KernelServerInfo server = servers.get(host);
        if (server != null) {
            if ((referenceCount - server.referenceCount >= MAX_HEARTBEAT_MISS_TIMES)) {
                servers.remove(host);
                metrics.remove(host);
            }
        }
    }

    private KernelServer getServer(InetSocketAddress host) {
        KernelServerInfo serverInfo = servers.get(host);
        if (serverInfo == null) {
            serverInfo = addHost(host);
        }
        return serverInfo != null ? serverInfo.remoteRef : null;
    }

    /**
     * Updates the local cache of remote kernel servers with given list of kernel servers
     *
     * @param kernelServers
     */
    public void updateAvailableKernelServers(List<InetSocketAddress> kernelServers) {
        for (InetSocketAddress host : kernelServers) {
            if (GlobalKernelReferences.nodeServer.getLocalHost().equals(host)) {
                /* OMS sends complete list of available kernel servers. Excluding local kernel server from it. Because,
                we neither have to get the remote reference to access local kernel server nor collect latency/data rate
                metrics for self */
                continue;
            }

            /* Check and add the server if not already present in servers map */
            getServer(host);
        }
    }

    public KernelClient(OMSServer oms) {
        this.oms = oms;
        servers = new ConcurrentHashMap<InetSocketAddress, KernelServerInfo>();
        metrics = new ConcurrentHashMap<InetSocketAddress, NodeMetric>();
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

    /**
     * Method to measure the kernel server metrics with respect to other remote servers and store
     * them in {@link amino.run.kernel.client.KernelClient#metrics}. This method is called from
     * timer expiry run method of {@link
     * amino.run.kernel.server.KernelServerImpl#startMetricsMeasurement()}. These collected metrics
     * are reported to OMS in heartBeats
     */
    public void measureServerMetrics() {
        referenceCount++;
        for (Map.Entry<InetSocketAddress, NodeMetric> entry : metrics.entrySet()) {
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

                /* Measure data transfer rate by sending some arbitrary data */
                server.receiveHeartBeat(randomBytes);
                long t3 = System.nanoTime();
                metric.latency = (t2 - t1) / 2;

                /* TODO: T3-T2(heartbeat with data) is sometimes smaller than T2-T1(empty heartbeat). Need to relook */
                /* Data transfer rate in Mbps */
                metric.rate =
                        ((RANDOM_DATA_SIZE * 8 * 1000.0 * 1000.0)
                                / (((t3 - t2) - (t2 - t1)) * 1024.0 * 1024.0));

                servers.get(entry.getKey()).referenceCount = referenceCount;

                logger.fine(
                        String.format(
                                "To host[%s]: T1 = %d, T2 = %d, T3 = %d, metric.latency=%dns, metric.rate=%fMbps",
                                entry.getKey(), t1, t2, t3, metric.latency, metric.rate));
            } catch (RemoteException e) {
                logger.warning(
                        String.format("Remote kernel server %s is not reachable", entry.getKey()));
                removeHost(entry.getKey());
            }
        }
    }
}

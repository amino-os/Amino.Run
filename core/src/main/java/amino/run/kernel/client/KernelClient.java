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
import amino.run.policy.util.ResettableTimer;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client-side object for making MicroService kernel RPCs.
 *
 * @author iyzhang
 */
public class KernelClient {
    private static final int MIN_METRIC_POLL_PERIOD_MS = 1000; /* Minimum metrics poll period */
    private static final int MAX_METRIC_POLL_PERIOD_MS = 512000; /* Maximum metrics poll period */
    private static final int STEP_SIZE_IN_BYTES = 1024; /* Step size in bytes */
    /* Random byte array used to send in heartbeats */
    private static final byte[] randomBytes = new byte[STEP_SIZE_IN_BYTES];

    static {
        new Random().nextBytes(randomBytes);
    }

    /* Data holder class used in heartbeats */
    public static class RandomData implements Serializable {
        private transient int len = STEP_SIZE_IN_BYTES; /* length of data to be sent to server */

        private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
            s.writeInt(len);
            for (int i = 0; i < len; i += STEP_SIZE_IN_BYTES) {
                /* Write the same byte array again as this data is useless on receiver end */
                s.write(randomBytes);
            }
        }

        private void readObject(java.io.ObjectInputStream s)
                throws java.io.IOException, ClassNotFoundException {
            len = s.readInt();
            for (int i = 0; i < len; i += STEP_SIZE_IN_BYTES) {
                /* Received data is useless. Just overwrites to same byte array */
                s.read(randomBytes);
            }
        }
    }

    /** Stub for the OMS */
    private OMSServer oms;

    /* Maximum number of continuous heartbeat miss allowed */
    private static final int MAX_HEARTBEAT_MISS_TIMES = 3;

    /* Class to hold remote kernel server info. This class is accessible only within this outer class(KernelClient) */
    private final class KernelServerInfo {
        private InetSocketAddress serverAddress; /* Host address of kernel server */
        private KernelServer remoteRef; /* Remote reference to the kernel server */
        /* Below reference counts are used to remove a server when it fails to send heartbeats for
        MAX_HEARTBEAT_MISS_ALLOWED times continuously */
        /* Global reference count is incremented whenever heartbeat is attempted */
        private int refCount;
        private int refCountOnHeartBeat; /* Reference count upon successful heartbeats */

        /* Minimum samples to consider data rates as consistent. */
        private static final int MIN_STABLE_DATA_RATE_TIMES = 10;
        private int stableDataRateTimes; /* Number of continuous stable data transfer rates */
        private RandomData data; /* Amount of random data sent to server in heartbeats */

        private int metricPollPeriod = MIN_METRIC_POLL_PERIOD_MS; /* Poll period */
        private NodeMetric metric; /* Node metric to server */
        private ResettableTimer metricsTimer; /* Metric timer for the server */

        private KernelServerInfo(
                InetSocketAddress serverAddress, KernelServer remoteRef, final NodeMetric metric) {
            this.serverAddress = serverAddress;
            this.remoteRef = remoteRef;
            this.metric = metric;
            data = new RandomData();
            metricsTimer =
                    new ResettableTimer(
                            new TimerTask() {
                                public void run() {
                                    measureMetrics(KernelServerInfo.this);
                                }
                            },
                            metricPollPeriod);
            metricsTimer.start();
        }
    }

    /** Map of remote kernel server hostname to their kernel server info */
    private ConcurrentHashMap<InetSocketAddress, KernelServerInfo> servers;

    /* Map of remote kernel server hostname to their metrics. It is maintained independent from servers map, so that
    this map can be serialized and sent to OMS as is */
    private ConcurrentHashMap<InetSocketAddress, NodeMetric> metrics;

    private static final Logger logger = Logger.getLogger(KernelClient.class.getName());

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
            NodeMetric metric = new NodeMetric();
            KernelServerInfo serverInfo = new KernelServerInfo(host, server, metric);
            servers.put(host, serverInfo);
            metrics.put(host, metric);
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
            if ((server.refCount - server.refCountOnHeartBeat >= MAX_HEARTBEAT_MISS_TIMES)) {
                servers.remove(host);
                metrics.remove(host);
            }
        }
    }

    /**
     * Gets the remote kernel server information for the given host
     *
     * @param host
     * @return Kernel Server Information
     */
    private KernelServerInfo getServerInfo(InetSocketAddress host) {
        KernelServerInfo serverInfo = servers.get(host);
        if (serverInfo == null) {
            serverInfo = addHost(host);
        }
        return serverInfo;
    }

    /**
     * Gets the RMI stub reference to remote kernel server for the given host
     *
     * @param host
     * @return RMI stub to remote kernel server
     */
    private KernelServer getServer(InetSocketAddress host) {
        KernelServerInfo serverInfo = getServerInfo(host);
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
     * them in {@link amino.run.kernel.client.KernelClient#metrics}. These collected metrics are
     * reported to OMS in heartBeats
     */
    private void measureMetrics(final KernelServerInfo serverInfo) {
        KernelServer server = serverInfo.remoteRef;
        int dataLength = serverInfo.data.len;
        NodeMetric metric = serverInfo.metric;
        int period = serverInfo.metricPollPeriod;
        serverInfo.refCount++;
        if (serverInfo.refCount == Integer.MAX_VALUE) {
            /* Reset to 1 when it is about to overflow */
            serverInfo.refCountOnHeartBeat = serverInfo.refCount = 1;
        }

        try {
            /* Forward time = Request Serialization + Network Delay + Deserialization + Object/call Dispatch time
            + processing time + Optionally connection establishment time */
            /* Return time = Response Serialization + Network Delay + Deserialization + Get current nano time at end */
            /* Forward time would be comparatively higher than return time. But to avoid the clock sync issues
            between client and server, took the round trip time(RTT) as latency
             */

            /* Make an empty heartbeat to ensure session is established and cached before measurement */
            server.receiveHeartBeat();

            /* Measure latency */
            long t1 = System.nanoTime();
            server.receiveHeartBeat();
            long t2 = System.nanoTime();

            /* Measure data transfer rate by sending some arbitrary data */
            server.receiveHeartBeat(serverInfo.data);
            long t3 = System.nanoTime();
            if (t3 - t2 < t2 - t1) {
                period = MIN_METRIC_POLL_PERIOD_MS;
                serverInfo.data.len += STEP_SIZE_IN_BYTES;
                serverInfo.stableDataRateTimes = 0;
                serverInfo.metricPollPeriod = period;
                serverInfo.metricsTimer.reset(period);
                logger.fine(String.format("Data size increased to : %d", serverInfo.data.len));
                // Ignore this sample
                return;
            } else if (serverInfo.stableDataRateTimes
                    >= KernelServerInfo.MIN_STABLE_DATA_RATE_TIMES) {
                /* Data rates are consistent. Double the poll period */
                period = period << 1;
                /* Limit max poll period to 512 sec */
                period = period > MAX_METRIC_POLL_PERIOD_MS ? MAX_METRIC_POLL_PERIOD_MS : period;
                serverInfo.stableDataRateTimes = 0;
                /* TODO: Can try reducing the length by step size and get to an optimum length required to send in
                heartbeats. But, need to ensure it do not toggle between the 2 consecutive values when optimal value is
                reached */
            }

            serverInfo.stableDataRateTimes++;
            metric.latency = (t2 - t1); // RTT

            /* DataLength divided by TimeDiff gives rate in Bytes/NanoSec. Multiply it by 8000 to get the data transfer
            rate in Megabits/Sec */
            metric.rate = ((dataLength * 8000.0) / ((t3 - t2) - (t2 - t1)));

            serverInfo.refCountOnHeartBeat = serverInfo.refCount;
            logger.fine(
                    String.format(
                            "To host[%s]: Latency=%dns, Data Rate=%fMbps, Data Length=%d",
                            serverInfo.serverAddress, metric.latency, metric.rate, dataLength));
        } catch (RemoteException e) {
            logger.warning(
                    String.format("Kernel server %s is not reachable", serverInfo.serverAddress));
            removeHost(serverInfo.serverAddress);
        }

        /* If the server is not deleted, restart the metrics timer with poll period */
        KernelServerInfo serverInMap = servers.get(serverInfo.serverAddress);
        if (serverInMap != null) {
            serverInMap.metricPollPeriod = period;
            serverInMap.metricsTimer.reset(period);
        }
    }
}

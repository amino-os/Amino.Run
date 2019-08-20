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
import amino.run.kernel.common.KernelServerNotFoundException;
import amino.run.kernel.metric.NodeMetric;
import amino.run.kernel.server.KernelObject;
import amino.run.kernel.server.KernelServer;
import amino.run.oms.OMSServer;
import amino.run.policy.util.ResettableTimer;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    /* Maximum metrics poll period. It is arbitrarily chosen as 128seconds(~2min)i.e., 2 power 7. Node metric poll
     * period starts from MIN_METRIC_POLL_PERIOD_MS and is gradually increased by multiples of 2, upon every consecutive
     * MIN_STABLE_DATA_RATE_TIMES samples. And poll period is limited to not exceed 128sec so that, even if a node moves
     * between networks of different speeds (e.g. from wifi to 3G or vice verse) or if it's link gets congested, then
     * polling node shall not take more than ~2min to detect the change */
    private static final int MAX_METRIC_POLL_PERIOD_MS = 128000;
    private static final int RANDOM_BYTE_LEN = 1024; /* Length of random bytes array */
    /* Random byte array used to send data with randomRPC method */
    private static final byte[] randomBytes = new byte[RANDOM_BYTE_LEN];

    static {
        new Random().nextBytes(randomBytes);
    }

    /**
     * Data holder class. Instance of it is used as argument to {@link
     * amino.run.kernel.server.KernelServer#randomDataRPC(RandomData)}. This class just has a length
     * field in it to specify the amount of data i.e., bytes to be sent. Custom serializer and
     * deserializer are used to encode and decode actual data bytes to be sent/received. This way,
     * though we actually have a static randomBytes array of RANDOM_BYTE_LEN i.e., 1024 we can
     * send/receive much more data of customized lengths( multiples of RANDOM_BYTE_LEN) and avoid
     * using array/string of huge size.
     */
    public static class RandomData implements Serializable {
        private transient int len = RANDOM_BYTE_LEN; /* Length of data to be sent to server */
        /**
         * Custom serializer to encode bytes depending on data length to output stream
         *
         * @param s
         * @throws java.io.IOException
         */
        private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
            s.writeInt(len);
            /* Actual size of randomBytes is RANDOM_BYTE_LEN. If the length to encode is more than RANDOM_BYTE_LEN, then
             * encode same randomBytes multiple times depending on length */
            for (int i = 0; i < len; i += RANDOM_BYTE_LEN) {
                /* Write the same byte array again as this data is useless on receiver end */
                s.write(randomBytes);
            }
        }

        /**
         * Custom deserializer to decode bytes depending on data length from input stream
         *
         * @param s
         * @throws java.io.IOException
         * @throws ClassNotFoundException
         */
        private void readObject(java.io.ObjectInputStream s)
                throws java.io.IOException, ClassNotFoundException {
            len = s.readInt();
            byte[] random = new byte[RANDOM_BYTE_LEN];
            for (int i = 0; i < len; i += RANDOM_BYTE_LEN) {
                /* Received data is useless. Just overwrites to same byte array */
                s.read(random);
            }
        }
    }

    /** Stub for the OMS */
    private OMSServer oms;

    /* Maximum number of consecutive RPC failures allowed */
    private static final int MAX_RPC_FAILURE = 3;

    /**
     * Class to hold remote kernel server info. This class is accessible only within its outer class
     * {@link amino.run.kernel.client.KernelClient}
     */
    private final class KernelServerInfo {
        private InetSocketAddress serverAddress; /* Host address of kernel server */
        private KernelServer remoteRef; /* Remote reference to the kernel server */
        /* Consecutive failed RPC count. This count is used to remove a server when RPCs failed for MAX_RPC_FAILURE times consecutively. */
        private int failedRPCCount;

        /* Minimum samples to consider data rates as consistent. */
        private static final int MIN_STABLE_DATA_RATE_TIMES = 10;
        private int stableDataRateTimes; /* Number of consecutive stable data transfer rates */
        /* Amount of random data sent to server with randomDataRPC method invocation */
        private RandomData data;

        private int metricPollPeriod = MIN_METRIC_POLL_PERIOD_MS; /* Poll period */
        private NodeMetric metric; /* Node metric to server */
        private ResettableTimer metricsTimer; /* Metric timer for the server */

        private KernelServerInfo(InetSocketAddress serverAddress, KernelServer remoteRef) {
            this.serverAddress = serverAddress;
            this.remoteRef = remoteRef;
            this.metric = new NodeMetric();
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

    private static final Logger logger = Logger.getLogger(KernelClient.class.getName());

    /**
     * Adds remote kernel server information to cache for the given host
     *
     * @param host
     */
    private KernelServerInfo addHost(InetSocketAddress host) throws KernelServerNotFoundException {
        Registry registry;
        String error;
        try {
            registry = LocateRegistry.getRegistry(host.getHostName(), host.getPort());
        } catch (RemoteException e) {
            error = String.format("Could not locate registry on host: %s. %s", host, e);
            throw new KernelServerNotFoundException(error, e);
        }

        try {
            KernelServer server = (KernelServer) registry.lookup("io.amino.run.kernelserver");
            KernelServerInfo serverInfo = new KernelServerInfo(host, server);
            servers.put(host, serverInfo);
            return serverInfo;
        } catch (Exception e) {
            error = String.format("Service lookup failed in registry of host: %s. %s", host, e);
            throw new KernelServerNotFoundException(error, e);
        }
    }

    /**
     * Removes cached remote kernel server information for the given host, if RPCs to server has
     * failed for {@link amino.run.kernel.client.KernelClient#MAX_RPC_FAILURE} times consecutively
     *
     * @param host
     */
    private void removeHost(InetSocketAddress host) {
        KernelServerInfo server = servers.get(host);
        if (server != null) {
            if ((server.failedRPCCount > MAX_RPC_FAILURE)) {
                servers.remove(host);
            }
        }
    }

    /**
     * Gets the remote kernel server information for the given host
     *
     * @param host
     * @return Kernel Server Information
     */
    private KernelServerInfo getServerInfo(InetSocketAddress host)
            throws KernelServerNotFoundException {
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
    private KernelServer getServer(InetSocketAddress host) throws KernelServerNotFoundException {
        KernelServerInfo serverInfo = getServerInfo(host);
        return serverInfo.remoteRef;
    }

    /**
     * Writes node metrics to the output stream
     *
     * @param s
     * @throws IOException
     */
    public void writeMetrics(java.io.ObjectOutputStream s) throws IOException {
        int metricsLen = servers.size();
        s.writeInt(metricsLen);
        if (metricsLen == 0) {
            return;
        }

        Iterator<KernelClient.KernelServerInfo> iterator = servers.values().iterator();
        while (iterator.hasNext() && metricsLen-- > 0) {
            KernelClient.KernelServerInfo server = iterator.next();

            /* Encode the values only if is measured since last reported time */
            if (server.metric.latency != 0) {
                s.writeObject(server.serverAddress);
                s.writeObject(server.metric);

                /* Reset the node metrics values after encoding */
                server.metric.latency = 0;
                server.metric.rate = 0;
            }
        }
        /* Delimiter */
        s.writeObject(null);
        s.writeObject(null);
    }

    /**
     * Reads node metrics from the input stream
     *
     * @param s
     * @return
     * @throws java.io.IOException
     * @throws ClassNotFoundException
     */
    public Map<InetSocketAddress, NodeMetric> readMetrics(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        int metricsLen = s.readInt();
        ConcurrentHashMap<InetSocketAddress, NodeMetric> metrics =
                new ConcurrentHashMap<InetSocketAddress, NodeMetric>(metricsLen);
        if (metricsLen == 0) {
            return metrics;
        }
        while (metricsLen-- > 0) {
            InetSocketAddress host = (InetSocketAddress) s.readObject();
            NodeMetric metric = (NodeMetric) s.readObject();
            if (host == null || metric == null) {
                return metrics;
            }
            metrics.put(host, metric);
        }
        return metrics;
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
                 * we neither have to get the remote reference to access local kernel server nor collect latency/data
                 * rates for self. */
                continue;
            }

            try {
                /* Check and add the server if not already present in servers map */
                getServer(host);
            } catch (KernelServerNotFoundException e) {
                logger.warning(e.toString());
            }
        }
    }

    public KernelClient(OMSServer oms) {
        this.oms = oms;
        servers = new ConcurrentHashMap<InetSocketAddress, KernelServerInfo>();
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
                    MicroServiceReplicaNotFoundException, KernelServerNotFoundException {
        getServer(host).copyKernelObject(oid, object);
    }

    /**
     * Method to measure the kernel server metrics with respect to other remote servers and store
     * them in {@link amino.run.kernel.client.KernelClient.KernelServerInfo#metric}. These collected
     * metrics are reported to OMS in heartBeats
     */
    private void measureMetrics(final KernelServerInfo serverInfo) {

        try {
            /* Forward time = Request Serialization + Network Delay + Deserialization + Object/call Dispatch time +
             * processing time.
             * Return time = Response Serialization + Network Delay + Deserialization + Get current nano time at end.
             */

            /* Make an empty RPC to ensure session is established and cached before measurement */
            serverInfo.remoteRef.emptyRPC();

            /* Measure latency */
            long t1 = System.nanoTime();
            serverInfo.remoteRef.emptyRPC();
            long t2 = System.nanoTime();

            /* Measure data transfer rate by sending some arbitrary data */
            serverInfo.remoteRef.randomDataRPC(serverInfo.data);
            long t3 = System.nanoTime();
            if (t3 - t2 < t2 - t1) {
                /* Probably data size is too small relative to line latency. Double the data size and try at next period */
                serverInfo.data.len *= 2;
                serverInfo.stableDataRateTimes = 0;
                serverInfo.metricPollPeriod = MIN_METRIC_POLL_PERIOD_MS;
                serverInfo.metricsTimer.reset(MIN_METRIC_POLL_PERIOD_MS);
                logger.fine(String.format("Data size increased to : %d", serverInfo.data.len));
                // Ignore this sample
                return;
            } else if (serverInfo.stableDataRateTimes
                    >= KernelServerInfo.MIN_STABLE_DATA_RATE_TIMES) {
                /* Data rates are consistent. Double the poll period. Limit maximum poll period to
                MAX_METRIC_POLL_PERIOD_MS */
                if (serverInfo.metricPollPeriod < MAX_METRIC_POLL_PERIOD_MS) {
                    serverInfo.metricPollPeriod <<= 1;
                }
                serverInfo.stableDataRateTimes = 0;
                /* TODO: Can try reducing the length and get to an optimum length required to send */
            }

            serverInfo.stableDataRateTimes++;
            serverInfo.metric.latency = (t2 - t1); // RTT

            /* DataLength divided by TimeDiff gives rate in Bytes/NanoSec. Multiply it by 10 power 9 to get the data
            transfer rate in Bytes/Sec */
            serverInfo.metric.rate =
                    (long) (serverInfo.data.len * (1000000000.0 / ((t3 - t2) - (t2 - t1))));

            /* Reset consecutive failed RPC count to 0 upon successful RPC */
            serverInfo.failedRPCCount = 0;
            logger.fine(
                    String.format(
                            "To host[%s]: Latency=%dns, Data Rate=%dBytes/Sec, Data Length=%d",
                            serverInfo.serverAddress,
                            serverInfo.metric.latency,
                            serverInfo.metric.rate,
                            serverInfo.data.len));
        } catch (RemoteException e) {
            logger.warning(
                    String.format("Kernel server %s is not reachable", serverInfo.serverAddress));
            /* Increment consecutive failed RPC count */
            serverInfo.failedRPCCount++;
            removeHost(serverInfo.serverAddress);
        }

        /* If the server is not deleted, restart the metrics timer with poll period */
        KernelServerInfo serverInMap = servers.get(serverInfo.serverAddress);
        if (serverInMap != null) {
            serverInMap.metricsTimer.reset(serverInfo.metricPollPeriod);
        }
    }
}

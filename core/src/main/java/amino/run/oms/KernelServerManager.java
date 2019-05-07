package amino.run.oms;

import amino.run.app.MicroServiceSpec;
import amino.run.app.NodeSelectorSpec;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.KernelServerNotFoundException;
import amino.run.kernel.common.ServerInfo;
import amino.run.kernel.server.KernelServer;
import amino.run.oms.metrics.KernelServerMetric;
import amino.run.policy.util.ResettableTimer;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages MicroService kernel servers. Tracks which servers are up, which regions each server
 * belongs to, etc.
 *
 * @author iyzhang
 */
public class KernelServerManager {
    /* Kernel Server information */
    private final class KernelServerInfo {
        private ServerInfo config; // Registration information of kernel server
        private ResettableTimer heartBeatTimer; // HeartBeat timer
        private KernelServer remoteRef; // Remote reference to kernel server
        private KernelServerMetric metric; // Metrics

        private KernelServerInfo(
                ServerInfo config, KernelServer remoteRef, ResettableTimer heartBeatTimer, KernelServerMetric metric) {
            this.config = config;
            this.heartBeatTimer = heartBeatTimer;
            this.remoteRef = remoteRef;
            this.metric = metric;
        }
    }

    private static final Logger logger = Logger.getLogger(KernelServerManager.class.getName());
    private ConcurrentHashMap<InetSocketAddress, KernelServerInfo> servers;
    private ConcurrentHashMap<String, ArrayList<InetSocketAddress>> regions;
    private static final Random randgen = new Random();

    public KernelServerManager() {
        servers = new ConcurrentHashMap<InetSocketAddress, KernelServerInfo>();
        regions = new ConcurrentHashMap<String, ArrayList<InetSocketAddress>>();
    }

    void stopHeartBeat(ServerInfo srvInfo) {
        logger.info(
                String.format(
                        "Heartbeat not received from kernel server: %s in region: %s",
                        srvInfo.getHost(), srvInfo.getRegion()));
        removeKernelServer(srvInfo);
    }

    public void removeKernelServer(ServerInfo srvInfo) {
        InetSocketAddress host = srvInfo.getHost();
        String region = srvInfo.getRegion();
        KernelServerInfo kernelServerInfo = servers.remove(host);
        kernelServerInfo.heartBeatTimer.cancel();

        // Removing from the regions map
        ArrayList<InetSocketAddress> serversInRegion = regions.get(region);
        if (serversInRegion == null) {
            logger.severe(
                    String.format("Kernel server: %s do not exist in region: %s", host, region));
            return;
        }
        serversInRegion.remove(host);
        // If there are no servers in the region, remove region itself.
        if (serversInRegion.isEmpty()) {
            regions.remove(region);
        }
    }

    public void registerKernelServer(ServerInfo info) throws RemoteException, NotBoundException {
        InetSocketAddress host = info.getHost();
        String region = info.getRegion();
        Registry registry = LocateRegistry.getRegistry(host.getHostName(), host.getPort());
        KernelServer server = (KernelServer) registry.lookup("io.amino.run.kernelserver");

        ArrayList<InetSocketAddress> serversInRegion = regions.get(region);
        if (null == serversInRegion) {
            serversInRegion = new ArrayList<InetSocketAddress>();
        }
        serversInRegion.add(host);
        regions.put(info.getRegion(), serversInRegion);
        final ServerInfo srvInfo = info;
        ResettableTimer heartBeatTimer =
                new ResettableTimer(
                        new TimerTask() {
                            public void run() {
                                /* If we don't receive a heartbeat from this kernel server, remove it from the map */
                                stopHeartBeat(srvInfo);
                            }
                        },
                        OMSServer.KS_HEARTBEAT_TIMEOUT);
        heartBeatTimer.start();
        KernelServerInfo oldServer =
                servers.put(host, new KernelServerInfo(info, server, heartBeatTimer));
        if (oldServer != null) {
            oldServer.heartBeatTimer.cancel();
        }

        logger.info(String.format("Registered new kernel server: %s in region %s", host, region));
    }

    public void receiveHeartBeat(ServerInfo srvinfo) throws KernelServerNotFoundException {
        InetSocketAddress host = srvinfo.getHost();
        String region = srvinfo.getRegion();
        logger.fine(
                String.format(
                        "Received HeartBeat from kernel server: %s in region %s", host, region));

        KernelServerInfo kernelServerInfo = servers.get(host);
        if (kernelServerInfo != null) {
            kernelServerInfo.heartBeatTimer.reset();
            kernelServerInfo.metric.updateMetric(srvinfo.metrics);
            return;
        }

        String message = String.format("Kernel server: %s do not exist in region %s", host, region);
        logger.severe(message);
        throw new KernelServerNotFoundException(message);
    }

    /**
     * Returns a list of addresses of servers whose labels match the given {@code NodeSelectorSpec}
     *
     * @param spec {@code NodeSelectorSpec} instance
     * @return a list of {@code InetSocketAddress}
     */
    public List<InetSocketAddress> getServers(NodeSelectorSpec spec) {
        List<InetSocketAddress> nodes = new ArrayList<InetSocketAddress>();
        for (Map.Entry<InetSocketAddress, KernelServerInfo> entry : servers.entrySet()) {
            if (entry.getValue().config.matchNodeSelectorSpec(spec)) {
                nodes.add(entry.getKey());
            }
        }
        return nodes;
    }

    public ArrayList<String> getRegions() {
        return new ArrayList<String>(regions.keySet());
    }

    public KernelServer getServer(InetSocketAddress address) {
        if (address.equals(GlobalKernelReferences.nodeServer.getLocalHost())) {
            return GlobalKernelReferences.nodeServer;
        }

        KernelServerInfo kernelServerInfo = servers.get(address);
        if (kernelServerInfo == null) {
            logger.warning(String.format("Kernel server: %s is not registered", address));
            return null;
        }

        return kernelServerInfo.remoteRef;
    }

    /**
     * Gets the best suitable server from the given NodeSelector
     *
     * @param spec
     * @return
     */
    public InetSocketAddress getBestSuitableServer(MicroServiceSpec spec) {
        NodeSelectorSpec nodeSelector = null;
        if (spec != null) {
            nodeSelector = spec.getNodeSelectorSpec();
        }
        // If nodeSelector is null then return the list of address of all kernel servers
        List<InetSocketAddress> hosts = getServers(nodeSelector);

        if (hosts.size() <= 0) {
            logger.severe("Could not find kernel server for the given requirements");
            return null;
        }
        // In future we can consider some other specific things to select the
        // best one among the list
        return hosts.get(randgen.nextInt(hosts.size()));
    }

    /**
     * Gets the kernel server CPU information
     *
     * @param host
     * @return
     */
    public int getKernelServerCPU(InetSocketAddress host) {
        return servers.get(host).config.cpu;
    }
}

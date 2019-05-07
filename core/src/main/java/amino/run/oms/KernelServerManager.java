package amino.run.oms;

import amino.run.app.MicroServiceSpec;
import amino.run.app.NodeSelectorSpec;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.KernelServerNotFoundException;
import amino.run.kernel.common.ServerInfo;
import amino.run.kernel.server.KernelServer;
import amino.run.policy.util.ResettableTimer;
import java.net.InetSocketAddress;
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
    public static class KernelServerInfo {
        private ServerInfo config; // Registration information of kernel server
        private ResettableTimer heartBeatTimer; // HeartBeat timer
        private KernelServer remoteRef; // Remote reference to kernel server

        public KernelServerInfo(ServerInfo config, ResettableTimer heartBeatTimer) {
            this.config = config;
            this.heartBeatTimer = heartBeatTimer;
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
        KernelServerInfo kernelServerInfo = servers.remove(srvInfo.getHost());
        kernelServerInfo.heartBeatTimer.cancel();

        // Removing from the regions map
        ArrayList<InetSocketAddress> serverList = regions.get(srvInfo.getRegion());
        if (serverList == null) {
            logger.severe(
                    String.format(
                            "Kernel server: %s do not exist in region: %s",
                            srvInfo.getHost(), srvInfo.getRegion()));
            return;
        }
        serverList.remove(srvInfo.getHost());
        // If there are no servers in the region, remove region itself.
        if (serverList.size() == 0) {
            regions.remove(srvInfo.getRegion());
        }
    }

    public void registerKernelServer(ServerInfo info) {
        logger.info(
                String.format(
                        "Registered new kernel server: %s in region %s",
                        info.getHost(), info.getRegion()));
        ArrayList<InetSocketAddress> serverList = regions.get(info.getRegion());
        if (null == serverList) {
            serverList = new ArrayList<InetSocketAddress>();
        }
        serverList.add(info.getHost());
        regions.put(info.getRegion(), serverList);

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
        servers.put(info.getHost(), new KernelServerInfo(info, heartBeatTimer));
    }

    public void receiveHeartBeat(ServerInfo srvinfo) throws KernelServerNotFoundException {
        logger.fine(
                String.format(
                        "Received HeartBeat from kernel server: %s in region %s",
                        srvinfo.getHost(), srvinfo.getRegion()));

        KernelServerInfo kernelServerInfo = servers.get(srvinfo.getHost());
        if (kernelServerInfo != null) {
            kernelServerInfo.heartBeatTimer.reset();
            return;
        }

        String message =
                String.format(
                        "Kernel server: %s do not exist in region %s",
                        srvinfo.getHost(), srvinfo.getRegion());
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

        if (kernelServerInfo.remoteRef != null) {
            return kernelServerInfo.remoteRef;
        } else {
            KernelServer server = null;
            try {
                Registry registry =
                        LocateRegistry.getRegistry(address.getHostName(), address.getPort());
                server = (KernelServer) registry.lookup("io.amino.run.kernelserver");
                kernelServerInfo.remoteRef = server;
            } catch (Exception e) {
                logger.severe(
                        String.format(
                                "Could not find kernel server: %s. Exception: %s", address, e));
            }
            return server;
        }
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
}

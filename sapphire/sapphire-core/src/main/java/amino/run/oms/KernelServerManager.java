package amino.run.oms;

import amino.run.app.MicroServiceSpec;
import amino.run.app.NodeSelectorSpec;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.KernelServerNotFoundException;
import amino.run.kernel.common.ServerInfo;
import amino.run.kernel.server.KernelServer;
import amino.run.policy.util.ResettableTimer;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages MicroService kernel servers. Tracks which servers are up, which regions each server
 * belongs to, etc.
 *
 * @author iyzhang
 */
public class KernelServerManager {
    Logger logger = Logger.getLogger(KernelServerManager.class.getName());

    private ConcurrentHashMap<InetSocketAddress, KernelServer> servers;
    private ConcurrentHashMap<String, ArrayList<InetSocketAddress>> regions;
    private ConcurrentHashMap<InetSocketAddress, ResettableTimer> ksHeartBeatTimers;
    private Set<ServerInfo> serverInfos;
    private static final Random randgen = new Random();

    public KernelServerManager() {
        serverInfos = Collections.newSetFromMap(new ConcurrentHashMap<>());
        servers = new ConcurrentHashMap<InetSocketAddress, KernelServer>();
        regions = new ConcurrentHashMap<String, ArrayList<InetSocketAddress>>();
        ksHeartBeatTimers = new ConcurrentHashMap<InetSocketAddress, ResettableTimer>();
    }

    void stopHeartBeat(ServerInfo srvInfo) {
        logger.info(
                "Heartbeat not received from region:"
                        + srvInfo.getRegion()
                        + "host:"
                        + srvInfo.getHost().toString());

        ResettableTimer ksHeartBeatTimer = ksHeartBeatTimers.get(srvInfo.getHost());
        ksHeartBeatTimer.cancel();
        ksHeartBeatTimers.remove(srvInfo.getHost());
        removeKernelServer(srvInfo);
    }

    public void removeKernelServer(ServerInfo srvInfo) {
        // removing from the servers list
        servers.remove(srvInfo.getHost());
        serverInfos.remove(srvInfo);

        // removing from the regions map
        ArrayList<InetSocketAddress> serverList = regions.get(srvInfo.getRegion());
        if (serverList == null) {
            logger.severe(
                    "region does not exist for removeKernelServer: "
                            + srvInfo.getHost().toString()
                            + " in region "
                            + srvInfo.getRegion());
            return;
        }
        serverList.remove(srvInfo.getHost());
        // if no servers in the region remove full entry from the map
        if (serverList.size() == 0) {
            regions.remove(srvInfo.getRegion());
        }
    }

    public void registerKernelServer(ServerInfo info) throws RemoteException, NotBoundException {
        logger.info(
                "New kernel server: "
                        + info.getHost().toString()
                        + " in region "
                        + info.getRegion());

        serverInfos.add(info);
        ArrayList<InetSocketAddress> serverList = regions.get(info.getRegion());

        if (null == serverList) {
            serverList = new ArrayList<InetSocketAddress>();
        }
        serverList.add(info.getHost());
        regions.put(info.getRegion(), serverList);

        final ServerInfo srvInfo = info;
        ResettableTimer ksHeartBeatTimer =
                new ResettableTimer(
                        new TimerTask() {
                            public void run() {
                                // If we don't receive a heartbeat from this kernel server, remove
                                // that from the list
                                stopHeartBeat(srvInfo);
                            }
                        },
                        OMSServer.KS_HEARTBEAT_TIMEOUT);

        ksHeartBeatTimers.put(info.getHost(), ksHeartBeatTimer);
        ksHeartBeatTimer.start();
    }

    public void heartbeatKernelServer(ServerInfo srvinfo)
            throws RemoteException, NotBoundException, KernelServerNotFoundException {
        logger.fine(
                "heartbeat from KernelServer: "
                        + srvinfo.getHost().toString()
                        + " in region "
                        + srvinfo.getRegion());

        ArrayList<InetSocketAddress> serverList = regions.get(srvinfo.getRegion());

        if (null == serverList) {
            logger.severe(
                    "region does not exist for heartbeat KernelServer: "
                            + srvinfo.getHost().toString()
                            + " in region "
                            + srvinfo.getRegion());
            throw new KernelServerNotFoundException("region does not exist");
        }
        if (serverList.contains(srvinfo.getHost())) {
            ResettableTimer ksHeartBeatTimer = ksHeartBeatTimers.get(srvinfo.getHost());
            ksHeartBeatTimer.reset();
            return;
        }
        logger.severe(
                "Host does not exist for heartbeat KernelServer: "
                        + srvinfo.getHost().toString()
                        + " in region "
                        + srvinfo.getRegion());
        throw new KernelServerNotFoundException("region exist but host does not exist");
    }

    /**
     * Returns a list of addresses of servers whose labels match the given {@code NodeSelectorSpec}
     *
     * @param spec {@code NodeSelectorSpec} instance
     * @return a list of {@code InetSocketAddress}
     */
    public List<InetSocketAddress> getServers(NodeSelectorSpec spec) {
        List<InetSocketAddress> nodes = new ArrayList<>();
        Set<String> orLabels = null;
        Set<String> andLabels = null;
        if (null != spec) {
            orLabels = spec.getOrLabels();
            andLabels = spec.getAndLabels();
        }
        for (ServerInfo s : serverInfos) {
            if (s.containsAll(andLabels) && s.containsAny(orLabels)) {
                nodes.add(s.getHost());
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

        if (servers.containsKey(address)) {
            return servers.get(address);
        } else {
            KernelServer server = null;
            try {
                Registry registry =
                        LocateRegistry.getRegistry(address.getHostName(), address.getPort());
                server = (KernelServer) registry.lookup("SapphireKernelServer");
                servers.put(address, server);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Could not find kernel server: " + e.toString());
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
        // if nodeSelector is null then returns all the kernelserver's addresses
        List<InetSocketAddress> hosts = getServers(nodeSelector);

        if (hosts.size() <= 0) {
            logger.log(Level.SEVERE, "Could not find kernel server forthe given requirements");
            return null;
        }
        // In future we can consider some other specific things to select the
        // best one among the list
        return hosts.get(randgen.nextInt(hosts.size()));
    }
}

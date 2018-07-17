package sapphire.oms;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import sapphire.kernel.common.KernelServerNotFoundException;
import sapphire.kernel.common.ServerInfo;
import sapphire.kernel.server.KernelServer;
import sapphire.policy.util.ResettableTimer;

/**
 * Manages Sapphire kernel servers. Tracks which servers are up, which regions each server belongs
 * to, etc.
 *
 * @author iyzhang
 */
public class KernelServerManager {
    Logger logger = Logger.getLogger("sapphire.oms.KernelServerManager");

    private ConcurrentHashMap<InetSocketAddress, KernelServer> servers;
    private ConcurrentHashMap<String, ArrayList<InetSocketAddress>> regions;
    private ConcurrentHashMap<InetSocketAddress, ResettableTimer> ksHeartBeatTimers;

    public KernelServerManager() throws IOException, NotBoundException, JSONException {
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
        logger.info(
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

    /** */
    public ArrayList<InetSocketAddress> getServers() {
        ArrayList<InetSocketAddress> nodes = new ArrayList<InetSocketAddress>();

        for (ArrayList<InetSocketAddress> addresses : this.regions.values()) {
            for (InetSocketAddress address : addresses) {
                nodes.add(address);
            }
        }

        return nodes;
    }

    public ArrayList<String> getRegions() {
        return new ArrayList<String>(regions.keySet());
    }

    public KernelServer getServer(InetSocketAddress address) {
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

    public InetSocketAddress getServerInRegion(String region) {
        return regions.get(region).get(0);
    }

    /**
     * Gets all the servers in the region
     *
     * @param region
     * @return list of kernel server host addresses in the given region otherwise null
     */
    public ArrayList<InetSocketAddress> getServersInRegion(String region) {
        return regions.get(region);
    }
}

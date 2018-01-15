package sapphire.oms;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;

import sapphire.kernel.server.KernelServer;

/**
 * Manages Sapphire kernel servers. Tracks which servers are up, which regions each server belongs to, etc.
 * @author iyzhang
 * TODO (smoon, 1/12/2018): regions is not used in the current code path; therefore, region is defined by IP address instead. *
 */
public class KernelServerManager {
	Logger logger = Logger.getLogger("sapphire.oms.KernelServerManager");

	private ConcurrentHashMap<InetSocketAddress, KernelServer> servers;
	private ConcurrentHashMap<String, ArrayList<InetSocketAddress>> regions;

	public KernelServerManager() throws IOException, NotBoundException, JSONException {
		servers = new ConcurrentHashMap<InetSocketAddress, KernelServer>();
		regions = new ConcurrentHashMap<String, ArrayList<InetSocketAddress>>();
	}
	
	public void registerKernelServer(InetSocketAddress address) throws RemoteException, NotBoundException {
	    // TODO (smoon, 1/12/2018): For now, put address as a region name when region name is null but this should be changed later.
        this.registerKernelServer(address, address.toString());
	}

	/**
	 * This method is currently not used in any code path but added in case region value needs to be set.
     * @author smoon
 	 * @param address
	 * @param region
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	public void registerKernelServer(InetSocketAddress address, String region) throws RemoteException, NotBoundException {
	    ArrayList<InetSocketAddress> addresses;
	    logger.info("New kernel server: " + address.toString() + " Region: " + region);

	    addresses = regions.containsKey(region)? regions.get(region): new ArrayList<InetSocketAddress>();

        if (!addresses.contains(address)) {
		    addresses.add(address);
		    regions.put(region, addresses);
        }

        // TODO (smoon, 1/12/2018): Server is registered when getServer is called; therefore, register to servers is not needed now.
        // However, this can change. Leaving the server object addition code in case this changes in the future.
        // servers.putIfAbsent(address, null);
	}

    public ArrayList<InetSocketAddress> getServers() {
        return new ArrayList<InetSocketAddress>(servers.keySet());
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
    			Registry registry = LocateRegistry.getRegistry(address.getHostName(), address.getPort());
    			server = (KernelServer) registry.lookup("SapphireKernelServer");
    			servers.put(address, server);
    		} catch (Exception e) {
    			logger.log(Level.SEVERE, "Could not find kernel server: "+e.toString());
    		}
			return server;
    	}
    }
    
    public InetSocketAddress getServerInRegion(String region) {
    	return regions.get(region).get(0);
    }
}

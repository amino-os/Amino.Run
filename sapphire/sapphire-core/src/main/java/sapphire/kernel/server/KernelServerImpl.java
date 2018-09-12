package sapphire.kernel.server;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import sapphire.app.AppEntryPoint;
import sapphire.common.AppObjectStub;
import sapphire.kernel.client.KernelClient;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectMigratingException;
import sapphire.kernel.common.KernelObjectNotCreatedException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.common.KernelRPC;
import sapphire.kernel.common.KernelRPCException;
import sapphire.kernel.common.ServerInfo;
import sapphire.oms.OMSServer;
import sapphire.policy.DefaultSapphirePolicyUpcallImpl;
import sapphire.policy.SapphirePolicy;
import sapphire.policy.SapphirePolicyContainer;
import sapphire.policy.SapphirePolicyLibrary.*;

/** 
 * Sapphire Kernel Server. Runs on every Sapphire node, knows how to talk to the OMS, handles RPCs and has a client for making RPCs.
 * 
 * @author iyzhang
 *
 */

public class KernelServerImpl implements KernelServer{
	private static Logger logger = Logger.getLogger("sapphire.kernel.server.KernelServerImpl");
	private InetSocketAddress host;
	private String region;
	/** manager for kernel objects that live on this server */
	private KernelObjectManager objectManager;
	/** stub for the OMS */
	public static OMSServer oms;
	/** local kernel client for making RPCs */
	private KernelClient client;
	
	public KernelServerImpl(InetSocketAddress host, InetSocketAddress omsHost) {
		OMSServer oms = null;
		try {
			Registry registry = LocateRegistry.getRegistry(omsHost.getHostName(), omsHost.getPort());
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

	/** RPC INTERFACES **/
	
	/**
	 * Invoke an RPC on this kernel server. This is a public RMI interface.
	 * 
	 * @param rpc All of the information about the RPC, the object id, the method and arguments
	 * @return the return value from the method invocation
	 */
	@Override
	public Object makeKernelRPC(KernelRPC rpc) throws RemoteException, KernelObjectNotFoundException, KernelObjectMigratingException, KernelRPCException {
		KernelObject object = null;
		Object ret = null;

		object = objectManager.lookupObject(rpc.getOID());

		logger.log(Level.FINE, "Invoking RPC on Kernel Object with OID: " + rpc.getOID() + "with rpc:" + rpc.getMethod() + " params: " + rpc.getParams().toString());

		try {
			ret = object.invoke(rpc.getMethod(), rpc.getParams());
		} catch (Exception e) {
			throw new KernelRPCException(e);
		}

		return ret;
	}
	
	/**
	 * Move a kernel object to this server.
	 * 
	 * @param oid the kernel object id
	 * @param object the kernel object to be stored on this server
	 */
	public void copyKernelObject(KernelOID oid, KernelObject object) throws RemoteException, KernelObjectNotFoundException {
		System.out.println("Adding object " + oid + " to this server at " + host.getAddress() + ":" + host.getPort());
		objectManager.addObject(oid, object);

		// to add KOs of in-chained server policy to local object manager
		Serializable realObj = object.getObject();
		if (realObj instanceof SapphireServerPolicyLibrary) {
			SapphireServerPolicyLibrary outmostSP = (SapphireServerPolicyLibrary)realObj;
			for (SapphirePolicyContainer spContainer: outmostSP.getProcessedPolicies()) {
				SapphireServerPolicyLibrary sp = spContainer.getServerPolicy();
				KernelOID koid = sp.$__getKernelOID();
				if (koid != oid) {	// except for the outmost which had already been added
					// assuming no oid collision
					this.objectManager.addObject(koid, new KernelObject(sp));
				}
			}
		}

		object.uncoalesce();
	}
	
	/** LOCAL INTERFACES **/
	/** 
	 * Create a new kernel object locally on this server.
	 * 
	 * @param cl
	 * @param args
	 */
	public KernelOID newKernelObject(Class<?> cl, Object ... args) throws KernelObjectNotCreatedException {
		KernelOID oid = null;
		// get OID
		try {
			oid = oms.registerKernelObject(host);
		} catch (RemoteException e) {
			throw new KernelObjectNotCreatedException("Error making RPC to OMS: "+e);
		}
		
		// Create the actual kernel object stored in the object manager
		objectManager.newObject(oid, cl, args);
		logger.fine("Created new Kernel Object on host: " + host + " with OID: " + oid.getID());
		
		return oid;
	}

	/**
	 * Move object from this server to host.
	 * @param host
	 * @param oid
	 * @throws RemoteException
	 * @throws KernelObjectNotFoundException
	 */
	public void moveKernelObjectToServer(InetSocketAddress host, KernelOID oid) throws RemoteException, KernelObjectNotFoundException {
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
	
	public Serializable getObject(KernelOID oid) throws KernelObjectNotFoundException {
		KernelObject object = objectManager.lookupObject(oid);
		return object.getObject();
	}

	public Serializable getKernelObject(KernelOID oid) throws KernelObjectNotFoundException {
		KernelObject object = objectManager.lookupObject(oid);
		return object;
	}

	public void removeObject(KernelOID oid) throws KernelObjectNotFoundException {
		objectManager.removeObject(oid);
	}
	
	/**
	 * Get the local hostname
	 * @return IP address of host that this server is running on
	 */
	public InetSocketAddress getLocalHost() {
		return host;
	}
	
	/**
	 * Get the kernel client for making RPCs
	 * @return the kernel client in this server
	 */
	public KernelClient getKernelClient() {
		return client;
	}
		
	/**
	 * Start the first server-side app object
	 */
	@Override
	public AppObjectStub startApp(String className) throws RemoteException {
		AppObjectStub appEntryPoint = null;
		try {
			AppEntryPoint entryPoint = (AppEntryPoint) Class.forName(className).newInstance();
            appEntryPoint = entryPoint.start();
		} catch (Exception e) {
			logger.severe("Could not start app");
			e.printStackTrace();
		}
		return appEntryPoint;
	}

	public class MemoryStatThread extends Thread {
		public void run() {
			while (true) {
				try {
					Thread.sleep(100000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
//				System.out.println("Total memory: " + Runtime.getRuntime().totalMemory() + " Bytes");
//				System.out.println("Free memory: " + Runtime.getRuntime().freeMemory() + " Bytes");
			}
		}
	}
	
	public MemoryStatThread getMemoryStatThread() {
		return new MemoryStatThread();
	}
	
	
	/**
	 * At startup, contact the OMS.
	 * @param args
	 */
	public static void main(String args[]) {
		//Time Being for backward compatibility Region is optional in the configuration
		if (args.length < 4) {
			System.out.println("Incorrect arguments to the kernel server");
			System.out.println("[host ip] [host port] [oms ip] [oms port] [region]");
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

		try {
			KernelServerImpl server = new KernelServerImpl(host, omsHost);
			KernelServer stub = (KernelServer) UnicastRemoteObject.exportObject(server, 0);
			Registry registry = LocateRegistry.createRegistry(Integer.parseInt(args[1]));
			registry.rebind("SapphireKernelServer", stub);

			if (args.length > 4) {
				server.setRegion(args[4]);
			}else {
				//server.setRegion("default");
				// TODO once we are sure we can comment below line & uncomment above line
				server.setRegion(host.toString());
			}
			oms.registerKernelServer(new ServerInfo(host,server.getRegion()));
			logger.info("Server ready!");
			System.out.println("Server ready!");
			
			/* Start a thread that print memory stats */
			server.getMemoryStatThread().start();

		} catch (Exception e) {
			logger.severe("Cannot start Sapphire Kernel Server");
			e.printStackTrace();
		}
	}
}

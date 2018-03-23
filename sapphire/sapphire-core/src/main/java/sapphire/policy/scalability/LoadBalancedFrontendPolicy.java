package sapphire.policy.scalability;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.policy.DefaultSapphirePolicy;


/**
 * Created by SrinivasChilveri on 2/19/18.
 * Simple load balancing w/ static number of replicas and no consistency
 */

public class LoadBalancedFrontendPolicy extends DefaultSapphirePolicy {

	/**
	 * LoadBalancedFrontend client policy. The client will LoadBalance among the Sapphire Server replica objects.
	 * client side DM instance should randomise the order in which it performs round robin against replicas
	 * @author SrinivasChilveri
	 *
	 */
	public static class ClientPolicy extends DefaultSapphirePolicy.DefaultClientPolicy {
		private static int index;
		private ArrayList<SapphireServerPolicy> replicaList;
		private static Logger logger = Logger.getLogger(ClientPolicy.class.getName());


		@Override
		public Object onRPC(String method, ArrayList<Object> params) throws Exception {
			ServerPolicy server = getCurrentServer();
			return server.onRPC(method, params);
		}

		private synchronized  ServerPolicy getCurrentServer() {

			if(null == replicaList || replicaList.isEmpty()){
				// get all the servers which has replicated Objects only once
				// dynamically added replicas are considered later
				replicaList = getGroup().getServers();
				index = (int)(Math.random()*replicaList.size());

			} else {
						if (++index >= replicaList.size()){
							index = 0;
						}
			}
			return (ServerPolicy)replicaList.get(index);
		}

	}

	/**
	 * LoadBalancedFrontend server policy.
	 * a configurable value for the number of concurrent requests supported per replica should be provided
	 * If the number of concurrent requests against a given replica exceeds that number, requests to that server
	 * replica should fail (in the server DM) with an appropriate exception (indicating server overload).
	 * @author SrinivasChilveri
	 *
	 */
	public static class ServerPolicy extends DefaultSapphirePolicy.DefaultServerPolicy {
		private static Logger logger = Logger.getLogger(ServerPolicy.class.getName());
		private static int STATIC_RPS = 2 ; //currently its hard coded we can read from config or annotations
		private Semaphore limiter = new Semaphore(STATIC_RPS,true);


		@Override
		public void onCreate(SapphireGroupPolicy group) {
			super.onCreate(group);
		
		}

		@Override
		public Object onRPC(String method, ArrayList<Object> params) throws Exception {

			try {
				if (limiter.tryAcquire()) { //concurrent requests count not reached
					return super.onRPC(method, params);
				} else {
					logger.warning("Trowing Exception on server overload");
					throw new ServerOverLoadException("The Replica of the SappahireObject on this Kernel Server Over Loaded");
				}
			}
			finally {
				limiter.release();
			}
		}
        public ServerPolicy onSapphireObjectReplicate() {
            return (ServerPolicy) this.sapphire_replicate();
        }
        public void onSapphirePin(InetSocketAddress server) throws RemoteException {
			sapphire_pin_to_server(server);
		}
    }

	/**
	 * Group policy. creates the configured num of replicas.
	 * @author SrinivasChilveri
	 *
	 */
	public static class GroupPolicy extends DefaultSapphirePolicy.DefaultGroupPolicy {
		private ArrayList<SapphireServerPolicy> nodes;
		private static int STATIC_REPLICAS = 2 ; //currently its hard coded we can read from config or annotations
		private static Logger logger = Logger.getLogger(GroupPolicy.class.getName());

		@Override
		synchronized public void addServer(SapphireServerPolicy server) {
			nodes.add(server);
		}


		@Override
		public ArrayList<SapphireServerPolicy> getServers() {
			return nodes;
		}

		@Override
		public void onCreate(SapphireServerPolicy server) {
			nodes = new ArrayList<SapphireServerPolicy>();
			int count = 1; /* count is started with 1 excluding the present server */

			//Initialize and consider this server
			addServer(server);


			/* Creation of group happens when the first instance of sapphire object is
			being created. Loop through all the kernel servers and replicate the
			sapphire objects on them based on the static replica count */
			try {

				/* Find the current region and the kernel server on which this first instance of
				sapphire object is being created. And try to replicate the
				sapphire objects in the same region(excluding this kernel server) */
				String region = server.sapphire_getRegion();

				ArrayList<InetSocketAddress> kernelServers = sapphire_getServersInRegion(region);

				/* Create the replicas on different kernelServers belongs to same region*/
				if (kernelServers != null ) {
					for (int i = 0; i < kernelServers.size() && count < STATIC_REPLICAS; i++) {

						if ((kernelServers.get(i)).equals(((KernelObjectStub) server).$__getHostname())) {
							continue;
						}

						ServerPolicy replica = ((ServerPolicy) server).onSapphireObjectReplicate();
						replica.onSapphirePin(kernelServers.get(i));

						((KernelObjectStub) replica).$__updateHostname(kernelServers.get(i));

						count++;
					}
				}

				/* If the replicas created are less than the number of replicas configured,
				log a warning message */
				if (count != STATIC_REPLICAS)  {
					logger.log(Level.SEVERE,"Configured replicas count: " + STATIC_REPLICAS + ", created replica count : " + count);
					throw new Error("Configured replicas count: " + STATIC_REPLICAS + ", created replica count : " + count);
				}

            } catch (RemoteException e) {
                e.printStackTrace();
                throw new Error("Could not create new group policy because the oms is not available.");
            }
        }

    }

}
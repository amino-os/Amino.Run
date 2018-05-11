package sapphire.policy.scalability;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

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
				replicaList = ((GroupPolicy)getGroup()).getServers();
				index = (int)(Math.random()* 100);

			}
			index = ++index % replicaList.size();
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
		private static int MAX_CONCURRENT_REQUESTS = 2000 ; //currently its hard coded we can read from config or annotations
		private Semaphore limiter = new Semaphore(MAX_CONCURRENT_REQUESTS,true);


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
					logger.warning("Throwing Exception on server overload on reaching the concurrent requests count"+ MAX_CONCURRENT_REQUESTS);
					throw new ServerOverLoadException("The Replica of the SappahireObject on this Kernel Server Over Loaded on reaching the concurrent requests count " + MAX_CONCURRENT_REQUESTS);
				}
			}
			finally {
				limiter.release();
			}
		}
    }

	/**
	 * Group policy. creates the configured num of replicas.
	 * @author SrinivasChilveri
	 *
	 */
	public static class GroupPolicy extends DefaultSapphirePolicy.DefaultGroupPolicy {
		private static int STATIC_REPLICAS = 2 ; //currently its hard coded we can read from config or annotations
		private static Logger logger = Logger.getLogger(GroupPolicy.class.getName());

		@Override
		public void onCreate(SapphireServerPolicy server) {
			int count = 0;     // count is compared below < STATIC_REPLICAS-1 excluding the present server
			int numnodes = 0 ; // num of nodes/servers in the selected region

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
				InetSocketAddress addr = server.sapphire_locate_kernel_object(server.$__getKernelOID());

				ArrayList<InetSocketAddress> kernelServers = sapphire_getServersInRegion(region);

				/* Create the replicas on different kernelServers belongs to same region*/
				if (kernelServers != null ) {

					kernelServers.remove(addr);
					numnodes = kernelServers.size();

					for (count = 0; count < numnodes && count < STATIC_REPLICAS-1; count++) {
						ServerPolicy replica = (ServerPolicy) server.sapphire_replicate();
						replica.sapphire_pin_to_server(kernelServers.get(count));
						((KernelObjectStub) replica).$__updateHostname(kernelServers.get(count));
					}
				}

				/* If the replicas created are less than the number of replicas configured,
				log a warning message */
				if (count != STATIC_REPLICAS-1)  {
					logger.severe("Configured replicas count: " + STATIC_REPLICAS + ", created replica count : " + count+
							"insufficient servers in region "+ numnodes + "to create required replicas");
					throw new Error("Configured replicas count: " + STATIC_REPLICAS + ", created replica count : " + count);
				}

            } catch (RemoteException e) {
                logger.severe("Received RemoteException may be oms is down ");
                throw new Error("Could not create new group policy because the oms is not available.", e);
            }
        }

    }

}
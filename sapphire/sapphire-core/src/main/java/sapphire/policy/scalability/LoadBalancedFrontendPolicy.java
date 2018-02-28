package sapphire.policy.scalability;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.apache.harmony.rmi.common.RMIUtil;

import sapphire.compiler.GlobalStubConstants;
import sapphire.common.AppObjectStub;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.oms.KernelServerInfo;


/**
 * Created by SrinivasChilveri on 19/2/18.
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
		private static int     index;
		ArrayList<SapphireServerPolicy> replicaList = new ArrayList<SapphireServerPolicy>();

		@Override
		public Object onRPC(String method, ArrayList<Object> params) throws Exception {

			if(replicaList.isEmpty()){
				replicaList = getGroup().getServers();
				// get all the servers which has replicated Objects only once dynamically added replicas
				// are considered later
				index = (int)(Math.random()*replicaList.size());
			}
			else {
				if (++index >= replicaList.size()){
					index = 0;
				}
			}

			return ((ServerPolicy)(replicaList.get(index))).onRPC(method,params);
		}
	}

	/**
	 * LoadBalancedFrontend server policy. throws .
	 * a configurable value for the number of concurrent requests supported per replica per should be provided
	 * If the number of concurrent requests against a given replica exceeds that number, requests to that server
	 * replica should fail (in the server DM) with an appropriate exception (indicating server overload).
	 * @author SrinivasChilveri
	 *
	 */
	public static class ServerPolicy extends DefaultSapphirePolicy.DefaultServerPolicy {
		private static Logger logger = Logger.getLogger(GroupPolicy.class.getName());
		private static int STATIC_RPS = 2000 ; //currently its hard coded we can read from config or annotations
		private static int PERIOD = 1;
		transient protected RateLimiter limiter;

		@Override
		public void onCreate(SapphireGroupPolicy group) {
			super.onCreate(group);
			limiter = new SimpleRateLimiter(STATIC_RPS);
			limiter.start();
		}

		@Override
		public Object onRPC(String method, ArrayList<Object> params) throws Exception {
			if (limiter.tryAcquire()) { //rps is not reached
				return super.onRPC(method, params);
			} else {
				throw new ServerOverLoadException("The Replica of the SappahireObject on this Kernel Server Over Loaded");
			}
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
		synchronized public void addServer(SapphireServerPolicy server) throws Exception {
			nodes.add(server);
		}

		@Override
		public void onFailure(SapphireServerPolicy server) {
			// TODO
		}

		@Override
		public SapphireServerPolicy onRefRequest() {
			return nodes.get(0);
		}

		@Override
		public ArrayList<SapphireServerPolicy> getServers() {
			return nodes;
		}

		@Override
		public void onCreate(SapphireServerPolicy server) {
			nodes = new ArrayList<SapphireServerPolicy>();
			int count = 1; /* count is started with 1 excluding the present server */

			/* Creation of group happens when the first instance of sapphire object is
			being created. Loop through all the kernel servers and replicate the
			sapphire objects on them based on the static replica count */
			try {

				/* Find the current region and the kernel server on which this first instance of
				sapphire object is being created. And try to replicate the
				sapphire objects in the same region(excluding this kernel server) */
				String region = server.sapphire_getRegion();

				ArrayList<KernelServerInfo> kernelServers = sapphire_getKernelServersInRegion(region);

				/* Create the replicas */
				for (int  i = 0; i < kernelServers.size() && count < STATIC_REPLICAS; i++) {

					if ((kernelServers.get(i)).getHost().equals(((KernelObjectStub)server).$__getHostname())) {
						continue;
					}

					kernelServers.get(i).getKernelServer().createSapphireObjectReplica(
							RMIUtil.getShortName(server.getClass()),
							RMIUtil.getShortName(getClass()) + GlobalStubConstants.STUB_SUFFIX,
							$__getKernelOID(), (AppObjectStub) server.sapphire_getAppObject().getObject());

					count++;
				}

				addServer(server);

				/* If the replicas created are less than the number of replicas configured,
				log a warning message */
				if (count != STATIC_REPLICAS)  {
					logger.warning("Configured replicas count: " + STATIC_REPLICAS + ", created replica count : " + count);
					throw new Error("Configured replicas count: " + STATIC_REPLICAS + ", created replica count : " + count);
				}

			} catch (Exception e) {
				e.printStackTrace();
				// TODO: Need Cleanup
				throw new Error("Could not create new group policy" + e);
			}
		}
	}

}
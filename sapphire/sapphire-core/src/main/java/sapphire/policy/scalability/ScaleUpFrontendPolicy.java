package sapphire.policy.scalability;

import java.util.ArrayList;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.policy.util.ResettableTimer;

/**
 * ScaleUpFrontEnd DM: Load-balancing w/ dynamic allocation of replicas and no consistency
 * Created by Venugopal Reddy K 00900280 on 2/18/18.
 */

public class ScaleUpFrontendPolicy extends LoadBalancedFrontendPolicy {

	public static class ClientPolicy extends LoadBalancedFrontendPolicy.ClientPolicy {
		private final AtomicInteger replicaListSyncCtr = new AtomicInteger();
		@Override
		public Object onRPC(String method, ArrayList<Object> params) throws Exception {
			if (0 == (replicaListSyncCtr.getAndIncrement() % 100)) {
				synchronized (this) {
					// TODO: Should device a mechanism to fetch the latest replica list
					replicaList = null;
				}
			}

			return super.onRPC(method, params);
		}
	}

	public static class ServerPolicy extends LoadBalancedFrontendPolicy.ServerPolicy {
		private int REPLICA_CREATE_MIN_TIME_IN_MSEC = 100;// for n milliseconds
		private int REPLICA_COUNT = 1; // 1 replica in n milliseconds
		private Semaphore replicaCreateLimiter = new Semaphore(REPLICA_COUNT, true);
		transient volatile private ResettableTimer timer = null; // Timer for limiting

		private void startServerTimer() {
			/* Double checked locking */
			if (null == timer) {
				synchronized (this) {
					if (null == timer) {
						timer = new ResettableTimer(new TimerTask() {
							public void run() {
								replicaCreateLimiter.release(REPLICA_COUNT - replicaCreateLimiter.availablePermits());
								scaleDown();
							}
						}, REPLICA_CREATE_MIN_TIME_IN_MSEC);
						timer.start();
					}
				}
			}
		}

		@Override
		public Object onRPC(String method, ArrayList<Object> params) throws Exception {
			try {
				/* Check and start the timer. Currently, sapphire library do not provide
				the support for dynamic data initialization of DM upon migration of kernel objects.
				Need to remove check and start timer call here when sapphire supports dynamic data
				initialization */
				startServerTimer();
				return super.onRPC(method, params);
			} catch (ServerOverLoadException e) {
				if (!replicaCreateLimiter.tryAcquire()) {
					throw new ScaleUpException("Replica creation rate exceeded for this sapphire object");
				}

				((GroupPolicy)getGroup()).scaleUpReplica(sapphire_getRegion());
			}

			return null;
		}

		private void scaleDown() {

			/* When the load at a given replica drops to approximately p * (m-2)/m
			(where m is the current number of replicas, and p is the maximum concurrency
			setting per replica), then the server-side DM for that replica should remove one
			replica (randomly chosen). This is because there are in theory two more replicas
			than required, so one can be removed. The number of replicas should not be
			reduced below 2 (in case one fails).
			 */
			ArrayList<SapphireServerPolicy> replicaServers = getGroup().getServers();
			double currentReplicas = replicaServers.size();
			if (currentReplicas <= 2) {
				// Scale down shouldn't happen if the replica count is less than or equal to 2
				return;
			}

			double maxConcurrencyLimit = MAX_CONCURRENT_REQUESTS;
			double currentLoad = maxConcurrencyLimit - limiter.availablePermits();
			if (currentLoad < (maxConcurrencyLimit * (currentReplicas - 2)/currentReplicas)) {
				//delete this replica
				try {
					((GroupPolicy)getGroup()).scaleDownReplica(this);
					sapphire_deleteKernelObject();
					synchronized(this) {
						timer.cancel();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static class GroupPolicy extends LoadBalancedFrontendPolicy.GroupPolicy {
		private int REPLICA_CREATE_MIN_TIME_IN_MSEC = 100;// n milliseconds
		private int REPLICA_COUNT = 1; // 1 replica in n milliseconds
		private Semaphore replicaCreateLimiter = new Semaphore(REPLICA_COUNT, true);
		transient private ResettableTimer timer = null; // Timer for limiting

		private void startGroupTimer() {
			/* Double checked locking */
			if (null == timer) {
				synchronized (this) {
					if (null == timer) {
						timer = new ResettableTimer(new TimerTask() {
							public void run() {
								replicaCreateLimiter.release(REPLICA_COUNT - replicaCreateLimiter.availablePermits());
							}
						}, REPLICA_CREATE_MIN_TIME_IN_MSEC);
						timer.start();
					}
				}
			}
		}

		@Override
		public void onCreate(SapphireServerPolicy server) {
			super.onCreate(server);
			/* Check and start the group timer. Currently, sapphire library do not provide the support
			for dynamic data initialization of DM upon migration of kernel objects. Need to remove
			check and start timer call here when sapphire supports dynamic data initialization */
			startGroupTimer();
		}

		public synchronized void scaleUpReplica(String region) throws Exception {
			if (!replicaCreateLimiter.tryAcquire()) {
				throw new ScaleUpException("Replica creation rate exceeded for this sapphire object.");
			}

			/* Get the list of available servers in region */
			ArrayList<InetSocketAddress> fullKernelList;
			fullKernelList = sapphire_getServersInRegion(region);
			if (null == fullKernelList) {
				throw new ScaleUpException("Scaleup failed. Couldn't fetch kernel server list.");
			}

			/* Get the list of servers on which replicas already exist */
			ArrayList<InetSocketAddress> sappObjReplicatedKernelList = new ArrayList <InetSocketAddress>();
			for (SapphireServerPolicy tmp : getServers()) {
				sappObjReplicatedKernelList.add(((KernelObjectStub)tmp).$__getHostname());
			}

			/* Remove the servers which already have replicas of this sapphire object */
			fullKernelList.removeAll(sappObjReplicatedKernelList);

			if (!fullKernelList.isEmpty()) {
				/* create a replica on the first server in the list */
				SapphireServerPolicy server = getServers().get(0);
				SapphireServerPolicy replica = ((ServerPolicy) server).onSapphireObjectReplicate();
				((ServerPolicy)replica).onSapphirePin(fullKernelList.get(0));
				((KernelObjectStub) replica).$__updateHostname(fullKernelList.get(0));
				servers.add(replica);
			}
			else {
				throw new ScaleUpException("Replica cannot be created for this sapphire object. All kernel servers have its replica.");
			}
		}

		private void deleteServer(SapphireServerPolicy server) {
			ArrayList<SapphireServerPolicy> serverList = servers;
			Iterator itr = serverList.iterator();

			while (itr.hasNext()) {
				SapphireServerPolicy temp = (SapphireServerPolicy) itr.next();
				if (temp.$__getKernelOID().equals(server.$__getKernelOID())) {
					itr.remove();
					break;
				}
			}
		}

		public synchronized void scaleDownReplica(SapphireServerPolicy server) throws Exception {
			if (2 >= servers.size()) {
				throw new ScaleDownException("Cannot scale down. Current replica count is " + servers.size());
			}

			deleteServer(server);
		}
	}
}

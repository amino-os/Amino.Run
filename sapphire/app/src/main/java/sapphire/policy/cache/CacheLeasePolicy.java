package sapphire.policy.cache;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import sapphire.common.AppObject;
import sapphire.common.SapphireObjectNotAvailableException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.policy.SapphirePolicy;
import sapphire.policy.SapphirePolicy.SapphireClientPolicy;
import sapphire.policy.SapphirePolicy.SapphireGroupPolicy;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;

/**
 * A caching policy between the mobile device and the server that uses leases for writing.
 * @author iyzhang
 *
 */
public class CacheLeasePolicy extends SapphirePolicy {
	static final int LEASE_PERIOD = 10;
	/** stick in some buffer to account for differences in time **/
	static final int LEASE_BUFFER = 1;

	/**
	 * Object representing a lease. Includes a lease ID, a timeout for the lease and the cached app object
	 * @author iyzhang
	 *
	 */
	public static class CacheLease implements Serializable {
		private Integer lease;
		private Date leaseTimeout;
		private AppObject cachedObject;
		
		public CacheLease(Integer lease, Date leaseTimeout, AppObject cachedObject) {
			this.lease = lease;
			this.leaseTimeout = leaseTimeout;
			this.cachedObject = cachedObject;
			assert(cachedObject != null);
		}
		
		public Integer getLease() {
			return lease;
		}
		
		public Date getLeaseTimeout() {
			return leaseTimeout;
		}
		
		public AppObject getCachedObject() {
			return cachedObject;
		}
	}
	
	/**
	 * Cache lease client policy. The client side proxy for the cache that holds the
	 * cached object, gets leases from the server and writes locally. 
	 * @author iyzhang
	 *
	 */
	public static class CacheLeaseClientPolicy extends SapphireClientPolicy {
		private CacheLeaseServerPolicy server;
		private CacheLeaseGroupPolicy group;
		private Integer lease = -1;
		private Date leaseTimeout;
		private AppObject cachedObject = null;

		@Override
		public void onCreate(SapphireGroupPolicy group) {
			this.group = (CacheLeaseGroupPolicy) group;
		}
		
		@Override
		public SapphireGroupPolicy getGroup() {
			return group;
		}

		@Override
		public SapphireServerPolicy getServer() {
			return server;
		}

		@Override
		public void setServer(SapphireServerPolicy server) {
			this.server = (CacheLeaseServerPolicy) server;
		}

		private Boolean leaseStillValid() {
			System.out.println("Lease: "+lease.toString());
			if (lease > 0) {
				Date currentTime = new Date();
				System.out.println("Lease timeout: "+leaseTimeout.toString()+" current time: "+currentTime.toString());
				return true; //currentTime.getTime() < leaseTimeout.getTime();
			} else {
				return false;
			}
		}
		
		private void sync() {
			server.syncObject(lease, cachedObject.getObject());
		}
		
		@Override
		public Object onRPC(String method, ArrayList<Object> params) throws Exception {
			Object ret = null;
			if (leaseStillValid()) {
				ret = cachedObject.invoke(method, params);
				if (true) {
					sync();
				}
			} else {
				try {
					CacheLease cachelease = null;
					if (lease > 0) {
						cachelease = server.getLease(lease);
					} else {
						cachelease = server.getLease();
					}
					
					if (cachelease == null) {
						throw new SapphireObjectNotAvailableException("Could not get lease.");
					}
					
					// If we have a new lease, then the object might have changed
					if (!cachelease.getLease().equals(lease)) {
						cachedObject = cachelease.getCachedObject();
					}
					lease = cachelease.getLease();
					leaseTimeout = cachelease.getLeaseTimeout();
					ret = cachedObject.invoke(method, params);					
				} catch (RemoteException e) {
					throw new SapphireObjectNotAvailableException("Could not contact Sapphire server.");
				} catch (KernelObjectNotFoundException e) {
					throw new SapphireObjectNotAvailableException("Could not find server policy object.");					
				}
			}
			return ret;
		}

		private void writeObject(ObjectOutputStream out) throws IOException {
			 out.writeObject(server);
			 out.writeObject(group);
		 }
	     
		 private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
			 server = (CacheLeaseServerPolicy) in.readObject();
			 group = (CacheLeaseGroupPolicy) in.readObject();
			 lease = -1;
			 cachedObject = null;
		 }

	}
	
	/**
	 * Cache lease server policy. Holds the canonical object and grants leases.
	 * @author iyzhang
	 *
	 */
	public static class CacheLeaseServerPolicy extends SapphireServerPolicy {
		static private Logger logger = Logger.getLogger("sapphire.policy.DHTPolicy.CacheLeaseServerPolicy");
		private Integer lease;
		private Date leaseTimeout;
		private Random leaseGenerator;
		private CacheLeaseGroupPolicy group;

		public CacheLeaseServerPolicy() {
			leaseGenerator = new Random();
			lease = -1;
		}

		@Override
		public void onCreate(SapphireGroupPolicy group) {
			// TODO Auto-generated method stub
			this.group = (CacheLeaseGroupPolicy) group;
		}
		
		@Override
		public SapphireGroupPolicy getGroup() {
			return group;
		}

		
		private Date generateTimeout() {
			Date currentTime = new Date();
			return new Date(currentTime.getTime() + LEASE_PERIOD * 60000);
		}
		
		private CacheLease getNewLease() {
			// Always generate a positive lease id
			lease = leaseGenerator.nextInt(Integer.MAX_VALUE);
			leaseTimeout = generateTimeout();
			return new CacheLease(lease, leaseTimeout, sapphire_getAppObject());
		}
		
		private Boolean leaseStillValid() {
			if (lease > 0) {
				Date currentTime = new Date();
				return currentTime.getTime() < leaseTimeout.getTime() + LEASE_BUFFER * 60000;
			} else {
				return false;
			}
		}

		public CacheLease getLease() throws Exception {
			if (leaseStillValid()) {
				logger.log(Level.INFO, "Someone else holds the lease.");
				return null;
			} else {
				CacheLease cachelease = getNewLease();
				logger.log(Level.INFO, "Granted lease "+cachelease.getLease().toString()+" on object "+cachelease.getCachedObject().toString()+" until "+cachelease.getLeaseTimeout().toString());
				return cachelease;
			}
		}
		
		public CacheLease getLease(Integer lease) throws Exception {
			logger.log(Level.INFO, "Get lease "+lease.toString()+" currentlease: "+this.lease.toString());

			if (this.lease.equals(lease)) {
				// This person still has the lease, so just return it and renew the lease
				leaseTimeout = generateTimeout();
				return new CacheLease(lease, leaseTimeout, null);
			} else if (leaseStillValid()) {
				// Someone else has a valid lease still, so this person can't have it
				return null;
			} else {
				// Someone else's lease expired, so you can have a new lease
				return getNewLease();
			}
		}
		
		public void syncObject(Integer lease, Serializable object) {
			appObject.setObject(object);
		}

		@Override
		public void onMembershipChange() {
			// TODO Auto-generated method stub
			
		}
	}
	
	/**
	 * Group policy. Doesn't do anything for now.
	 * TODO recreate the server on failure with a checkpointed object?
	 * @author iyzhang
	 *
	 */
	public static class CacheLeaseGroupPolicy extends SapphireGroupPolicy {
		CacheLeaseServerPolicy server;
		
		@Override
		public void addServer(SapphireServerPolicy server) {
			this.server = (CacheLeaseServerPolicy) server;
		}

		@Override
		public void onFailure(SapphireServerPolicy server) {
			
		}

		@Override
		public SapphireServerPolicy onRefRequest() {
			return server;
		}

		@Override
		public ArrayList<SapphireServerPolicy> getServers() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void onCreate(SapphireServerPolicy server) {
			// TODO Auto-generated method stub
			addServer(server);
		}
		
	}
}

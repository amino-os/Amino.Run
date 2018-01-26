package sapphire.policy.cache;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import sapphire.common.AppObject;
import sapphire.common.SapphireObjectNotAvailableException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.policy.SapphirePolicy;

/**
 * A caching policy between the mobile device and the server that uses leases for writing.
 * @author iyzhang
 *
 */
public class CacheLeasePolicy extends SapphirePolicy {
	public static final long DEFAULT_LEASE_PERIOD = 10 * 1000; // milliseconds
	/** stick in some buffer to account for differences in time **/
	static final int LEASE_BUFFER = 1 * 1000; // milliseconds TODO: Quinton.  This won't work.  Don't rely on clocks being in sync.
	// Rather rely on server sending back a duration.  Both client and server expire after that duration.
	// The lease on the server is then guaranteed to expire before the lease on the client, by exactly the amount of
	// network latency between the client and the server, which is typically less than 1 sec.

	/**
	 * Object representing a lease. Includes a lease ID, a timeout for the lease and the cached app object
	 * @author iyzhang
	 *
	 */
	public static class CacheLease implements Serializable {
		public static final UUID NO_LEASE = new UUID(0L, 0L); // This is an invalid UUID
		private UUID lease;
		private Date leaseTimeout;
		private AppObject cachedObject;
		
		public CacheLease(UUID lease, Date leaseTimeout, AppObject cachedObject) {
			this.lease = lease;
			this.leaseTimeout = leaseTimeout;
			this.cachedObject = cachedObject;
			assert(cachedObject != null);
		}
		
		public UUID getLease() {
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
		protected CacheLeaseServerPolicy server;
		private CacheLeaseGroupPolicy group;
		protected UUID lease = CacheLease.NO_LEASE;
		protected Date leaseTimeout;
		protected AppObject cachedObject = null;

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

		protected Boolean leaseStillValid() {
			System.out.println("Lease: "+lease.toString());
			if (!lease.equals(CacheLease.NO_LEASE)) {
				Date currentTime = new Date();
				System.out.println("Lease timeout: "+leaseTimeout.toString()+" current time: "+currentTime.toString());
				return  leaseTimeout.compareTo(currentTime) > 0;
			} else {
				return false;
			}
		}
		
		protected void sync() {
			server.syncObject(lease, cachedObject.getObject());
		}
		
		@Override
		public Object onRPC(String method, ArrayList<Object> params) throws Exception {
			Object ret = null;
			if (!leaseStillValid()) {
				getNewLease(CacheLeasePolicy.DEFAULT_LEASE_PERIOD);
			}
			ret = cachedObject.invoke(method, params);
			if (true) {  // TODO: isMutable?
				sync();
			}
			return ret;
		}

		protected void getNewLease(long timeoutMillisec) throws Exception {
			try {
				CacheLease cachelease = null;
				if (!lease.equals(CacheLease.NO_LEASE)) {
					cachelease = server.getLease(lease, timeoutMillisec);
				} else {
					cachelease = server.getLease(timeoutMillisec);
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
			} catch (RemoteException e) {
				throw new SapphireObjectNotAvailableException("Could not contact Sapphire server.", e);
			} catch (KernelObjectNotFoundException e) {
				throw new SapphireObjectNotAvailableException("Could not find server policy object.", e);
			}
		}

		protected void releaseCurrentLease() throws Exception {
			try {
				server.releaseLease(lease);
			}
			finally {
				lease = CacheLease.NO_LEASE;
				leaseTimeout = new Date(0L); // The beginning of time.
				cachedObject = null;
			}
		}

		protected void writeObject(ObjectOutputStream out) throws IOException {
			 out.writeObject(server);
			 out.writeObject(group);
		 }
	     
		 protected void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
			 server = (CacheLeaseServerPolicy) in.readObject();
			 group = (CacheLeaseGroupPolicy) in.readObject();
			 lease = CacheLease.NO_LEASE;
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
		private UUID lease;
		private Date leaseTimeout;
		private CacheLeaseGroupPolicy group;

		public CacheLeaseServerPolicy() {
			lease = CacheLease.NO_LEASE;
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
			return generateTimeout(DEFAULT_LEASE_PERIOD);
		}

		private Date generateTimeout(long leasePeriodMillisec) {
			Date currentTime = new Date();
			return new Date(currentTime.getTime() + leasePeriodMillisec);
		}
		
		private CacheLease getNewLease(long timeoutMillisec) {
			// Always generate a positive lease id
			lease = UUID.randomUUID();
			leaseTimeout = generateTimeout(timeoutMillisec);
			return new CacheLease(lease, leaseTimeout, sapphire_getAppObject());
		}
		
		private Boolean leaseStillValid() {
			if (!lease.equals(CacheLease.NO_LEASE)) {
				Date currentTime = new Date();
				return currentTime.getTime() < leaseTimeout.getTime() + LEASE_BUFFER;
			} else {
				return false;
			}
		}

		public CacheLease getLease(long timeoutMillisec) throws Exception {
			if (leaseStillValid()) {
				logger.log(Level.INFO, "Someone else holds the lease.");
				return null;
			} else {
				CacheLease cachelease = getNewLease(timeoutMillisec);
				logger.log(Level.INFO, "Granted lease "+cachelease.getLease().toString()+" on object "+cachelease.getCachedObject().toString()+" until "+cachelease.getLeaseTimeout().toString());
				return cachelease;
			}
		}
		
		public CacheLease getLease(UUID lease, long timeoutMillisec) throws Exception {
			logger.log(Level.INFO, "Get lease "+lease.toString()+" currentlease: "+this.lease.toString());

			if (this.lease.equals(lease)) {
				// This person still has the lease, so just return it and renew the lease
				leaseTimeout = generateTimeout(timeoutMillisec);
				return new CacheLease(lease, leaseTimeout, null);
			} else if (leaseStillValid()) {
				// Someone else has a valid lease still, so this person can't have it
				return null;
			} else {
				// Someone else's lease expired, so you can have a new lease
				return getNewLease(timeoutMillisec);
			}
		}

		public void releaseLease(UUID lease) throws Exception {
			if (this.lease == lease) {
				this.lease = CacheLease.NO_LEASE;
				this.leaseTimeout = new Date(0L);
			}
			else {
				throw new LeaseExpiredException("Attempt to release expired server lease " + lease + " Current server lease is " + this.lease);
			}
		}
		
		public void syncObject(UUID lease, Serializable object) {
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

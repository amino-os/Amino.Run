package amino.run.policy.cache;

import amino.run.common.AppObject;
import amino.run.common.MicroServiceNotAvailableException;
import amino.run.kernel.common.KernelObjectNotFoundException;
import amino.run.policy.DefaultPolicy;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A caching policy between the mobile device and the server that uses leases for writing.
 *
 * @author iyzhang
 */
public class CacheLeasePolicy extends DefaultPolicy {
    public static final long DEFAULT_LEASE_PERIOD = 10 * 1000; // milliseconds
    /** stick in some buffer to account for differences in time * */
    static final int LEASE_BUFFER =
            1 * 1000; // milliseconds TODO: Quinton.  This won't work.  Don't rely on clocks
    // being in sync.
    // Rather rely on server sending back a duration.  Both client and server expire after that
    // duration.
    // The lease on the server is then guaranteed to expire before the lease on the client, by
    // exactly the amount of
    // network latency between the client and the server, which is typically less than 1 sec.

    /**
     * Object representing a lease. Includes a lease ID, a timeout for the lease and the cached app
     * object
     *
     * @author iyzhang
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
     * Cache lease client policy. The client side proxy for the cache that holds the cached object,
     * gets leases from the server and writes locally.
     *
     * @author iyzhang
     */
    public static class ClientPolicy extends DefaultClientPolicy {
        protected UUID lease = CacheLease.NO_LEASE;
        protected Date leaseTimeout;
        protected AppObject cachedObject = null;

        protected Boolean leaseStillValid() {
            System.out.println("Lease: " + lease.toString());
            if (!lease.equals(CacheLease.NO_LEASE)) {
                Date currentTime = new Date();
                System.out.println(
                        "Lease timeout: "
                                + leaseTimeout.toString()
                                + " current time: "
                                + currentTime.toString());
                return leaseTimeout.compareTo(currentTime) > 0;
            } else {
                return false;
            }
        }

        protected void sync() throws Exception {
            ((ServerPolicy) getServer()).syncObject(lease, cachedObject.getObject());
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            Object ret = null;
            if (!leaseStillValid()) {
                getNewLease(CacheLeasePolicy.DEFAULT_LEASE_PERIOD);
            }
            ret = cachedObject.invoke(method, params);
            if (true) { // TODO: isMutable?
                sync();
            }
            return ret;
        }

        protected void getNewLease(long timeoutMillisec) throws Exception {
            try {
                CacheLease cachelease = null;
                if (!lease.equals(CacheLease.NO_LEASE)) {
                    cachelease = ((ServerPolicy) getServer()).getLease(lease, timeoutMillisec);
                } else {
                    cachelease = ((ServerPolicy) getServer()).getLease(timeoutMillisec);
                }

                if (cachelease == null) {
                    throw new LeaseNotAvailableException("Could not get lease.");
                }

                // If we have a new lease, then the object might have changed
                if (!cachelease.getLease().equals(lease)) {
                    cachedObject = cachelease.getCachedObject();
                }
                lease = cachelease.getLease();
                leaseTimeout = cachelease.getLeaseTimeout();
            } catch (RemoteException e) {
                throw new MicroServiceNotAvailableException(
                        "Could not contact MicroService server.", e);
            } catch (KernelObjectNotFoundException e) {
                throw new MicroServiceNotAvailableException(
                        "Could not find server policy object.", e);
            }
        }

        protected void releaseCurrentLease() throws Exception {
            try {
                ((ServerPolicy) getServer()).releaseLease(lease);
            } finally {
                lease = CacheLease.NO_LEASE;
                leaseTimeout = new Date(0L); // The beginning of time.
                cachedObject = null;
            }
        }
    }

    /**
     * Cache lease server policy. Holds the canonical object and grants leases.
     *
     * @author iyzhang
     */
    public static class ServerPolicy extends DefaultServerPolicy {
        private static Logger logger = Logger.getLogger(ServerPolicy.class.getName());
        private UUID lease;
        private Date leaseTimeout;

        public ServerPolicy() {
            lease = CacheLease.NO_LEASE;
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
            return new CacheLease(lease, leaseTimeout, getAppObject());
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
                logger.log(
                        Level.INFO,
                        "Granted lease "
                                + cachelease.getLease().toString()
                                + " on object "
                                + cachelease.getCachedObject().toString()
                                + " until "
                                + cachelease.getLeaseTimeout().toString());
                return cachelease;
            }
        }

        public CacheLease getLease(UUID lease, long timeoutMillisec) throws Exception {
            logger.log(
                    Level.INFO,
                    "Get lease " + lease.toString() + " currentlease: " + this.lease.toString());

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
            if (this.lease.equals(lease)) {
                this.lease = CacheLease.NO_LEASE;
                this.leaseTimeout = new Date(0L);
            } else {
                throw new LeaseExpiredException(
                        "Attempt to release expired server lease "
                                + lease
                                + " Current server lease is "
                                + this.lease);
            }
        }

        public void syncObject(UUID lease, Serializable object) throws Exception {
            appObject.setObject(object);
        }
    }

    /**
     * Group policy. Doesn't do anything for now. TODO recreate the server on failure with a
     * checkpointed object?
     *
     * @author iyzhang
     */
    public static class GroupPolicy extends DefaultGroupPolicy {}
}

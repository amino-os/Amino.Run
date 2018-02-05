package sapphire.policy.serializability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Venugopal Reddy K 00900280 on 1/2/18.
 * Optimistic Lock Transaction Policy is an extension to Locking Transaction
 * Policy. Locking Transaction Policy allows only a single lease holder to
 * do the transactions during lease time. At any point of time, single client
 * can acquire lease and hold it till lease timeout. Whereas, Optmistic Lock
 * Transaction Policy allows for concurrent (i.e., multiple clients can acquire
 * the lease(with lease timeout) at the same time) transactions. But the first
 * client who commits, succeed its transaction and others fail.
 */

public class OptimisticLockTransactPolicy extends LockingTransactionPolicy {
    // Client Policy mechanism is same as that of locking transaction policy
    public static class ClientPolicy extends LockingTransactionPolicy.ClientPolicy {}

    public static class ServerPolicy extends LockingTransactionPolicy.ServerPolicy {
        static private Logger logger = Logger.getLogger("sapphire.policy.serializability.OptimisticLockTransactPolicy.ServerPolicy");
        private HashMap<UUID, CacheLease> concurrentLeases; //Map of concurrent clients trying to do the transactions
        public ServerPolicy() {
            concurrentLeases = new HashMap<UUID,CacheLease>();
        }

        private CacheLease getNewLease(long timeoutMillisec) {
            // Always generate a positive lease id
            UUID leaseId = UUID.randomUUID();
            CacheLease newLease = new CacheLease(leaseId, generateTimeout(timeoutMillisec), sapphire_getAppObject());
            synchronized(this) {
                concurrentLeases.put(leaseId, newLease);
            }
            return newLease;
        }

        @Override
        public CacheLease getLease(long timeoutMillisec) throws Exception {
            CacheLease cachelease = getNewLease(timeoutMillisec);
            logger.log(Level.INFO, "Granted lease "+cachelease.getLease().toString()+" on object "+cachelease.getCachedObject().toString()+" until "+cachelease.getLeaseTimeout().toString());
            return cachelease;
        }

        @Override
        public CacheLease getLease(UUID leaseId, long timeoutMillisec) throws Exception {
            CacheLease cacheLease = null;

            logger.log(Level.INFO, "Get lease Id"+leaseId.toString());

            synchronized(this) {
                cacheLease = this.concurrentLeases.get(leaseId);
            }

            if (null != cacheLease) {
                // This lease is already held. Just renew the lease and return it
                logger.log(Level.INFO, "Current lease: "+ cacheLease.getLease().toString());
                cacheLease.setLeaseTimeout(generateTimeout(timeoutMillisec));
                return cacheLease;
            } else {
                // Lease is not valid
                throw new Exception("Lease " + leaseId + " is invalid");
            }
        }

        @Override
        synchronized public void releaseLease(UUID leaseId) throws Exception {
            this.concurrentLeases.remove(leaseId);
        }

        @Override
        synchronized public void syncObject(UUID leaseId, Serializable object) throws Exception {
            CacheLease cacheLease = this.concurrentLeases.get(leaseId);
            if (null != cacheLease) {
                /* App object synchronization is allowed from a valid lease
                holder. Rest of the concurrent clients holding the lease at this
                point in time must fail when tried to synchronize. */
                appObject.setObject(object);
                this.concurrentLeases.clear();
                /* Add this client's lease back. It is removed on release lease */
                this.concurrentLeases.put(leaseId, cacheLease);
            }
            else {
                throw new Exception("Lease " + leaseId + " is invalid");
            }
        }

        @Override
        synchronized public Object onRPC(String method, ArrayList<Object> params) throws Exception {

            // Remove all the concurrent leases. So that, their respective commits will fail.

            /* NOTE: Commented clearing the concurrent lease map to the behavior same as
            locking transaction */

            //this.concurrentLeases.clear();
            return super.onRPC(method, params);
        }
    }
    // Group Policy mechanism is same as that of locking transaction policy
    public static class GroupPolicy extends LockingTransactionPolicy.CacheLeaseGroupPolicy {}
}

package sapphire.policy.serializability;

import java.rmi.RemoteException;
import java.util.ArrayList;

import sapphire.common.SapphireObjectNotAvailableException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.policy.cache.CacheLeasePolicy;

/**
 * Created by quinton on 1/21/18.
 * Multi-RPC transactions w/ server-side locking, no concurrent transactions.
 */
public class LockingTransactionPolicy extends CacheLeasePolicy {
	/**
	 * Locking Transaction client policy. The client side proxy that holds the
	 * cached object, gets leases from the server and writes locally during transactions.
	 *
	 */
	public static class ClientPolicy extends CacheLeasePolicy.CacheLeaseClientPolicy {
        protected boolean transactionInProgress;  // Has this client begun a transaction?

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if (isStartTransaction(method)) {
                this.startTransaction(params);
                return null;
            } else if (isCommitTransaction(method)) {
                this.commitTransaction();
                return null;
            } else if (isRollbackTransaction(method)) {
                this.rollbackTransaction();
                return null;
            } else { // Normal method invocation
                Object ret = null;
                if (transactionInProgress) { // Inside a transaction we invoke against the local copy.
                    if (leaseStillValid()) {
                        try {
                            return cachedObject.invoke(method, params);
                        } catch (Exception e) {
                            rollbackTransaction();
                            throw new Exception("Exception occurred inside transaction.  Transaction rolled back.", e);
                        }
                    } else { // Transaction has timed out.
                        rollbackTransaction();
                        throw new Exception("Transaction timed out.  Transaction rolled back.");
                    }
                } else { // Outside of transactions, we invoke against the server
                    return server.onRPC(method, params);
                }
            }
        }

        Boolean isStartTransaction(String method) {
            // TODO better check than simple base name
            return method.contains(".startTransaction(");
        }

        Boolean isCommitTransaction(String method) {
            // TODO better check than simple base name
            return method.contains(".commitTransaction(");
        }

        Boolean isRollbackTransaction(String method) {
            // TODO better check than simple base name
            return method.contains(".rollbackTransaction(");
        }

        public synchronized void startTransaction(ArrayList<Object> params) throws Exception {
            if (!transactionInProgress) {
                if(!params.isEmpty()) {
                    getNewLease((Integer)params.get(0));
                }
                else {
                    getNewLease(CacheLeasePolicy.DEFAULT_LEASE_PERIOD);
                }
                transactionInProgress = true;
            } else {
                throw new TransactionAlreadyStartedException("Transaction already started on Sapphire object.  Rollback or commit before starting a new transaction.");
            }
        }

        public synchronized void commitTransaction() throws Exception {
            if (transactionInProgress) {
                if (!leaseStillValid()) {
                    rollbackTransaction();
                    throw new Exception("Transaction timed out.  Transaction rolled back.");
                } else {
                    sync(); // Copy the results of the transaction to the server.
                    releaseCurrentLease();
                    transactionInProgress = false;
                }
            } else {
                throw new NoTransactionStartedException("No transaction to commit");
            }
        }

        public synchronized void rollbackTransaction() throws Exception {
            if (transactionInProgress) {
                releaseCurrentLease();
                transactionInProgress = false;
            } else {
                throw new NoTransactionStartedException("No transaction to roll back");
            }
        }

        // To make visible for unit testing, because superclass is in a different package.
        @Override
        protected void sync() {
            super.sync();
        }
    }

	public static class ServerPolicy extends CacheLeasePolicy.CacheLeaseServerPolicy {}
	
	public static class GroupPolicy extends CacheLeasePolicy.CacheLeaseGroupPolicy {}
}

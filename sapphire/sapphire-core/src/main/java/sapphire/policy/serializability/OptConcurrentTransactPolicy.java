package sapphire.policy.serializability;

import java.util.ArrayList;
import java.util.UUID;

import java.io.Serializable;

import sapphire.common.AppObject;
import sapphire.policy.DefaultSapphirePolicy;

/**
 * Created by Venugopal Reddy K 00900280 on 1/2/18.
 * Optimistic concurrent Transaction Policy allows for concurrent transactions unlike locking
 * transaction policy where only single client can perform transaction at any point of time.
 * Multiple clients can acquire token to perform transactions simultaneously. But the first
 * client who commits, succeed its transaction and others fail and rollback its transaction
 */

public class OptConcurrentTransactPolicy extends DefaultSapphirePolicy {

    public static class Transaction implements Serializable {
        private UUID TransactId;
        private AppObject cachedObject;

        public Transaction(UUID TransactId, AppObject cachedObject) {
            this.TransactId = TransactId;
            this.cachedObject = cachedObject;
        }

        public UUID getTransactId() {
            return TransactId;
        }

        public AppObject getCachedObject() {
            return cachedObject;
        }
    }


    public static class ClientPolicy extends DefaultClientPolicy {
        Transaction transaction;
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
                if (null != transaction) {
                    // Transaction based invocation. Invoke against the local copy
                    try {
                        return transaction.getCachedObject().invoke(method, params);
                    } catch (Exception e) {
                            rollbackTransaction();
                            throw new Exception("Exception occurred inside transaction.  Transaction rolled back.", e);
                    }
                } else {
                    // Non transactional based invocation. Make an RPC call
                    return super.onRPC(method, params);
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
            if (null == transaction) {
                transaction = ((ServerPolicy) getServer()).getTransaction();
            } else {
                throw new TransactionAlreadyStartedException("Transaction already started on Sapphire object.  Rollback or commit before starting a new transaction.");
            }
        }

        public synchronized void commitTransaction() throws Exception {
            if (null != transaction) {
                // Sync the local object to server
                try {
                    ((ServerPolicy) getServer()).syncObject(transaction.getTransactId(),
                            transaction.getCachedObject().getObject());
                }
                finally {
                    transaction = null;
                }
            } else {
                throw new NoTransactionStartedException("No transaction to commit");
            }
        }

        public synchronized void rollbackTransaction() throws Exception {
            if (null != transaction) {
                // Just release the reference to transaction
                transaction = null;
            } else {
                throw new NoTransactionStartedException("No transaction to roll back");
            }
        }
    }

    public static class ServerPolicy extends DefaultServerPolicy {
        private UUID TransactId;
        public ServerPolicy() {
            TransactId = UUID.randomUUID();
        }

        public Transaction getTransaction() throws Exception {
            return new Transaction(TransactId, sapphire_getAppObject());
        }

        synchronized public void syncObject(UUID transactId, Serializable object) throws Exception {

            if (transactId.equals(this.TransactId)) {
                // App object synchronization is allowed only when transaction id matches
                appObject.setObject(object);
                TransactId = UUID.randomUUID();
            }
            else {
                throw new Exception("Some other client updated the object. Transaction "
                            + transactId + " is invalid now. Get new transaction and try again.");
            }
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            // TODO: Should we update the transaction id here ? If no, we don't have to override it
            //TransactId = UUID.randomUUID();
            return super.onRPC(method, params);
        }
    }

    public static class GroupPolicy extends DefaultGroupPolicy {}
}

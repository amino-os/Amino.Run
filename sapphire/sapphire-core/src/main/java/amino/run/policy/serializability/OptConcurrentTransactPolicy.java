package amino.run.policy.serializability;

import amino.run.common.AppObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * Created by Venugopal Reddy K 00900280 on 1/2/18. Optimistic concurrent Transaction Policy allows
 * for concurrent transactions unlike locking transaction policy where only single client can
 * perform transaction at any point of time. Multiple clients can start the transactions
 * simultaneously. But the first client committing, succeed its transaction and others fail,
 * rollback their transaction.
 */
public class OptConcurrentTransactPolicy extends DefaultPolicy {

    private static byte[] calculateMessageDigest(Object appObject) throws TransactionException {
        ObjectOutputStream oos = null;
        byte[] digest = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DigestOutputStream dos = new DigestOutputStream(baos, MessageDigest.getInstance("MD5"));
            oos = new ObjectOutputStream(dos);
            oos.writeObject(appObject);
            digest = dos.getMessageDigest().digest();
        } catch (NoSuchAlgorithmException e) {
        } catch (IOException e) {
        } finally {
            if (null != oos) {
                try {
                    oos.close();
                } catch (IOException e) {

                }
            }
        }

        if (null == digest) {
            throw new TransactionException(
                    "Message Digest calculation exception. Start a new transaction again.");
        }

        return digest;
    }

    public static class ClientPolicy extends DefaultClientPolicy {
        private byte[]
                msgDigest; // Message digest of the app object prior to the modification of object
        // state
        private AppObject cachedObject; // app object

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
                if (null != cachedObject) {
                    // Transaction based invocation. Invoke against the local copy
                    try {
                        return cachedObject.invoke(method, params);
                    } catch (Exception e) {
                        rollbackTransaction();
                        throw new TransactionException(
                                "Exception occurred inside transaction. Transaction rolled back.",
                                e);
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
            AppObject localCachedObject = cachedObject;
            if (null == localCachedObject) {
                localCachedObject = ((ServerPolicy) getServer()).getAppObject();
                msgDigest = calculateMessageDigest(localCachedObject.getObject());
                cachedObject = localCachedObject;
            } else {
                throw new TransactionAlreadyStartedException(
                        "Transaction already started on Sapphire object.  Rollback or commit before starting a new transaction.");
            }
        }

        public synchronized void commitTransaction() throws Exception {
            byte[] localMsgDigest = msgDigest;
            AppObject localCachedObject = cachedObject;

            msgDigest = null;
            cachedObject = null;

            if (null != localCachedObject) {
                byte[] newMsgDigest = calculateMessageDigest(localCachedObject.getObject());
                if (MessageDigest.isEqual(localMsgDigest, newMsgDigest)) {
                    // App object is not modified. No need to sync object to server in this case
                    return;
                }

                // Sync the local object to server
                ((ServerPolicy) getServer())
                        .syncObject(localMsgDigest, localCachedObject.getObject());
            } else {
                throw new NoTransactionStartedException("No transaction to commit.");
            }
        }

        public synchronized void rollbackTransaction() throws Exception {
            if (null != cachedObject) {
                // Just release the references to app object and message digest
                cachedObject = null;
                msgDigest = null;
            } else {
                throw new NoTransactionStartedException("No transaction to rollback.");
            }
        }
    }

    public static class ServerPolicy extends DefaultServerPolicy {

        public AppObject getAppObject() throws Exception {
            return sapphire_getAppObject();
        }

        public synchronized void syncObject(byte[] msgDigest, Serializable object)
                throws Exception {
            /* TODO :  Have serialized object to generate the digest. This approach is slow. Need to be optimized later */
            byte[] oldDigest = calculateMessageDigest(sapphire_getAppObject().getObject());
            if (MessageDigest.isEqual(msgDigest, oldDigest)) {
                /* App object synchronization is allowed only when object snapshot has not been
                modified since the beginning of transaction */
                appObject.setObject(object);
            } else {
                throw new TransactionException(
                        "Some other client updated the object. "
                                + "Transaction is invalid now. Start a new transaction again.");
            }
        }

        @Override
        public synchronized Object onRPC(String method, ArrayList<Object> params) throws Exception {
            return super.onRPC(method, params);
        }
    }

    public static class GroupPolicy extends DefaultGroupPolicy {}
}

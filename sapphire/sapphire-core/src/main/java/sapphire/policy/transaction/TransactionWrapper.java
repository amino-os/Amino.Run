package sapphire.policy.transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

/**
 * wrapper to encapsulate a regular RPC data within a transaction
 */
public class TransactionWrapper {
    // outer RPC method which embeds the RPC call data inside the payload
    public final static String txWrapperTag = "tx_rpc";

    private UUID transactionId;
    private String rpcMethod;
    private ArrayList<Object> rpcParams;

    /**
     * constructs after parsing the inputs
     * @param method the rpc method name which indicates the call wrapped inside a transaxtion or not
     * @param params the rpc call parameters which may include the real rpc info inside of a transaction
     */
    public TransactionWrapper(String method, ArrayList<Object> params) {
        if (!method.equals(txWrapperTag)){
            this.set(null, method, params);
        } else {
            UUID transactionId = (UUID)params.get(0);
            ArrayList<Object> rpcPayload = (ArrayList<Object>)params.get(1);
            String rpcMethod = (String)rpcPayload.get(0);
            ArrayList<Object> rpcParams = (ArrayList<Object>)rpcPayload.get(1);

            this.set(transactionId, rpcMethod, rpcParams);
        }
    }

    public TransactionWrapper(UUID txnId, String method, ArrayList<Object> params) {
        this.set(txnId, method, params);
    }

    /**
     * gets the rpc params that wraps the original rpc data inside of a transaction
     * @param transactionId the id of the transaction
     * @param method the original rpc method name
     * @param params th eoriginal rpc params
     * @return the rpc params that of the transaction rpc encapsulating the original rpc
     */
    public static ArrayList<Object> getTransactionRPCParams(UUID transactionId, String method, ArrayList<Object> params) {
        ArrayList<Object> rpcOriginal = new ArrayList<Object>(Arrays.asList(method, params));
        ArrayList<Object> txnPayload = new ArrayList<Object>(Arrays.asList(transactionId, rpcOriginal));
        return txnPayload;
    }

    /**
     * gets the transaction-wrapped RPC call argument payload
     * @return the payload that goes with txWrappperTag as rpc method
     */
    public ArrayList<Object> getRPCParams() {
        return getTransactionRPCParams(this.transactionId, this.rpcMethod, this.rpcParams);
    }

    public UUID getTransaction() {
        return this.transactionId;
    }

    public String getInnerRPCMethod() {
        return this.rpcMethod;
    }

    public ArrayList<Object> getInnerRPCParams() {
        return this.rpcParams;
    }

    private void set(UUID txnId, String method, ArrayList<Object> params) {
        this.transactionId = txnId;
        this.rpcMethod = method;
        this.rpcParams = params;
    }
}

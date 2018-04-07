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

    public TransactionWrapper(String method, ArrayList<Object> params) {
        if (method.equals(txWrapperTag)){
            this.transactionId = (UUID)params.get(0);
            ArrayList<Object> rpcPayload = (ArrayList<Object>)params.get(1);
            this.rpcMethod = (String)rpcPayload.get(0);
            this.rpcParams = (ArrayList<Object>)rpcPayload.get(1);
        } else {
            this.rpcMethod = method;
            this.rpcParams = params;
            this.transactionId = null;
        }
    }

    public TransactionWrapper(UUID txnId, String method, ArrayList<Object> params) {
        this.transactionId = txnId;
        this.rpcMethod = method;
        this.rpcParams = params;
    }

    public ArrayList<Object> getRpcParams() {
        ArrayList<Object> rpcOriginal = new ArrayList<Object>(Arrays.asList(this.rpcMethod, this.rpcParams));
        ArrayList<Object> txnPayload = new ArrayList<Object>(Arrays.asList(this.transactionId, rpcOriginal));
        return txnPayload;
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
}

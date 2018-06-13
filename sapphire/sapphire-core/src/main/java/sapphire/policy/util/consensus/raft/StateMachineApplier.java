package sapphire.policy.util.consensus.raft;

import java.lang.Exception;

/**
 * Created by quinton on 3/20/18.
 */

public interface StateMachineApplier {
    /**
     * Apply an operation.
     * @param operation The operation to be applied.
     * @return The result of applying the operation.
     * @throws Exception Any exception arising from applying the operation.
     */
    // TODO: Suggest define it as `Object apply(RPC rpc)`
    public Object apply(Object operation) throws Exception;
}

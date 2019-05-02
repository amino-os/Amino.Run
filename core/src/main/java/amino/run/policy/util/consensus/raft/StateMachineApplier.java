package amino.run.policy.util.consensus.raft;

/** Created by quinton on 3/20/18. */
public interface StateMachineApplier {
    /**
     * Apply an operation.
     *
     * @param operation The operation to be applied.
     * @return The result of applying the operation.
     * @throws java.lang.Exception Any exception arising from applying the operation.
     */
    // TODO: Suggest define it as `Object apply(RPC rpc)`
    public Object apply(Object operation) throws java.lang.Exception;
}

package amino.run.policy.transaction;

/**
 * Exception of MicroService object that is not intended in transaction was engaged in transaction
 * somehow
 */
public class IllegalComponentException extends Exception {
    private static final String prefixMessage = "RPC call unexpected in transaction: ";

    public IllegalComponentException(String message) {
        super(prefixMessage + message);
    }
}

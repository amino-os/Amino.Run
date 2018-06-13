package sapphire.policy.transaction;

/**
 * Exception of Sapphire object that is not intended in transaction was engaged in transaction somehow
 */
public class IllegalComponentException extends Exception {
    private final static String prefixMessage = "RPC call unexpected in transaction: ";

    public IllegalComponentException(String message) {
        super(prefixMessage + message);
    }
}

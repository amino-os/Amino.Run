package sapphire.policy.transaction;

/** exception of transaction execution; used by transaction coordinator. */
public class TransactionExecutionException extends Exception {
    public TransactionExecutionException(String message, Exception e) {
        super(message, e);
    }
}

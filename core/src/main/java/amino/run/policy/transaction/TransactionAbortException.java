package amino.run.policy.transaction;

/** exception of transaction abort notification; client usually should catch this exception. */
public class TransactionAbortException extends Exception {
    private static final String prefix = "Distributed transaction has been rolled back. ";

    public TransactionAbortException(String message, Throwable cause) {
        super(prefix + message, cause);
    }
}

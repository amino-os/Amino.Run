package amino.run.policy.serializability;

/** Created by quinton on 1/21/18. */
public class NoTransactionStartedException extends Exception {
    public NoTransactionStartedException(String s) {
        super(s);
    }
}

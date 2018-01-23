package sapphire.policy.serializability;

/**
 * Created by quinton on 1/21/18.
 */

public class TransactionAlreadyStartedException extends Exception {
    public TransactionAlreadyStartedException(String s) {
        super(s);
    }
}

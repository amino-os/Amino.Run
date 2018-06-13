package sapphire.policy.serializability;

/**
 * Created by Venugopal Reddy K 00900280 on 8/3/18.
 */

public class TransactionException extends Exception {
	public TransactionException() {}

	public TransactionException(String message) {
		super(message);
	}

	public TransactionException(Throwable cause) {
		super(cause);
	}

	public TransactionException(String message, Throwable cause) { super(message, cause); }
}

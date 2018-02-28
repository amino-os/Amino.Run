package sapphire.policy.scalability;

/**
 * Created by SrinivasChilveri on 26/2/18.
 * Server overload exception
 */



public class ServerOverLoadException extends Exception {

	public ServerOverLoadException() {}

	public ServerOverLoadException(String message) {
		super(message);
	}

	public ServerOverLoadException(Throwable cause) {
		super(cause);
	}

	public ServerOverLoadException(String message, Throwable cause) { super(message, cause); }
}

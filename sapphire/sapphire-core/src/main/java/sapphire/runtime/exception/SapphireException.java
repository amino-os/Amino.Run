package sapphire.runtime.exception;

/**
 * SapphireException is the superclass of those exceptions that can be thrown
 * due to Sapphire runtime issues.
 *
 * @author terryz
 */
public class SapphireException extends Exception {

    public SapphireException() {
        super();
    }

    public SapphireException(String message) {
        super(message);
    }

    public SapphireException(String message, Throwable cause) {
        super(message, cause);
    }

    public SapphireException(Throwable cause) {
        super(cause);
    }

}

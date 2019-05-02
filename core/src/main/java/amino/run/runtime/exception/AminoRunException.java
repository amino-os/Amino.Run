package amino.run.runtime.exception;

/**
 * AminoRunException is the superclass of those exceptions that can be thrown due to Amino.Run
 * runtime issues.
 *
 * @author terryz
 */
public class AminoRunException extends Exception {

    public AminoRunException() {
        super();
    }

    public AminoRunException(String message) {
        super(message);
    }

    public AminoRunException(String message, Throwable cause) {
        super(message, cause);
    }

    public AminoRunException(Throwable cause) {
        super(cause);
    }
}

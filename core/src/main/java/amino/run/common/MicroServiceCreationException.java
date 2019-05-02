package amino.run.common;

/** Created by Venugopal Reddy K 00900280 on 16/7/18. */
public class MicroServiceCreationException extends Exception {

    public MicroServiceCreationException() {}

    public MicroServiceCreationException(String message) {
        super(message);
    }

    public MicroServiceCreationException(Throwable cause) {
        super(cause);
    }

    public MicroServiceCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}

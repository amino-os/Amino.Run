package sapphire.common;

/** Created by root1 on 16/7/18. */
public class SapphireObjectCreationException extends Exception {

    public SapphireObjectCreationException() {}

    public SapphireObjectCreationException(String message) {
        super(message);
    }

    public SapphireObjectCreationException(Throwable cause) {
        super(cause);
    }

    public SapphireObjectCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}

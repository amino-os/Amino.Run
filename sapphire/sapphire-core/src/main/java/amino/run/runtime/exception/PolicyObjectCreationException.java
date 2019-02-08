package amino.run.runtime.exception;

/**
 * Exception when failed to create policy objects at createPolicyObject.
 *
 * @author sungwookm
 */
public class PolicyObjectCreationException extends Exception {

    public PolicyObjectCreationException() {
        super();
    }

    public PolicyObjectCreationException(String message) {
        super(message);
    }

    public PolicyObjectCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PolicyObjectCreationException(Throwable cause) {
        super(cause);
    }
}

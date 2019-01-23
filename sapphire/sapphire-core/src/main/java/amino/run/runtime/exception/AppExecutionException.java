package amino.run.runtime.exception;

import amino.run.policy.Library;

/**
 * Exception thrown when method invocation on {@link Library.ServerPolicyLibrary#appObject} failed.
 *
 * <p>This exception is caused by application errors, not Sapphire errors. It indicates something
 * wrong in application. Good applications should <em>not</em> cause this exception on method
 * invocations.
 *
 * @author terryz
 */
public class AppExecutionException extends Exception {
    public AppExecutionException() {
        super();
    }

    public AppExecutionException(String message) {
        super(message);
    }

    public AppExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public AppExecutionException(Throwable cause) {
        super(cause);
    }
}

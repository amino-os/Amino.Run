package amino.run.policy.scalability;

/** Created by Venugopal Reddy k 00900280 on 2/21/18. Scale down exception */
public class ScaleDownException extends Exception {

    public ScaleDownException() {}

    public ScaleDownException(String message) {
        super(message);
    }

    public ScaleDownException(Throwable cause) {
        super(cause);
    }

    public ScaleDownException(String message, Throwable cause) {
        super(message, cause);
    }
}

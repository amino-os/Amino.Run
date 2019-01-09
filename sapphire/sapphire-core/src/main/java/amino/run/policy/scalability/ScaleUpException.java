package amino.run.policy.scalability;

/** Created by Venugopal Reddy k 00900280 on 2/21/18. Scale up exception */
public class ScaleUpException extends Exception {
    public ScaleUpException() {}

    public ScaleUpException(String message) {
        super(message);
    }

    public ScaleUpException(Throwable cause) {
        super(cause);
    }

    public ScaleUpException(String message, Throwable cause) {
        super(message, cause);
    }
}

package amino.run.common;

/** Created by Venugopal Reddy K 00900280 on 16/7/18. */
public class MicroServiceReplicaNotFoundException extends Exception {
    public MicroServiceReplicaNotFoundException() {}

    public MicroServiceReplicaNotFoundException(String message) {
        super(message);
    }

    public MicroServiceReplicaNotFoundException(Throwable cause) {
        super(cause);
    }

    public MicroServiceReplicaNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

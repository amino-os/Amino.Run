package sapphire.common;

/** Created by Venugopal Reddy K 00900280 on 16/7/18. */

/** Sapphire object replica not found exception */
public class SapphireObjectReplicaNotFoundException extends Exception {
    public SapphireObjectReplicaNotFoundException() {}

    public SapphireObjectReplicaNotFoundException(String message) {
        super(message);
    }

    public SapphireObjectReplicaNotFoundException(Throwable cause) {
        super(cause);
    }

    public SapphireObjectReplicaNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

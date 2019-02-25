package amino.run.app;

import java.io.Serializable;

/** Created by Venugopal Reddy K 00900280 on 28/8/18. */

/**
 * StatusReporter class. Extended by application class to become a Microservice
 *
 * @param <T>
 */
public class StatusReporter<T> implements Serializable {
    public boolean getStatus() {
        return true;
    }
}

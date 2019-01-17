package amino.run.app;

import java.io.Serializable;

/** Created by Venugopal Reddy K 00900280 on 28/8/18. */

/**
 * Abstract Sapphire Object class. Extended by application class to become a sapphire object
 *
 * @param <T>
 */
public class AbstractSapphireObject<T> implements Serializable {
    public boolean getStatus() {
        return true;
    }
}

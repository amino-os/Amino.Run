package sapphire.policy.checkpoint.explicitcheckpoint;

import java.io.Serializable;

/**
 * Created by quinton on 1/15/18.
 */

public class ExplicitCheckpointerImpl implements ExplicitCheckpointer, Serializable {
    @Override
    public void saveCheckpoint() throws Exception {}

    @Override
    public void restoreCheckpoint() throws Exception {}
}

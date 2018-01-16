package sapphire.policy.checkpoint.explicitcheckpoint;

/**
 * Created by quinton on 1/16/18.
 * This interface must be implemented by all SO's that use ExplicitCheckpointPoilcy.
 * A convenience implementation (ExplicitCheckpointerImpl) is provided, so that SO's
 * can just extend that.
 */

public interface ExplicitCheckpointer {
    public void saveCheckpoint() throws Exception;
    public void restoreCheckpoint() throws Exception;
}

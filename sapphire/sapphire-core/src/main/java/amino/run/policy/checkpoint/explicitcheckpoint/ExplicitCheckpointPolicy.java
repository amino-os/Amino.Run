package amino.run.policy.checkpoint.explicitcheckpoint;

import amino.run.policy.checkpoint.CheckpointPolicyBase;
import java.util.ArrayList;

/**
 * Created by quinton on 1/15/18.
 *
 * <p>Checkpoint to disk and restore from checkpoint with explicit calls from the application. The
 * SO MUST implement saveCheckpoint() and restoreCheckpoint() methods by implementing the
 * ExplicitCheckpointer interface (or simply extending the ExplicitCheckpointerImpl class). Note
 * that those methods are only called after the DM has completed the actual checkpoint or restore
 * operation and may therefore be generally be left empty, unless the SO needs to perform logging
 * and/or other similar operations. TODO: Perhaps improve this by e.g. using annotations instead,
 * and possibly supporting both pre- and post- operation hooks.
 */
public class ExplicitCheckpointPolicy extends CheckpointPolicyBase {
    public static class ClientPolicy extends CheckpointPolicyBase.ClientPolicy {}

    public static class ServerPolicy extends CheckpointPolicyBase.ServerPolicy {
        @Override
        public Object onRPC(
                String appMethod,
                ArrayList<Object> appParams,
                String nextDMMethod,
                ArrayList<Object> nextDMParams)
                throws Exception {
            if (isSaveCheckpoint(appMethod)) {
                this.saveCheckpoint();
                return null;
            } else if (isRestoreCheckpoint(appMethod)) {
                this.restoreCheckpoint();
                return null;
            } else {
                return super.onRPC(appMethod, appParams, nextDMMethod, nextDMParams);
            }
        }

        Boolean isSaveCheckpoint(String method) {
            // TODO better check than simple base name
            return method.contains(".saveCheckpoint(");
        }

        Boolean isRestoreCheckpoint(String method) {
            // TODO better check than simple base name
            return method.contains(".restoreCheckpoint(");
        }
    }

    public static class GroupPolicy extends CheckpointPolicyBase.GroupPolicy {}
}

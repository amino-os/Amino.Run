package sapphire.policy.checkpoint.periodiccheckpoint;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.checkpoint.CheckpointPolicyBase;

/**
 * Created by quinton on 1/15/18.
 *
 * Checkpoint to disk on every N successful RPC's, and restore from checkpoint on failure.
 * N defaults to 1.
 * TODO: Make N configurable. Update unit tests accordingly.
 **/
public class PeriodicCheckpointPolicy extends CheckpointPolicyBase {
    public static class ClientPolicy extends CheckpointPolicyBase.ClientPolicy {}

    public static class ServerPolicy extends CheckpointPolicyBase.ServerPolicy {
        protected int MAX_RPCS_BEFORE_CHECKPOINT = 1; // N
        protected int rpcsBeforeCheckpoint = MAX_RPCS_BEFORE_CHECKPOINT;

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            Object retVal = null;
            try {
                retVal = super.onRPC(method, params);
            }
            catch(Exception e) { // RPC threw an exception, so restore to previous snapshot.
                // If no previous snapshot exists, that exception will also go back to the client.
                this.restoreCheckpoint();  // If this throws an exception, it goes straight back to the client.
                /* TODO: Wrap both the above and the RPC exception in a new exception, so that the client
                   knows both why the RPC failed, and why the checkpoint restore failed.
                 */
                throw e; // The exception from the method invocation, as opposed to dealing with the snapshot.
            }
            // RPC did not generate exception, so consider it successful and possibly save a good checkpoint
            synchronized(this){
                if (--rpcsBeforeCheckpoint <= 0) {
                    this.saveCheckpoint(); // If this throws an exception, it goes straight back to the client.
                    rpcsBeforeCheckpoint = MAX_RPCS_BEFORE_CHECKPOINT; // Snapshot was successful, so reset counter.
                }
            }
            return retVal;

        }
    }

    public static class GroupPolicy extends CheckpointPolicyBase.GroupPolicy {}
}

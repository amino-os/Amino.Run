package sapphire.policy.checkpoint.durableserializable;


import java.util.ArrayList;

import sapphire.policy.checkpoint.CheckpointPolicyBase;
import sapphire.policy.checkpoint.periodiccheckpoint.PeriodicCheckpointPolicy;

/**
 * Created by quinton on 1/15/18.
 *
 * Durable serializable RPCs, revert to last successful RPC on failure.
 * Serialize RPC's and checkpoint to disk on every successful RPC.
 * Restore from checkpoint on failure.
 **/
public class DurableSerializableRPCPolicy extends PeriodicCheckpointPolicy {
    public static class ClientPolicy extends PeriodicCheckpointPolicy.ClientPolicy {}

    public static class ServerPolicy extends PeriodicCheckpointPolicy.ServerPolicy {

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            synchronized(this) { // ensure that all RPC's are serialized
                // Note that Java does not support multiple inheritance, so there is no good way
                // to inherit synchronization logic from SerializableRPCPolicy.  But fortunately it
                // is extremely simple, so we just re-implement synchronization here.
                return super.onRPC(method, params); // Does save and restore of checkpoint on failure.
            }
        }
    }

    public static class GroupPolicy extends PeriodicCheckpointPolicy.GroupPolicy {}
}

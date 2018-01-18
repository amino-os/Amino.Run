package sapphire.policy.serializability;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import sapphire.policy.DefaultSapphirePolicy;

/**
 * Serializes all RPCs to Sapphire object with server side locking.
 *
 * <em>Notes:</em>
 *
 * This implementation closely follows the DM definition by maintaining one lock
 * for the whole Sapphire object in which case <i>all operations</i> on this Sapphire
 * object will be serialized. In reality, developers may just want to serialize
 * invocations on one specific operation or a combination of a few operations.
 */
public class SerializableRPCPolicy extends DefaultSapphirePolicy {
    public static class ClientPolicy extends DefaultClientPolicy {}
    public static class ServerPolicy extends DefaultServerPolicy {

        @Override
        public synchronized Object onRPC(String method, ArrayList<Object> params) throws Exception {
                return appObject.invoke(method, params);
        }
    }

    public static class GroupPolicy extends DefaultGroupPolicy { }
}

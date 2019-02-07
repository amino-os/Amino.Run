package amino.run.policy.serializability;

import amino.run.common.AppObject;
import amino.run.policy.DefaultPolicy;
import java.util.ArrayList;

/**
 * Serializes all RPCs to Sapphire object with server side locking.
 *
 * <p><em>Notes:</em>
 *
 * <p>This implementation closely follows the DM definition by maintaining one lock for the whole
 * Sapphire object in which case <i>all RPCs</i> on this Sapphire object will be serialized.
 *
 * @author terryz
 */
public class SerializableRPCPolicy extends DefaultPolicy {
    public static class ClientPolicy extends DefaultClientPolicy {}

    public static class ServerPolicy extends DefaultServerPolicy {
        /**
         * Synchronize RPC calls on {@link AppObject}
         *
         * @param appMethod method name
         * @param appParams types of method parameters
         * @return return value of the method
         * @throws Exception
         */
        @Override
        public Object onRPC(
                String appMethod,
                ArrayList<Object> appParams,
                String nextDMMethod,
                ArrayList<Object> nextDMParams)
                throws Exception {
            return appObject.invoke(nextDMMethod, nextDMParams);
        }
    }

    public static class GroupPolicy extends DefaultGroupPolicy {}
}

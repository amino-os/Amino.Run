package sapphire.policy.primitive;

import java.util.ArrayList;

import sapphire.common.AppObject;
import sapphire.policy.DefaultSapphirePolicy;

/**
 * {@link ImmutablePolicy} provides efficient distribution and access for immutable SOs.
 *
 * This policy assumes that the SO is immutable and therefore caches a local copy on client
 * side and always invoke methods on local cached copy. It is up to the developer to ensure
 * that the object itself is immutable. This policy provides efficient access to immutable
 * SOs, it does not provide immutability.
 *
 * @author terryz
 */
public class ImmutablePolicy extends DefaultSapphirePolicy {
    public static class ClientPolicy extends DefaultClientPolicy{
        private AppObject cachedObject = null;

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            synchronized (this) {
                if (cachedObject == null) {
                    cachedObject = getServer().sapphire_getAppObject();
                }
            }

            return cachedObject.invoke(method, params);
        }
    }

    public static class ServerPolicy extends DefaultServerPolicy {}

    public static class GroupPolicy extends DefaultGroupPolicy{}
}

package sapphire.policy.primitive;

import java.util.ArrayList;

import sapphire.common.AppObject;
import sapphire.policy.DefaultSapphirePolicy;

/**
 * {@link ImmutablePolicy} provides efficient distribution and access for immutable SOs.
 *
 * This policy assumes that the SO is immutable and therefore caches a local copy on client
 * side and always invoke methods on local cached copy. It ups to the developer to ensure
 * that the object itself is immutable. This policy provides efficient access to immutable
 * SOs, it does not provide immutability.
 *
 * @author terryz
 */
public class ImmutablePolicy extends DefaultSapphirePolicy {
    public static class ClientPolicy extends DefaultClientPolicy{
        private ImmutablePolicy.ServerPolicy server;
        private AppObject cachedObject = null;

        @Override
        public void setServer(SapphireServerPolicy server) {
            this.server = (ImmutablePolicy.ServerPolicy) server;
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            synchronized (this) {
                if (cachedObject == null) {
                    cachedObject = server.getObject();
                }
            }

            return cachedObject.invoke(method, params);
        }
    }

    public static class ServerPolicy extends DefaultServerPolicy {
        public AppObject getObject() {
            return sapphire_getAppObject();
        }
    }

    public static class GroupPolicy extends DefaultGroupPolicy{}
}

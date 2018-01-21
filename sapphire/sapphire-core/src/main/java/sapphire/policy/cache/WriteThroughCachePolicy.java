package sapphire.policy.cache;

import java.util.ArrayList;

import sapphire.common.AppObject;
import sapphire.policy.DefaultSapphirePolicy;

/**
 * <code><WriteThroughCache</code> directs write operations onto cached object and through to
 * remote object before confirming write completion. Read operations will be invoked on cached
 * object directly.
 * <p>
 * State changes on remote object caused by one client will not automatically invalidate to
 * cached objects on other clients. Therefore <code>WriteThroughCache</code> may contain staled
 * object.
 *
 * @author terryz
 */
public class WriteThroughCachePolicy extends DefaultSapphirePolicy {

    public static class ClientPolicy extends DefaultClientPolicy {
        private ServerPolicy server;
        // TODO: Add a timeout for cachedObject
        private AppObject cachedObject = null;

        @Override
        public void setServer(SapphireServerPolicy server) {
            this.server = (ServerPolicy) server;
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            Object ret = null;

            if (isMethodMutable(method, params)) {
                ret = server.onRPC(method, params);
                cachedObject = server.getObject();
            } else {
                if (cachedObject == null) {
                    cachedObject = server.getObject();
                }

                ret = cachedObject.invoke(method, params);
            }

            return ret;
        }

        /**
         * Determines if the given method is immutable.
         *
         * @param method method name
         * @param params types of method parameters
         * @return <code>true</code> if the method is immutable; <code>false</code> otherwise
         */
        boolean isMethodMutable(String method, ArrayList<Object> params) {
            // TODO: Need to determine based on annotations on methods
            return true;
        }
    }

    public static class ServerPolicy extends DefaultServerPolicy {
        public AppObject getObject() {
            return sapphire_getAppObject();
        }
    }

    public static class GroupPolicy extends DefaultGroupPolicy {}
}


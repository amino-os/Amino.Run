package sapphire.policy.cache;

import java.util.ArrayList;
import java.util.logging.Logger;

import sapphire.common.AppObject;
import sapphire.policy.SapphirePolicy;

/**
 * <code><WriteThroughCache</code> directs write operations onto cached object and through to
 * remote object before confirming write completion. Read operations will be invoked on cached
 * object directly.
 * <p>
 * State changes on remote object caused by one client will not automatically invalidate to
 * cached objects on other clients. Therefore <code>WriteThroughCache</code> may contain staled
 * object.
 */
public class WriteThroughCachePolicy extends SapphirePolicy {

    public static class WriteThroughCacheClientPolicy extends SapphireClientPolicy {
        private WriteThroughCachePolicy.WriteThroughCacheServerPolicy server;
        private WriteThroughCachePolicy.WriteThroughCacheGroupPolicy group;
        // TODO: Add a timeout for cachedObject
        private AppObject cachedObject = null;

        @Override
        public void onCreate(SapphireGroupPolicy group) {
            this.group = (WriteThroughCachePolicy.WriteThroughCacheGroupPolicy) group;
        }

        @Override
        public SapphireGroupPolicy getGroup() {
            return group;
        }

        @Override
        public SapphireServerPolicy getServer() {
            return server;
        }

        @Override
        public void setServer(SapphireServerPolicy server) {
            this.server = (WriteThroughCachePolicy.WriteThroughCacheServerPolicy) server;
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
         * @param method
         * @param params
         * @return <code>true</code> if the method is immutable; <code>false</code> otherwise
         */
        boolean isMethodMutable(String method, ArrayList<Object> params) {
            // TODO: determine mutability of method based on annotation or other mechanism
            return true;
        }
    }

    public static class WriteThroughCacheServerPolicy extends SapphireServerPolicy {
        static private Logger logger = Logger.getLogger("sapphire.policy.cache.WriteThroughCachePolicy.WriteThroughCacheServerPolicy");
        private WriteThroughCachePolicy.WriteThroughCacheGroupPolicy group;

        @Override
        public void onCreate(SapphireGroupPolicy group) {
            this.group = (WriteThroughCachePolicy.WriteThroughCacheGroupPolicy) group;
        }

        @Override
        public SapphireGroupPolicy getGroup() {
            return group;
        }

        @Override
        public void onMembershipChange() {
        }

        public AppObject getObject() {
            return sapphire_getAppObject();
        }
    }

    public static class WriteThroughCacheGroupPolicy extends SapphireGroupPolicy {
        WriteThroughCachePolicy.WriteThroughCacheServerPolicy server;

        @Override
        public void addServer(SapphireServerPolicy server) {
            this.server = (WriteThroughCachePolicy.WriteThroughCacheServerPolicy) server;
        }

        @Override
        public void onFailure(SapphireServerPolicy server) {

        }

        @Override
        public SapphireServerPolicy onRefRequest() {
            return server;
        }

        @Override
        public ArrayList<SapphireServerPolicy> getServers() {
            return null;
        }

        @Override
        public void onCreate(SapphireServerPolicy server) {
            addServer(server);
        }
    }
}


package amino.run.policy.cache.explicitcaching;

import amino.run.common.AppObject;
import amino.run.policy.DefaultPolicy;
import java.io.Serializable;
import java.util.ArrayList;

// caching with explicit push and pull calls from the application
// The guarded SO MUST provide pull/push method body (placeholder) of ExplicitCacher interface
// TODO: stub generator to generate related methods
// TODO: inherit from a common caching base class which has getCopy/syncCopy methods
public class ExplicitCachingPolicy extends DefaultPolicy {
    public static class ExplicitCachingClientPolicy extends DefaultClientPolicy {
        private AppObject cachedCopy = null;

        private void pull() {
            AppObject appObject = ((ExplicitCachingServerPolicy) this.getServer()).getCopy();
            if (!(appObject.getObject() instanceof ExplicitCacher)) {
                throw new IllegalArgumentException("should be subclass of ExplicitCacher");
            }

            this.cachedCopy = appObject;
        }

        private void push() {
            if (this.cachedCopy != null) {
                ((ExplicitCachingServerPolicy) this.getServer())
                        .syncCopy(this.cachedCopy.getObject());
                this.cachedCopy = null;
            }
        }

        // for unit test use
        AppObject getCachedCopy() {
            return cachedCopy;
        }

        private Boolean isPull(String method) {
            // todo: stringent check more than simple base name
            return method.endsWith(".pull()");
        }

        private Boolean isPush(String method) {
            // todo: stringent check more than simple base name
            return method.endsWith(".push()");
        }

        @Override
        public Object onRPC(
                String appMethod,
                ArrayList<Object> appParams,
                String prevDMMethod,
                ArrayList<Object> prevDMParams)
                throws Exception {
            // All the operations between pull/push calls goes against local cache
            if (this.isPull(appMethod)) {
                this.pull();
                return null;
            } else if (this.isPush(appMethod)) {
                this.push();
                return null;
            } else if (this.cachedCopy != null) {
                return this.cachedCopy.invoke(appMethod, appParams);
            }

            /* When app object is explicitly not pulled, rpc calls are directed to server */
            return this.getServer().onRPC(appMethod, appParams, prevDMMethod, prevDMParams);
        }
    }

    public static class ExplicitCachingServerPolicy extends DefaultServerPolicy {
        public AppObject getCopy() {
            return this.appObject;
        }

        public void syncCopy(Serializable object) {
            appObject.setObject(object);
        }
    }

    public static class ExplicitCachingGroupPolicy extends DefaultGroupPolicy {}
}

package sapphire.policy.explicitcaching;

import sapphire.common.AppObject;
import sapphire.policy.DefaultSapphirePolicy;

import java.io.Serializable;
import java.util.ArrayList;

// caching with explicit push and pull calls from the application
// The guarded SO MUST provide pull/push method body (placeholder) of ExplicitCacher interface
// TODO: stub generator to generate related methods
// TODO: inherit from a common caching base class which has getCopy/syncCopy methods
public class ExplicitCachingPolicy extends DefaultSapphirePolicy{
    public static class ExplicitCachingClientPolicy extends DefaultClientPolicy {
        private AppObject cachedCopy = null;

        private void pull() {
            this.cachedCopy = ((ExplicitCachingServerPolicy)this.getServer()).getCopy();
        }

        private void push() {
            ((ExplicitCachingServerPolicy)this.getServer()).syncCopy(this.cachedCopy);
        }

        // for unit test use
        public void setCopy(AppObject copy) {
            this.cachedCopy = copy;
        }

        // for unit test use
        public AppObject getCachedCopy() {
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
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            // almost every op goes against local cache, except for pull/push
            if (this.isPull(method)) {
                this.cachedCopy = ((ExplicitCachingServerPolicy)this.getServer()).getCopy();
                return null;
            }

            if (this.isPush(method)) {
                if (this.cachedCopy != null){
                    ((ExplicitCachingServerPolicy)this.getServer()).syncCopy(this.cachedCopy.getObject());
                }
                return null;
            }

            if (this.cachedCopy == null) {
                this.cachedCopy = ((ExplicitCachingServerPolicy)this.getServer()).getCopy();
            }

            if (!((Object)this.cachedCopy.getObject() instanceof ExplicitCacher)) {
                throw new IllegalArgumentException("should be subclass of ExcplicitCacher");
            }

            return this.cachedCopy.invoke(method, params);
        }
    }

    public static class ExplicitCachingServerPolicy extends DefaultServerPolicy {
        public AppObject getCopy(){
            return this.appObject;
        }

        public void syncCopy(Serializable object) {
            appObject.setObject(object);
        }
    }

    public static class ExplicitCachingGroupPolicy extends DefaultGroupPolicy {}

}

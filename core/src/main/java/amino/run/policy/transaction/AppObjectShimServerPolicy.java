package amino.run.policy.transaction;

import amino.run.common.AppObject;
import amino.run.common.Utils;
import amino.run.policy.DefaultPolicy;
import amino.run.policy.scalability.masterslave.MethodInvocationRequest;
import amino.run.policy.scalability.masterslave.MethodInvocationResponse;

/** server policy that directly works on the deep-cloned copy of an AppObject */
public class AppObjectShimServerPolicy extends DefaultPolicy.DefaultServerPolicy {
    private AppObject originMaster;

    public MethodInvocationResponse onRPC(MethodInvocationRequest request) {
        throw new UnsupportedOperationException("onRPC not supported in AppObjectShimServerPolicy");
    }

    /**
     * gets the origin app object
     *
     * @return the origin app object
     */
    public AppObject getOriginMaster() {
        return originMaster;
    }

    private AppObjectShimServerPolicy(AppObject origin, AppObject sandbox) {
        this.originMaster = origin;
        this.appObject = sandbox;
    }

    /**
     * creates server policy which contains deep copy of the input app object
     *
     * @param appObject the origin app object
     */
    public static AppObjectShimServerPolicy cloneInShimServerPolicy(AppObject appObject)
            throws Exception {
        AppObject deepCloneAppObject = (AppObject) Utils.ObjectCloner.deepCopy(appObject);
        return new AppObjectShimServerPolicy(appObject, deepCloneAppObject);
    }
}

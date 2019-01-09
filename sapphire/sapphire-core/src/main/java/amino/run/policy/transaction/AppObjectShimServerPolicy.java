package amino.run.policy.transaction;

import amino.run.app.SapphireObjectSpec;
import amino.run.common.AppObject;
import amino.run.common.Utils;
import amino.run.policy.SapphirePolicy;
import amino.run.policy.SapphirePolicyUpcalls;
import amino.run.policy.scalability.masterslave.MethodInvocationRequest;
import amino.run.policy.scalability.masterslave.MethodInvocationResponse;
import java.util.ArrayList;

/** server policy that directly works on the deep-cloned copy of an AppObject */
public class AppObjectShimServerPolicy
        implements SapphirePolicyUpcalls.SapphireServerPolicyUpcalls {
    private AppObject appObject;
    private AppObject originMaster;

    @Override
    public void onCreate(SapphirePolicy.SapphireGroupPolicy group, SapphireObjectSpec spec) {}

    @Override
    public void onDestroy() {}

    @Override
    public SapphirePolicy.SapphireGroupPolicy getGroup() {
        return null;
    }

    @Override
    public Object onRPC(String method, ArrayList<Object> params) throws Exception {
        return appObject.invoke(method, params);
    }

    public MethodInvocationResponse onRPC(MethodInvocationRequest request) {
        throw new UnsupportedOperationException("onRPC not supported in AppObjectShimServerPolicy");
    }

    @Override
    public void onMembershipChange() {}

    /**
     * gets the app object referenced by the server policy
     *
     * @return the app object
     */
    public AppObject getAppObject() {
        return this.appObject;
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

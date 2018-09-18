package sapphire.policy.transaction;

import sapphire.common.AppObject;
import sapphire.common.Utils;
import sapphire.policy.SapphirePolicy;
import sapphire.policy.SapphirePolicyContainer;
import sapphire.policy.SapphirePolicyUpcalls.SapphireServerPolicyUpcalls;
import sapphire.policy.scalability.masterslave.MethodInvocationRequest;
import sapphire.policy.scalability.masterslave.MethodInvocationResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * server policy that directly works on the deep-cloned copy of an AppObject
 */
public class AppObjectShimServerPolicy implements SapphireServerPolicyUpcalls{
    private AppObject appObject;
    private AppObject originMaster;

    @Override
    public void onCreate(SapphirePolicy.SapphireGroupPolicy group) {

    }

    @Override
    public void initialize() {}

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
    public void onMembershipChange() {

    }

    @Override
    public List<SapphirePolicyContainer> getProcessedPolicies() {
        //TODO (8/21/18) Implement to return processedDMs.
        return null;
    }

    @Override
    public void setNextDMs(List<SapphirePolicyContainer> nextDMs) {}

    @Override
    public void setProcessedPolicies(List<SapphirePolicyContainer> setProcessedDMs) {}

    /**
     * gets the app object referenced by the server policy
     * @return the app object
     */
    public AppObject getAppObject() {
        return this.appObject;
    }

    /**
     * gets the origin app object
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
     *  creates server policy which contains deep copy of the input app object
     * @param appObject the origin app object
     */
    public static AppObjectShimServerPolicy cloneInShimServerPolicy(AppObject appObject) throws Exception {
        AppObject deepCloneAppObject = (AppObject)Utils.ObjectCloner.deepCopy(appObject);
        return new AppObjectShimServerPolicy(appObject, deepCloneAppObject);
    }
}

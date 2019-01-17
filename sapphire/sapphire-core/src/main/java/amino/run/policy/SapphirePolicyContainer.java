package amino.run.policy;

import amino.run.kernel.common.KernelObjectStub;
import java.io.Serializable;

/** Contains Sapphire policies. */
public class SapphirePolicyContainer implements Serializable {
    private String policyName;
    private SapphirePolicy.GroupPolicy getGroupPolicyStub;
    private SapphirePolicy.ServerPolicy serverPolicy;
    private KernelObjectStub serverPolicyStub;

    public SapphirePolicyContainer(
            String policyName, SapphirePolicy.GroupPolicy getGroupPolicyStub) {
        this.policyName = policyName;
        this.getGroupPolicyStub = getGroupPolicyStub;
    }

    public String getPolicyName() {
        return this.policyName;
    }

    public SapphirePolicy.GroupPolicy getGroupPolicyStub() {
        return this.getGroupPolicyStub;
    }

    public SapphirePolicy.ServerPolicy getServerPolicy() {
        return this.serverPolicy;
    }

    public KernelObjectStub getServerPolicyStub() {
        return this.serverPolicyStub;
    }

    public void setServerPolicy(SapphirePolicy.ServerPolicy serverPolicy) {
        this.serverPolicy = serverPolicy;
    }

    public void setServerPolicyStub(KernelObjectStub serverPolicyStub) {
        this.serverPolicyStub = serverPolicyStub;
    }
}

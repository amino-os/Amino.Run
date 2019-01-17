package amino.run.policy;

import amino.run.kernel.common.KernelObjectStub;
import java.io.Serializable;

/** Contains Sapphire policies. */
public class SapphirePolicyContainer implements Serializable {
    private String policyName;
    private Policy.GroupPolicy getGroupPolicyStub;
    private Policy.ServerPolicy serverPolicy;
    private KernelObjectStub serverPolicyStub;

    public SapphirePolicyContainer(String policyName, Policy.GroupPolicy getGroupPolicyStub) {
        this.policyName = policyName;
        this.getGroupPolicyStub = getGroupPolicyStub;
    }

    public String getPolicyName() {
        return this.policyName;
    }

    public Policy.GroupPolicy getGroupPolicyStub() {
        return this.getGroupPolicyStub;
    }

    public Policy.ServerPolicy getServerPolicy() {
        return this.serverPolicy;
    }

    public KernelObjectStub getServerPolicyStub() {
        return this.serverPolicyStub;
    }

    public void setServerPolicy(Policy.ServerPolicy serverPolicy) {
        this.serverPolicy = serverPolicy;
    }

    public void setServerPolicyStub(KernelObjectStub serverPolicyStub) {
        this.serverPolicyStub = serverPolicyStub;
    }
}

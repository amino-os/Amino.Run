package amino.run.policy;

import amino.run.kernel.common.KernelObjectStub;
import java.io.Serializable;

/** Contains Sapphire policies. */
public class SapphirePolicyContainer implements Serializable {
    private String policyName;
    private SapphirePolicy.SapphireGroupPolicy getGroupPolicyStub;
    private SapphirePolicy.SapphireServerPolicy serverPolicy;
    private KernelObjectStub serverPolicyStub;

    public SapphirePolicyContainer(
            String policyName, SapphirePolicy.SapphireGroupPolicy getGroupPolicyStub) {
        this.policyName = policyName;
        this.getGroupPolicyStub = getGroupPolicyStub;
    }

    public String getPolicyName() {
        return this.policyName;
    }

    public SapphirePolicy.SapphireGroupPolicy getGroupPolicyStub() {
        return this.getGroupPolicyStub;
    }

    public SapphirePolicy.SapphireServerPolicy getServerPolicy() {
        return this.serverPolicy;
    }

    public KernelObjectStub getServerPolicyStub() {
        return this.serverPolicyStub;
    }

    public void setServerPolicy(SapphirePolicy.SapphireServerPolicy serverPolicy) {
        this.serverPolicy = serverPolicy;
    }

    public void setServerPolicyStub(KernelObjectStub serverPolicyStub) {
        this.serverPolicyStub = serverPolicyStub;
    }
}

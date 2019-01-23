package amino.run.policy;

import amino.run.kernel.common.KernelObjectStub;
import java.io.Serializable;

/** Contains policies. */
public class PolicyContainer implements Serializable {
    private String policyName;
    private Policy.GroupPolicy groupPolicyStub;
    private Policy.ServerPolicy serverPolicy;
    private KernelObjectStub serverPolicyStub;
    private Policy.ClientPolicy clientPolicy;

    public PolicyContainer(String policyName, Policy.GroupPolicy getGroupPolicyStub) {
        this.policyName = policyName;
        this.groupPolicyStub = getGroupPolicyStub;
    }

    public String getPolicyName() {
        return this.policyName;
    }

    public Policy.GroupPolicy getGroupPolicyStub() {
        return this.groupPolicyStub;
    }

    public Policy.ServerPolicy getServerPolicy() {
        return this.serverPolicy;
    }

    public KernelObjectStub getServerPolicyStub() {
        return this.serverPolicyStub;
    }

    public Policy.ClientPolicy getClientPolicy() {
        return this.clientPolicy;
    }

    public void setServerPolicy(Policy.ServerPolicy serverPolicy) {
        this.serverPolicy = serverPolicy;
    }

    public void setServerPolicyStub(KernelObjectStub serverPolicyStub) {
        this.serverPolicyStub = serverPolicyStub;
    }

    public void setClientPolicy(Policy.ClientPolicy clientPolicy) {
        this.clientPolicy = clientPolicy;
    }
}

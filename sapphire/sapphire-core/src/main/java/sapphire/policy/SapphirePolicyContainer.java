package sapphire.policy;

import java.io.Serializable;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectStub;

/** Contains Sapphire policies. */
public class SapphirePolicyContainer implements Serializable {
    private String policyName;
    private SapphirePolicy.SapphireGroupPolicy getGroupPolicyStub;
    private SapphirePolicy.SapphireServerPolicy serverPolicy;
    private KernelObjectStub serverPolicyStub;
    private int oid;

    public SapphirePolicyContainer(
            String policyName, SapphirePolicy.SapphireGroupPolicy getGroupPolicyStub) {
        this.policyName = policyName;
        this.getGroupPolicyStub = getGroupPolicyStub;
    }

    public int getKernelOID() {
        return oid;
    }

    public void setKernelOID(KernelOID kernelOID) {
        this.oid = kernelOID.getID();
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

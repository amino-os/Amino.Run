package sapphire.policy;

import java.io.Serializable;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectStub;

/** Contains Sapphire policies. */
public interface SapphirePolicyContainer extends Serializable {
    String policyName = null;
    int oid = 0;
    SapphirePolicy.SapphireGroupPolicy groupPolicy = null;

    int getKernelOID();

    void setKernelOID(KernelOID kernelOID);

    String getPolicyName();

    SapphirePolicy.SapphireGroupPolicy getGroupPolicyStub();

    SapphirePolicy.SapphireServerPolicy getServerPolicy();

    KernelObjectStub getServerPolicyStub();

    void setServerPolicy(SapphirePolicy.SapphireServerPolicy serverPolicy);

    void setServerPolicyStub(KernelObjectStub serverPolicyStub);
}
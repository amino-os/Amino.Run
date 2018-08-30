package sapphire.policy;

import java.io.Serializable;

import sapphire.kernel.common.KernelObjectStub;

/**
 * Contains Sapphire policies.
 */
public interface SapphirePolicyContainer extends Serializable {
	String policyName = null;
	SapphirePolicy.SapphireGroupPolicy groupPolicy = null;

	String getPolicyName ();
	SapphirePolicy.SapphireGroupPolicy getGroupPolicy();
	SapphirePolicy.SapphireServerPolicy getServerPolicy();
	KernelObjectStub getServerPolicyStub();
	void setServerPolicy(SapphirePolicy.SapphireServerPolicy serverPolicy);
	void setServerPolicyStub(KernelObjectStub serverPolicyStub);
}
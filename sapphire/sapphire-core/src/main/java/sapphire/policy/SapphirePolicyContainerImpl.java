package sapphire.policy;

/**
 * Contains Sapphire policies.
 */
public class SapphirePolicyContainerImpl implements SapphirePolicyContainer {
	private String policyName;
	private SapphirePolicy.SapphireGroupPolicy groupPolicy;

	public SapphirePolicyContainerImpl(String policyName, SapphirePolicy.SapphireGroupPolicy groupPolicy) {
		this.policyName = policyName;
		this.groupPolicy = groupPolicy;
	}

	public String GetPolicyName () {
		return this.policyName;
	}

	public SapphirePolicy.SapphireGroupPolicy GetGroupPolicy() {
		return this.groupPolicy;
	}
}
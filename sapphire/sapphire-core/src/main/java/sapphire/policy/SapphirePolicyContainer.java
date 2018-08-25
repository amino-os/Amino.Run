package sapphire.policy;

/**
 * Contains Sapphire policies.
 */
public class SapphirePolicyContainer extends DefaultSapphirePolicyUpcallImpl {
	String policyName;
	SapphirePolicy.SapphireGroupPolicy groupPolicy;

	public SapphirePolicyContainer(String policyName, SapphirePolicy.SapphireGroupPolicy groupPolicy) {
		this.policyName = policyName;
		this.groupPolicy = groupPolicy;
	}
}
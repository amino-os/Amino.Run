package sapphire.policy;

import java.io.Serializable;

/**
 * Contains Sapphire policies.
 */
public interface SapphirePolicyContainer extends Serializable {
	String policyName = null;
	SapphirePolicy.SapphireGroupPolicy groupPolicy = null;

	String GetPolicyName ();
	SapphirePolicy.SapphireGroupPolicy GetGroupPolicy();

}
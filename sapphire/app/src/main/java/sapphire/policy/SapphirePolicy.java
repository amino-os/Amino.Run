package sapphire.policy;

/**
 *  Class that describes how Sapphire Policies look like. Each policy should extend this class.
 *  Each Sapphire Policy contains a Server Policy, a Client Policy and a Group Policy.
 *  The Policies contain a set of internal functions (used by the sapphire runtime system), a 
 *  set of upcall functions that are called when an event happened and a set of functions that
 *  implement the sapphire API for policies. 
 */
public abstract class SapphirePolicy extends DefaultSapphirePolicyUpcallImpl {
	public abstract static class SapphireClientPolicy extends DefaultSapphireClientPolicyUpcallImpl {

	}
	
	public abstract static class SapphireServerPolicy extends DefaultSapphireServerPolicyUpcallImpl {
	    
	}

	public abstract static class SapphireGroupPolicy extends DefaultSapphireGroupPolicyUpcallImpl {

	}
}
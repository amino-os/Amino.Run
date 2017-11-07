package sapphire.policy;

import java.rmi.RemoteException;
import java.util.ArrayList;

import sapphire.common.AppObject;
import sapphire.kernel.common.KernelOID;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;
import sapphire.policy.SapphirePolicyLibrary.SapphireGroupPolicyLibrary;
import sapphire.policy.SapphirePolicyLibrary.SapphireServerPolicyLibrary;
import sapphire.runtime.Sapphire;

public abstract class DefaultSapphirePolicyUpcallImpl extends SapphirePolicyLibrary {

	public abstract static class DefaultSapphireClientPolicyUpcallImpl extends SapphireClientPolicyLibrary {
		public Object onRPC(String method, ArrayList<Object> params) throws Exception {
			/* The default behavior is to just perform the RPC to the Policy Server */
			Object ret = null;
			
			try {
				ret = getServer().onRPC(method, params);
			} catch (RemoteException e) {
				setServer(getGroup().onRefRequest());
			}
			return ret;
		}
	}
	
	public abstract static class DefaultSapphireServerPolicyUpcallImpl extends SapphireServerPolicyLibrary {
		public Object onRPC(String method, ArrayList<Object> params) throws Exception {
			/* The default behavior is to just invoke the method on the Sapphire Object this Server Policy Object manages */
			return appObject.invoke(method, params);
		}
	}
	
	public abstract static class DefaultSapphireGroupPolicyUpcallImpl extends SapphireGroupPolicyLibrary {
		
		/*
		 * INTERNAL FUNCTIONS (Used by Sapphire runtime)
		 */
		public void $__initialize(String appObjectClassName, ArrayList<Object> params) {
			this.appObjectClassName = appObjectClassName;
			this.params = params;
		}
		
		public SapphireServerPolicy onRefRequest() {
			return getServers().get(0);
		}
	}
}
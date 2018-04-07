package sapphire.policy;

import sapphire.policy.SapphirePolicy.SapphireServerPolicy;
import sapphire.policy.transaction.TransactionContext;
import sapphire.policy.transaction.I2PCClient;
import sapphire.policy.transaction.IllegalComponentException;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.UUID;

public abstract class DefaultSapphirePolicyUpcallImpl extends SapphirePolicyLibrary {

	public abstract static class DefaultSapphireClientPolicyUpcallImpl extends SapphireClientPolicyLibrary {
		public Object onRPC(String method, ArrayList<Object> params) throws Exception {
			// only transaction-capable SO is allowed in DCAP transaction -- change of the original behavior
			if (!(this instanceof I2PCClient) &&  this.hasTransaction()) {
				throw new IllegalComponentException();
			}

			/* The default behavior is to just perform the RPC to the Policy Server */
			Object ret = null;
			
			try {
				ret = getServer().onRPC(method, params);
			} catch (RemoteException e) {
				// TODO: Quinton: This looks like a bug.  RemoteExceptions are silently swallowed and null is returned.
				setServer(getGroup().onRefRequest());
			}
			return ret;
		}

		protected UUID getCurrentTransaction() {
			return TransactionContext.getCurrentTransaction();
		}


		protected boolean hasTransaction() {
			return this.getCurrentTransaction() != null;
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
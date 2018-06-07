package sapphire.policy;

import sapphire.policy.SapphirePolicy.SapphireServerPolicy;
import sapphire.policy.dmchain.DMChainManager;
import sapphire.policy.transaction.IllegalComponentException;
import sapphire.policy.transaction.TransactionContext;
import sapphire.policy.transaction.TwoPCClient;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.UUID;

public abstract class DefaultSapphirePolicyUpcallImpl extends SapphirePolicyLibrary {

	public abstract static class DefaultSapphireClientPolicyUpcallImpl extends SapphireClientPolicyLibrary {
		DMChainManager dmChainManager;

		public Object onRPC(String method, ArrayList<Object> params) throws Exception {
			// only transaction-capable SO is allowed in DCAP transaction -- change of the original behavior
			if (!(this instanceof TwoPCClient) &&  this.hasTransaction()) {
				throw new IllegalComponentException(method);
			}

			/* The default behavior is to just perform the RPC to the Policy Server */
			Object ret = null;
			
			try {
				SapphirePolicy.SapphireClientPolicy clientPolicy = dmChainManager.getNextClient();

				if (clientPolicy == null) {
					ret = getServer().onRPC(method, params);
				} else {
					ret = clientPolicy.onRPC(method, params);
				}
			} catch (RemoteException e) {
				// TODO: Quinton: This looks like a bug.  RemoteExceptions are silently swallowed and null is returned.
				setServer(getGroup().onRefRequest());
			}
			return ret;
		}

		public void setDMChainManager(DMChainManager dmChainManager) {
			this.dmChainManager = dmChainManager;
		}

		protected UUID getCurrentTransaction() {
			return TransactionContext.getCurrentTransaction();
		}


		protected boolean hasTransaction() {
			return this.getCurrentTransaction() != null;
		}
	}
	
	public abstract static class DefaultSapphireServerPolicyUpcallImpl extends SapphireServerPolicyLibrary {
		DMChainManager dmChainManager;

		public Object onRPC(String method, ArrayList<Object> params) throws Exception {

			SapphirePolicy.SapphireServerPolicy serverPolicy = dmChainManager.getNextServer();
			if (serverPolicy == null) {
			/* The default behavior is to just invoke the method on the Sapphire Object this Server Policy Object manages */
				return appObject.invoke(method, params);
			} else {
				return serverPolicy.onRPC(method, params);
			}
		}

		public void setDMChainManager(DMChainManager dmChainManager) {
			this.dmChainManager = dmChainManager;
		}


		/* This function is added here just to generate the stub for this function in all DMs server policy */
		public SapphireServerPolicy sapphire_replicate() {
			return super.sapphire_replicate();
		}
		/* This function is added here just to generate the stub for this function in all DMs server policy */
		public void sapphire_pin(String region) throws RemoteException {
			super.sapphire_pin(region);
		}
		/* This function is added here just to generate the stub for this function in all DMs server policy */
		public void sapphire_pin_to_server(InetSocketAddress server) throws RemoteException {
			super.sapphire_pin_to_server(server);
		}
	}
	
	public abstract static class DefaultSapphireGroupPolicyUpcallImpl extends SapphireGroupPolicyLibrary {
		DMChainManager dmChainManager;

		/*
		 * INTERNAL FUNCTIONS (Used by Sapphire runtime)
		 */
		public void $__initialize(String appObjectClassName, ArrayList<Object> params) {
			this.appObjectClassName = appObjectClassName;
			this.params = params;
		}
		public void setDMChainManager(DMChainManager dmChainManager) {
			this.dmChainManager = dmChainManager;
		}


		public SapphireServerPolicy onRefRequest() {
			return getServers().get(0);
		}
	}
}

package sapphire.policy;

import sapphire.kernel.server.KernelObject;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;
import sapphire.policy.transaction.IllegalComponentException;
import sapphire.policy.transaction.TransactionContext;
import sapphire.policy.transaction.TwoPCClient;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class DefaultSapphirePolicyUpcallImpl extends SapphirePolicyLibrary {

	public abstract static class DefaultSapphireClientPolicyUpcallImpl extends SapphireClientPolicyLibrary {
		public Object onRPC(String method, ArrayList<Object> params) throws Exception {
			// only transaction-capable SO is allowed in DCAP transaction -- change of the original behavior
			if (!(this instanceof TwoPCClient) &&  this.hasTransaction()) {
				throw new IllegalComponentException(method);
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
			StringBuilder sb = new StringBuilder();
			for (Object param : params) {
				sb.append(param.toString() + ",");
			}

			if (nextServerKernelObject == null) {
				/* The default behavior is to just invoke the method on the Sapphire Object this Server Policy Object manages */
				System.out.println("No next policy. Invoking: " + method + " with " + sb);
				return appObject.invoke(method, params);
			} else {
				System.out.println("invoking next server policy for " + method + " with " + sb);
				return nextServerKernelObject.invoke(method, params);
			}
		}

		public void setNextServerKernelObject(KernelObject sapphireServerPolicy) {
			this.nextServerKernelObject = sapphireServerPolicy;
		}

		public void setNextServerPolicy(SapphireServerPolicy sapphireServerPolicy) {
			this.nextServerPolicy = sapphireServerPolicy;
		}

		public void setPreviousServerPolicy(SapphireServerPolicy sapphireServerPolicy) {
			this.previousServerPolicy = sapphireServerPolicy;
		}

		/* This function is added here just to generate the stub for this function in all DMs server policy */
		public SapphireServerPolicy sapphire_replicate() {
			return super.sapphire_replicate();
		}
		public SapphireServerPolicy sapphire_replicate(List<SapphirePolicyContainer> processedDMs) {
			return super.sapphire_replicate(processedDMs);
		}

		/* This function is added here just to generate the stub for this function in all DMs server policy */
		public void sapphire_pin(String region) throws RemoteException {
			super.sapphire_pin(region);
		}

		/* This function is added here just to generate the stub for this function in all DMs server policy */
		public void sapphire_pin_to_server(InetSocketAddress server) throws RemoteException {
			super.sapphire_pin_to_server(server);
		}

		/* This function is added here just to generate the stub for this function in all DMs server policy */
		public void sapphire_pin_to_server(SapphireServerPolicy sapphireServerPolicy, InetSocketAddress server) throws RemoteException {
			super.sapphire_pin_to_server(sapphireServerPolicy, server);
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

// TODO (8/29/2018) Implement or discard onRPC at Default Group policy.
//		public Object onRPC(String method, ArrayList<Object> params) throws Exception {
////			return appObject.invoke(method, params);
//
//			if (nextServerKernelObject == null) {
//				/* The default behavior is to just invoke the method on the Sapphire Object this Server Policy Object manages */
//				return appObject.invoke(method, params);
//			} else {
////				return methods.get(method).invoke(object, params.toArray());
//				String newMethod = (String)params.get(0);
//				ArrayList<Object> nextParams = (ArrayList<Object>) params.get(1);
//				return nextServerKernelObject.onRPC(newMethod, nextParams);
//			}
//		}

		public SapphireServerPolicy onRefRequest() {
			return getServers().get(0);
		}
	}
}

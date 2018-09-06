package sapphire.policy;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.apache.harmony.rmi.common.RMIUtil;

import sapphire.common.AppObject;
import sapphire.common.AppObjectStub;
import sapphire.compiler.GlobalStubConstants;
import sapphire.compiler.PolicyStub;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectNotCreatedException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.server.KernelObject;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;
import sapphire.policy.primitive.ImmutablePolicy;
import sapphire.runtime.Sapphire;

public abstract class SapphirePolicyLibrary implements SapphirePolicyUpcalls {
	public static abstract class SapphireClientPolicyLibrary implements SapphireClientPolicyUpcalls {
		/*
		 * INTERNAL FUNCTIONS (Used by sapphire runtime system)
		 */
	}

	public static abstract class SapphireServerPolicyLibrary implements SapphireServerPolicyUpcalls {
		protected AppObject appObject;
		protected AppObjectStub appObjectStub;
		protected KernelOID oid;

		static Logger logger = Logger.getLogger("sapphire.policy.SapphirePolicyLibrary");
		protected KernelObject nextServerKernelObject;
		protected SapphireServerPolicy nextServerPolicy;
		protected SapphireServerPolicy previousServerPolicy;
		// TODO (8/29/2018) Remove thisPolicyContainer if it is not necessary.
		protected SapphirePolicyContainer thisPolicyContainer;
		protected List<SapphirePolicyContainer> nextDMs = new ArrayList<SapphirePolicyContainer>();
		protected List<SapphirePolicyContainer> processedDMs = new ArrayList<SapphirePolicyContainer>();

		private OMSServer oms() {
			return GlobalKernelReferences.nodeServer.oms;
		}

		private KernelServerImpl kernel() {
			return GlobalKernelReferences.nodeServer;
		}

		/*
		 * SAPPHIRE API FOR SERVER POLICIES
		 */

		public List<SapphirePolicyContainer> getProcessedPolicies() {
			return this.processedDMs;
		}

		public void setThisPolicyContainer(SapphirePolicyContainer policyContainer) {
			this.thisPolicyContainer = policyContainer;
		}

		public void setNextDMs(List<SapphirePolicyContainer> nextDMs) {
			this.nextDMs = nextDMs;
		}

		public void setProcessedPolicies(List<SapphirePolicyContainer> processedDMs) {
			this.processedDMs = processedDMs;
		}

		/**
		 * Creates a replica of this server and registers it with the group		 *
		 */
		// TODO: Also replicate the policy ??
		// TODO (8/29/2018): Remove or edit for single policy case. This is not used by multiple policy scenario.
		public SapphireServerPolicy sapphire_replicate() {
			KernelObjectStub serverPolicyStub = null;
			String policyStubClassName = GlobalStubConstants.getPolicyPackageName() + "." + RMIUtil.getShortName(this.getClass()) + GlobalStubConstants.STUB_SUFFIX;
			try {
				serverPolicyStub = (KernelObjectStub) KernelObjectFactory.create(policyStubClassName);
				SapphireServerPolicy serverPolicy = (SapphireServerPolicy) kernel().getObject(serverPolicyStub.$__getKernelOID());
				serverPolicy.$__initialize(appObject);
				serverPolicy.$__setKernelOID(serverPolicyStub.$__getKernelOID());
				serverPolicy.onCreate(getGroup());
				getGroup().addServer((SapphireServerPolicy)serverPolicyStub);
				//TODO (8/21/18): set processedDMs.
				Sapphire.createPolicy(null, null, null, null, serverPolicy, serverPolicyStub,null);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new Error("Could not find the class for replication!");
			} catch (KernelObjectNotCreatedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new Error("Could not create a replica!");
			} catch (KernelObjectNotFoundException e) {
				e.printStackTrace();
				throw new Error("Could not find object to replicate!");
			} catch (Exception e) {
				e.printStackTrace();
				throw new Error("Unknown exception occurred!");
			}
			return (SapphireServerPolicy) serverPolicyStub;
		}

		/**
		 * Creates a replica of this server and registers it with the group.
		 */
		public SapphireServerPolicy sapphire_replicate(List<SapphirePolicyContainer> processedPolicies) {
			KernelObjectStub serverPolicyStub = null;
			SapphireServerPolicy serverPolicy = null;

			// Construct list of policies that will come after this policy on the server side.
			try {
				// Find the appStub which only exists in the last server policy (first in client side).
				SapphireServerPolicy lastServerPolicy = (SapphireServerPolicy)this;
				while (lastServerPolicy.nextServerPolicy != null) {
					lastServerPolicy = lastServerPolicy.nextServerPolicy;
				}

				AppObject actualAppObject = lastServerPolicy.sapphire_getAppObject();
				List<SapphirePolicyContainer> processedPolicesReplica = new ArrayList<SapphirePolicyContainer>();

				///////////////////////////
				//TODO: remove after test:
//				newHostName = this.getProcessedPolicies().get(0).getServerPolicyStub().$__getHostname();
				///////////////////////////

				Sapphire.createPolicy(null, actualAppObject, processedPolicies, processedPolicesReplica, null, null, null);

				// Last policy is this policy.
				serverPolicy = processedPolicesReplica.get(processedPolicesReplica.size() - 1).getServerPolicy();
				serverPolicyStub = processedPolicesReplica.get(processedPolicesReplica.size() - 1).getServerPolicyStub();

				List<SapphirePolicyContainer> nextPolicyList = Sapphire.createPolicy(null, null, this.nextDMs, processedPolicesReplica, serverPolicy, serverPolicyStub,null);

				getGroup().addServer((SapphireServerPolicy) serverPolicyStub);

				if (nextPolicyList != null && nextPolicyList.size() > 0) {
					serverPolicy = nextPolicyList.get(nextPolicyList.size() - 1).getServerPolicy();
				}
			} catch (RemoteException e) {
				throw new Error("Could not contact oms.");
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new Error("Could not find the class for replication!");
			} catch (KernelObjectNotCreatedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new Error("Could not create a replica!");
			} catch (KernelObjectNotFoundException e) {
				e.printStackTrace();
				throw new Error("Could not find object to replicate!");
			} catch (Exception e) {
				e.printStackTrace();
				throw new Error("Unknown exception occurred!");
			}
			return serverPolicy;
		}

		public AppObject sapphire_getAppObject() {
			return appObject;
		}

		public AppObjectStub sapphire_getAppObjectStub() {
			return appObjectStub;
		}

		public void sapphire_pin(String region) throws RemoteException {
			logger.info("Pinning Sapphire object " + oid.toString() + " to " + region);
			InetSocketAddress server = null;
			try {
				server = oms().getServerInRegion(region);
			} catch (RemoteException e) {
				throw new RemoteException("Could not contact oms.");
			}
			sapphire_pin_to_server(server);
		}

		// This function is same as sapphire_pin but pining to the server instead of region
		public void sapphire_pin_to_server(InetSocketAddress server) throws RemoteException {
			logger.info("Pinning Sapphire object " + oid.toString() + " to " + server);
			try {
				kernel().moveKernelObjectToServer(server, oid);
			} catch (KernelObjectNotFoundException e) {
				e.printStackTrace();
				throw new Error("Could not find myself on this server!");
			}
		}

		// pin first server policy to given kernel server.
		public void sapphire_pin_to_server(SapphireServerPolicy serverPolicy, InetSocketAddress server) throws RemoteException {
			List<SapphireServerPolicy> serverPoliciesToRemove = new ArrayList<SapphireServerPolicy>();

			if (serverPolicy == null) {
				throw new Error("No server policy to pin to the server: " + server);
			}

			while (serverPolicy.previousServerPolicy != null) {
				serverPolicy = serverPolicy.previousServerPolicy;
			}

			SapphireServerPolicy firstServerPolicy = serverPolicy;

			while (serverPolicy.nextServerPolicy != null) {
				// First server policy will be removed after object was moved; therefore, not needed to be included in the removal list.
				serverPolicy = serverPolicy.nextServerPolicy;
				System.out.println(" Adding object to removal list for " + serverPolicy.$__getKernelOID());
				serverPoliciesToRemove.add(serverPolicy);
			}
			serverPolicy = firstServerPolicy;

			// Before pinning the Sapphire Object replica, need to update the Hostname.
			// Ignore the first Policy as it will be same as the current one for which replication is being performed.
			List<SapphirePolicyContainer> processedDMList = serverPolicy.getProcessedPolicies().subList(1, serverPolicy.getProcessedPolicies().size());
			Iterator<SapphirePolicyContainer> itr = processedDMList.iterator();
			KernelObjectStub tempServerPolicyStub = null;
			while(itr.hasNext()) {
				tempServerPolicyStub = itr.next().getServerPolicyStub();
				System.out.println("Updating hostname of serverPolicyStub: " + tempServerPolicyStub + " to server: " + server);
				tempServerPolicyStub.$__updateHostname(server);
			}

			logger.info("Pinning Sapphire object " + serverPolicy.$__getKernelOID() + " to " + server);
			try {
				kernel().moveKernelObjectToServer(server, serverPolicy.$__getKernelOID());
			} catch (KernelObjectNotFoundException e) {
				e.printStackTrace();
				throw new Error("Could not find myself on this server!");
			}

			// Remove all of the associated objects with the first server policy.
			for (SapphireServerPolicy serverPolicyToRemove : serverPoliciesToRemove) {
				try {
					kernel().removeObject(serverPolicyToRemove.$__getKernelOID());
				} catch (KernelObjectNotFoundException e) {
					e.printStackTrace();
					throw new Error("Could not find object to remove in this server. Oid: " + serverPolicyToRemove.$__getKernelOID());
				}
			}
		}

		/*
		 * INTERNAL FUNCTIONS
		 */
		/**
		 * Internal function used to initialize the App Object
		 *
		 * @param appObjectClassName
		 * @param params
		 */
		//TODO: not final (stub overrides it)
		public AppObjectStub $__initialize(Class<?> appObjectStubClass, Object[] params) {
			AppObjectStub actualAppObject = null; // The Actual App Object, managed by an AppObject Handler
			try {
				// Construct the list of classes of the arguments as Class[]
				if (params != null) {
					Class<?>[] argClasses = Sapphire.getParamsClasses(params);
					actualAppObject = (AppObjectStub) appObjectStubClass.getConstructor(argClasses).newInstance(params);
				}
				else
					actualAppObject = (AppObjectStub) appObjectStubClass.newInstance();

				actualAppObject.$__initialize(true);

				// Create the App Object
				appObject = new AppObject(actualAppObject);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return actualAppObject;
		}

		public String sapphire_getRegion() {
			return kernel().getRegion();
		}
		public void $__initialize(AppObject appObject) {
			this.appObject = appObject;
		}

		public void $__initialize(AppObjectStub appObjectStub) {
			this.appObjectStub = appObjectStub;
		}

		public void $__setKernelOID(KernelOID oid) {
			this.oid = oid;
		}

		public KernelOID $__getKernelOID() {
			return oid;
		}

		public InetSocketAddress sapphire_locate_kernel_object(KernelOID oid) throws RemoteException {
			InetSocketAddress addr;
			try {
				addr = oms().lookupKernelObject(oid);
			} catch (RemoteException e) {
				throw new RemoteException("Could not contact oms.");
			}
			catch (KernelObjectNotFoundException e) {
				e.printStackTrace();
				throw new Error("Could not find myself on this server!");
			}
			return addr;
		}
	}

	public static abstract class SapphireGroupPolicyLibrary implements SapphireGroupPolicyUpcalls {
		protected String appObjectClassName;
		protected ArrayList<Object> params;
		protected KernelOID oid;

		protected OMSServer oms() {
			return GlobalKernelReferences.nodeServer.oms;
		}

		/* 
		 * SAPPHIRE API FOR GROUP POLICIES
		 */

		public ArrayList<String> sapphire_getRegions() throws RemoteException {
			return oms().getRegions();
		}

		/**
		 * Gets the list of inet sock address of the servers in the specified region
		 *
		 * @param region
		 * @return inet socket address of the server
		 * @throws RemoteException
		 */
		public ArrayList<InetSocketAddress> sapphire_getServersInRegion(String region) throws RemoteException {
			return oms().getServersInRegion(region);
		}
		public void $__setKernelOID(KernelOID oid) {
			this.oid = oid;
		}
		public KernelOID $__getKernelOID() {
			return this.oid;
		}
	}
}

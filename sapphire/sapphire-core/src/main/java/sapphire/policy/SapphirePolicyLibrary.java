package sapphire.policy;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.harmony.rmi.common.RMIUtil;

import sapphire.common.AppObject;
import sapphire.common.AppObjectStub;
import sapphire.compiler.GlobalStubConstants;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectNotCreatedException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;
import sapphire.runtime.Sapphire;

public abstract class SapphirePolicyLibrary implements SapphirePolicyUpcalls {
	public static abstract class SapphireClientPolicyLibrary implements SapphireClientPolicyUpcalls {
		/*
		 * INTERNAL FUNCTIONS (Used by sapphire runtime system)
		 */
	}

	public static abstract class SapphireServerPolicyLibrary implements SapphireServerPolicyUpcalls {
		protected AppObject appObject;
		protected KernelOID oid;
		static Logger logger = Logger.getLogger("sapphire.policy.SapphirePolicyLibrary");

		private OMSServer oms() {
			return GlobalKernelReferences.nodeServer.oms;
		}

		private KernelServerImpl kernel() {
			return GlobalKernelReferences.nodeServer;
		}

		/*
		 * SAPPHIRE API FOR SERVER POLICIES
		 */

		/**
		 * Creates a replica of this server and registers it with the group
		 */
		// TODO: Also replicate teh policy ??
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
			}
			return (SapphireServerPolicy) serverPolicyStub;
		}

		public AppObject sapphire_getAppObject() {
			return appObject;
		}

		public void sapphire_pin(String region) throws RemoteException {
			logger.info("Pinning Sapphire object " + oid.toString() + " to " + region);
			InetSocketAddress server = null;
			try {
				server = oms().getServerInRegion(region);
			} catch (RemoteException e) {
				throw new RemoteException("Could not contact oms.");
			}

			try {
				kernel().moveKernelObjectToServer(server, oid);
			} catch (KernelObjectNotFoundException e) {
				e.printStackTrace();
				throw new Error("Could not find myself on this server!");
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

		public void $__initialize(AppObject appObject) {
			this.appObject = appObject;
		}

		public void $__setKernelOID(KernelOID oid) {
			this.oid = oid;
		}

		public KernelOID $__getKernelOID() {
			return oid;
		}
	}

	public static abstract class SapphireGroupPolicyLibrary implements SapphireGroupPolicyUpcalls {
		protected String appObjectClassName;
		protected ArrayList<Object> params;
		protected KernelOID oid;

		private OMSServer oms() {
			return GlobalKernelReferences.nodeServer.oms;
		}

		/* 
		 * SAPPHIRE API FOR GROUP POLICIES
		 */

		public ArrayList<String> sapphire_getRegions() throws RemoteException {
			return oms().getRegions();
		}

		public void $__setKernelOID(KernelOID oid) {
			this.oid = oid;
		}
	}
}

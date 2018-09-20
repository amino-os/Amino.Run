package sapphire.policy;

import java.lang.annotation.Annotation;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import org.apache.harmony.rmi.common.RMIUtil;
import sapphire.common.AppObject;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.common.SapphireReplicaID;
import sapphire.compiler.GlobalStubConstants;
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
import sapphire.runtime.EventHandler;
import sapphire.runtime.Sapphire;

public abstract class SapphirePolicyLibrary implements SapphirePolicyUpcalls {
    public abstract static class SapphireClientPolicyLibrary
            implements SapphireClientPolicyUpcalls {
        /*
         * INTERNAL FUNCTIONS (Used by sapphire runtime system)
         */
    }

    public abstract static class SapphireServerPolicyLibrary
            implements SapphireServerPolicyUpcalls {
        protected AppObject appObject;
		protected AppObjectStub appObjectStub;
        protected KernelOID oid;
        protected SapphireReplicaID replicaId;

        static Logger logger = Logger.getLogger("sapphire.policy.SapphirePolicyLibrary");
		protected KernelObject nextServerKernelObject;
		protected SapphireServerPolicy nextServerPolicy;
		protected SapphireServerPolicy previousServerPolicy;
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

		public SapphireServerPolicy getPreviousServerPolicy() {
			return this.previousServerPolicy;
		}
		public SapphireServerPolicy getNextServerPolicy() {
			return this.nextServerPolicy;
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

		public void setNextDMs(List<SapphirePolicyContainer> nextDMs) {
			this.nextDMs = nextDMs;
		}

		public void setProcessedPolicies(List<SapphirePolicyContainer> processedDMs) {
			this.processedDMs = processedDMs;
		}
        /** Creates a replica of this server and registers it with the group */
        // TODO: Also replicate the policy ??
        public SapphireServerPolicy sapphire_replicate() throws RemoteException {
            KernelObjectStub serverPolicyStub = null;
            String policyStubClassName =
                    GlobalStubConstants.getPolicyPackageName()
                            + "."
                            + RMIUtil.getShortName(this.getClass())
                            + GlobalStubConstants.STUB_SUFFIX;
            try {
                serverPolicyStub =
                        (KernelObjectStub) KernelObjectFactory.create(policyStubClassName);
                SapphireServerPolicy serverPolicy =
                        (SapphireServerPolicy)
                                kernel().getObject(serverPolicyStub.$__getKernelOID());
                serverPolicy.$__initialize(appObject);
                serverPolicy.$__setKernelOID(serverPolicyStub.$__getKernelOID());

                /* Register the handler for this replica to OMS */
                SapphireReplicaID replicaId =
                        oms().registerSapphireReplica(getGroup().getSapphireObjId());
                serverPolicy.setReplicaId(replicaId);
                ((SapphireServerPolicy) serverPolicyStub).setReplicaId(replicaId);
                ArrayList<Object> policyObjList = new ArrayList();
                policyObjList.add(serverPolicyStub);
                EventHandler replicaHandler =
                        new EventHandler(
                                GlobalKernelReferences.nodeServer.getLocalHost(), policyObjList);
                oms().setSapphireReplicaDispatcher(replicaId, replicaHandler);
                /* Here getClass() gives the Applications Object Stub class so we should use getSuperclass to get the actual Application class
                  for example getClass() gives as class sapphire.appexamples.hankstodo.app.stubs.TodoListManager_Stub
                  getClass().getSuperclass() gives as class sapphire.appexamples.hankstodo.app.TodoListManager
                */
                Class c = sapphire_getAppObject().getObject().getClass().getSuperclass();
                serverPolicy.onCreate(getGroup(), c.getAnnotations());
                getGroup().addServer((SapphireServerPolicy) serverPolicyStub);
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
            } catch (SapphireObjectNotFoundException e) {
                KernelObjectFactory.delete(serverPolicyStub.$__getKernelOID());
                e.printStackTrace();
                throw new Error("Could not find sapphire object on OMS");
            } catch (SapphireObjectReplicaNotFoundException e) {
                KernelObjectFactory.delete(serverPolicyStub.$__getKernelOID());
                e.printStackTrace();
                throw new Error("Could not find sapphire object replica on OMS");
            } catch (RemoteException e) {
                sapphire_remove_replica();
                e.printStackTrace();
                throw new Error("Could not create a replica of " + appObject.getObject(), e);
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

				AppObject actualAppObject = lastServerPolicy.sapphire_getAppObject();
				if (actualAppObject == null) throw new Exception("Could not find AppObject");

				// Create a new replica chain from already created policies before this policy and this policy.
				List<SapphirePolicyContainer> processedPolicesReplica = new ArrayList<SapphirePolicyContainer>();
				Sapphire.createPolicy(null, actualAppObject, processedPolicies, processedPolicesReplica, null, null, null);

				// Last policy in the returned chain is replica of this policy.
				serverPolicy = processedPolicesReplica.get(processedPolicesReplica.size() - 1).getServerPolicy();
				serverPolicyStub = processedPolicesReplica.get(processedPolicesReplica.size() - 1).getServerPolicyStub();

				// Complete the chain by creating new instances of server policies and stub that should be created after this policy.
				List<SapphirePolicyContainer> nextPolicyList = Sapphire.createPolicy(null, null, this.nextDMs, processedPolicesReplica, serverPolicy, serverPolicyStub,null);

				String ko = "";
				if (nextPolicyList != null) {
					for (SapphirePolicyContainer policyContainer : nextPolicyList) {
						ko += String.valueOf(policyContainer.getKernelOID()) + ",";
					}
				}
				System.out.println("OID from sapphire_replicate: " + ko);
				getGroup().addServer((SapphireServerPolicy) serverPolicyStub);
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
			return (SapphireServerPolicy) serverPolicyStub;
		}

        public AppObject sapphire_getAppObject() {
            return appObject;
        }

		public AppObjectStub sapphire_getAppObjectStub() {
            return appObjectStub;
        }

        public void sapphire_pin(String region)
                throws RemoteException, SapphireObjectNotFoundException,
                        SapphireObjectReplicaNotFoundException {
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
        public void sapphire_pin_to_server(InetSocketAddress server)
                throws RemoteException, SapphireObjectNotFoundException,
                        SapphireObjectReplicaNotFoundException {
            logger.info("Pinning Sapphire object " + oid.toString() + " to " + server);
            try {
                kernel().moveKernelObjectToServer(server, oid);
            } catch (KernelObjectNotFoundException e) {
                e.printStackTrace();
                throw new Error("Could not find myself on this server!");
            }
        }

		// Pin First server policy and inturn the associated KernelObjects to the given kernel server.
		public void sapphire_pin_to_server(SapphireServerPolicy serverPolicyStub, InetSocketAddress server) throws RemoteException {
			List<SapphireServerPolicy> serverPoliciesToRemove = new ArrayList<SapphireServerPolicy>();

            KernelOID serverOID = serverPolicyStub.$__getKernelOID();
            SapphireServerPolicy serverPolicy;
            try {
                serverPolicy = (SapphireServerPolicy) GlobalKernelReferences.nodeServer.getObject(serverOID);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RemoteException("No server policy to pin to the server: " + server);
            }

			// Ensure that we are inturn handling the first Server Policy.
			while (serverPolicy.getPreviousServerPolicy() != null) {
				serverPolicy = serverPolicy.getPreviousServerPolicy();
			}
			SapphireServerPolicy firstServerPolicy = serverPolicy;

			// Create a list of associated ServerPolicies which needs to be explicitly removed from the local KernelServer.
			// These associated ServerPolicy KernelObjects will be moved to the new Server when the first KernelObject is moved.
			// But have to be explicitly removed from the local KernelServer. The new KernelServer address need to be
			// registered with the OMS explicitly for these associated KernelObjects.
			while (serverPolicy.getNextServerPolicy() != null) {
				// First server policy will be removed when the object is moved; therefore, not needed to be included in the removal list.
				serverPolicy = serverPolicy.getNextServerPolicy();
				serverPoliciesToRemove.add(serverPolicy);
			}
			serverPolicy = firstServerPolicy;

			// Before pinning the Sapphire Object replica to the provided KernelServer, need to update the Hostname.
			List<SapphirePolicyContainer> processedPolicyList = serverPolicy.getProcessedPolicies();
			Iterator<SapphirePolicyContainer> itr = processedPolicyList.iterator();
			KernelObjectStub tempServerPolicyStub = null;
			while(itr.hasNext()) {
				tempServerPolicyStub = itr.next().getServerPolicyStub();
				tempServerPolicyStub.$__updateHostname(server);
			}

			System.out.println("(Starting) Pinning Sapphire object " + serverPolicy.$__getKernelOID() + " to " + server);
			try {
				kernel().moveKernelObjectToServer(server, serverPolicy.$__getKernelOID());
			} catch (KernelObjectNotFoundException e) {
				e.printStackTrace();
				throw new Error("Could not find myself on this server!");
			} catch (SapphireObjectNotFoundException e) {

            } catch (SapphireObjectReplicaNotFoundException e) {

            }

			// Register the moved associated KernelObjects to OMS with the new KernelServer address.
			// Remove the moved associated KernelObjects from the local KernelServer.
			for (SapphireServerPolicy serverPolicyToRemove : serverPoliciesToRemove) {
				try {
					oms().registerKernelObject(serverPolicyToRemove.$__getKernelOID(), server);
					kernel().removeObject(serverPolicyToRemove.$__getKernelOID());
				} catch (KernelObjectNotFoundException e) {
					e.printStackTrace();
					throw new Error("Could not find object to remove in this server. Oid: " + serverPolicyToRemove.$__getKernelOID());
				}
			}
			System.out.println("(Complete) Pinning Sapphire object " + serverPolicy.$__getKernelOID() + " to " + server);
		}
		
        public void sapphire_remove_replica() throws RemoteException {
            try {
                oms().unRegisterSapphireReplica(getReplicaId());
            } catch (SapphireObjectNotFoundException e) {
                /* Sapphire object not found */
                e.printStackTrace();
            }
            KernelObjectFactory.delete($__getKernelOID());
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
        // TODO: not final (stub overrides it)
        public AppObjectStub $__initialize(Class<?> appObjectStubClass, Object[] params) {
            AppObjectStub actualAppObject =
                    null; // The Actual App Object, managed by an AppObject Handler
            try {
                // Construct the list of classes of the arguments as Class[]
                if (params != null) {
                    Class<?>[] argClasses = Sapphire.getParamsClasses(params);
                    actualAppObject =
                            (AppObjectStub)
                                    appObjectStubClass
                                            .getConstructor(argClasses)
                                            .newInstance(params);
                } else actualAppObject = (AppObjectStub) appObjectStubClass.newInstance();

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

        public void setReplicaId(SapphireReplicaID rid) {
            replicaId = rid;
        }

        public SapphireReplicaID getReplicaId() {
            return replicaId;
        }

        public InetSocketAddress sapphire_locate_kernel_object(KernelOID oid)
                throws RemoteException {
            InetSocketAddress addr;
            try {
                addr = oms().lookupKernelObject(oid);
            } catch (RemoteException e) {
                throw new RemoteException("Could not contact oms.");
            } catch (KernelObjectNotFoundException e) {
                e.printStackTrace();
                throw new Error("Could not find myself on this server!");
            }
            return addr;
        }
    }

    public abstract static class SapphireGroupPolicyLibrary implements SapphireGroupPolicyUpcalls {
        protected String appObjectClassName;
        protected ArrayList<Object> params;
        protected Annotation[] appConfigAnnotation;
        protected KernelOID oid;
        protected SapphireObjectID sapphireObjId;

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
        public ArrayList<InetSocketAddress> sapphire_getServersInRegion(String region)
                throws RemoteException {
            return oms().getServersInRegion(region);
        }

        public void $__setKernelOID(KernelOID oid) {
            this.oid = oid;
        }

        public KernelOID $__getKernelOID() {
            return this.oid;
        }

        public void setAppConfigAnnotation(Annotation[] appConfigAnnotation) {
            this.appConfigAnnotation = appConfigAnnotation;
        }

        public Annotation[] getAppConfigAnnotation() {
            return appConfigAnnotation;
        }

        public void setSapphireObjId(SapphireObjectID sapphireId) {
            sapphireObjId = sapphireId;
        }

        public SapphireObjectID getSapphireObjId() {
            return sapphireObjId;
        }

        public SapphireServerPolicy addReplica(
                SapphireServerPolicy replicaSource, InetSocketAddress dest)
                throws RemoteException, SapphireObjectNotFoundException,
                        SapphireObjectReplicaNotFoundException {
            SapphireServerPolicy replica = replicaSource.sapphire_replicate();
            try {
                replica.sapphire_pin_to_server(dest);
                ((KernelObjectStub) replica).$__updateHostname(dest);
            } catch (Exception e) {
                try {
                    removeReplica(replica);
                } catch (Exception innerException) {
                }
                throw e;
            }
            return replica;
        }

        public void removeReplica(SapphireServerPolicy server)
                throws RemoteException, SapphireObjectReplicaNotFoundException,
                        SapphireObjectNotFoundException {
            server.sapphire_remove_replica();
            removeServer(server);
        }

        public void onDestroy() throws RemoteException {
            /* Delete all the servers */
            ArrayList<SapphireServerPolicy> servers = getServers();
            if (servers == null) {
                return;
            }

            for (Iterator<SapphireServerPolicy> itr = servers.iterator(); itr.hasNext(); ) {
                SapphireServerPolicy server = itr.next();
                try {
                    server.sapphire_remove_replica();
                    itr.remove();
                } catch (Exception e) {

                }
            }

            // TODO: Need retry upon failures ??

            try {
                oms().unRegisterSapphireObject(getSapphireObjId());
            } catch (SapphireObjectNotFoundException e) {
                /* Sapphire object not found */
                e.printStackTrace();
            }

            KernelObjectFactory.delete($__getKernelOID());
        }
    }
}

package sapphire.policy;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.harmony.rmi.common.RMIUtil;
import sapphire.app.Language;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.AppObject;
import sapphire.common.AppObjectStub;
import sapphire.common.GraalObject;
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
        protected Map<String, SapphirePolicyConfig> configMap;
        protected SapphirePolicy.SapphireGroupPolicy group;
        protected SapphireObjectSpec spec;

        static Logger logger = Logger.getLogger("sapphire.policy.SapphirePolicyLibrary");

        // SeverPolicy calls Kernel object in the chain - this is transparent call which will either
        // invoke method in the next server policy or app object.
        protected KernelObject nextServerKernelObject;

        // ServerPolicy that comes after the current policy in the server side chain - this order is
        // reverse in the client side.
        protected SapphireServerPolicy nextServerPolicy;

        // ServerPolicy that precedes the current policy in the server side chain - this order is
        // reverse in the client side.
        protected SapphireServerPolicy previousServerPolicy;

        // List of ServerPolicies that should be created in the chain after the current one when
        // creating replicas.
        // These nested part of chain where the last one created will be called by KernelServer
        // (farthest from actual app object).
        // It means these were the last in order in the client side of chain. New groups should be
        // created for this list of chain.
        protected List<SapphirePolicyContainer> nextPolicies =
                new ArrayList<SapphirePolicyContainer>();

        // List of ServerPolicies that were created previously. They are upper level in group
        // hierarchy. Therefore, this list of chain
        // should not create new group policies. When creating replicas, group stub information
        // stored in this chain will be copied over
        // to the new replicas so that they can reference the same group stubs.
        protected List<SapphirePolicyContainer> processedPolicies =
                new ArrayList<SapphirePolicyContainer>();

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
            return this.processedPolicies;
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

        public void setNextPolicies(List<SapphirePolicyContainer> nextPolicies) {
            this.nextPolicies = nextPolicies;
        }

        public void setProcessedPolicies(List<SapphirePolicyContainer> processedPolicies) {
            this.processedPolicies = processedPolicies;
        }

        public void setSapphireObjectSpec(SapphireObjectSpec spec) {
            this.spec = spec;
        }

        @Override
        public void onCreate(
                SapphirePolicy.SapphireGroupPolicy group,
                Map<String, SapphirePolicyConfig> configMap) {
            this.group = group;
            this.configMap = configMap;
        }

        /**
         * Returns configurations of this server policy.
         *
         * @return sapphire policy configuration map
         */
        public Map<String, SapphirePolicyConfig> getConfigMap() {
            return this.configMap;
        }

        /** Creates a replica of this server and registers it with the group. */
        public SapphireServerPolicy sapphire_replicate(
                List<SapphirePolicyContainer> processedPolicies) throws RemoteException {
            KernelObjectStub serverPolicyStub = null;
            SapphireServerPolicy serverPolicy = null;

            // Construct list of policies that will come after this policy on the server side.
            try {
                // Find the appStub which only exists in the last server policy (first in client
                // side).
                SapphireServerPolicy lastServerPolicy = (SapphireServerPolicy) this;

                Class appObjectClass =
                        sapphire_getAppObject().getObject().getClass().getSuperclass();
                AppObject actualAppObject = lastServerPolicy.sapphire_getAppObject();
                if (actualAppObject == null) throw new Exception("Could not find AppObject");

                // Create a new replica chain from already created policies before this policy and
                // this policy.
                List<SapphirePolicyContainer> processedPoliciesReplica =
                        new ArrayList<SapphirePolicyContainer>();
                Sapphire.createPolicy(
                        spec,
                        configMap,
                        processedPolicies,
                        processedPoliciesReplica,
                        null,
                        null,
                        null);

                // Last policy in the returned chain is replica of this policy.
                serverPolicy =
                        processedPoliciesReplica
                                .get(processedPoliciesReplica.size() - 1)
                                .getServerPolicy();
                serverPolicyStub =
                        processedPoliciesReplica
                                .get(processedPoliciesReplica.size() - 1)
                                .getServerPolicyStub();

                // Complete the chain by creating new instances of server policies and stub that
                // should be created after this policy.
                List<SapphirePolicyContainer> nextPolicyList =
                        Sapphire.createPolicy(
                                spec,
                                configMap,
                                this.nextPolicies,
                                processedPoliciesReplica,
                                serverPolicy,
                                serverPolicyStub,
                                null);

                String ko = "";
                if (nextPolicyList != null) {
                    for (SapphirePolicyContainer policyContainer : nextPolicyList) {
                        ko += String.valueOf(policyContainer.getKernelOID()) + ",";
                    }
                }

                getGroup().addServer((SapphireServerPolicy) serverPolicyStub);
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                logger.severe(e.getMessage());
                throw new Error("Could not find the class for replication!", e);
            } catch (KernelObjectNotCreatedException e) {
                // TODO Auto-generated catch block
                logger.severe(e.getMessage());
                throw new Error("Could not create a replica!", e);
            } catch (KernelObjectNotFoundException e) {
                logger.severe(e.getMessage());
                throw new Error("Could not find object to replicate!", e);
            } catch (SapphireObjectNotFoundException e) {
                KernelObjectFactory.delete(serverPolicyStub.$__getKernelOID());
                logger.severe(e.getMessage());
                throw new Error("Could not find sapphire object on OMS", e);
            } catch (SapphireObjectReplicaNotFoundException e) {
                KernelObjectFactory.delete(serverPolicyStub.$__getKernelOID());
                logger.severe(e.getMessage());
                throw new Error("Could not find sapphire object replica on OMS", e);
            } catch (RemoteException e) {
                sapphire_remove_replica(processedPolicies);
                logger.severe(e.getMessage());
                throw new Error("Could not create a replica of " + appObject.getObject(), e);
            } catch (Exception e) {
                logger.severe(e.getMessage());
                throw new Error("Unknown exception occurred!", e);
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
                logger.severe(e.getMessage());
                throw new RemoteException("Could not contact oms.", e);
            }
            sapphire_pin_to_server(server);
        }

        // This function is same as sapphire_pin but pining to the server instead of region
        // TODO (2018-9-26, Sungwook), Remove this method after updating all policies to use below
        // method.
        public void sapphire_pin_to_server(InetSocketAddress server)
                throws RemoteException, SapphireObjectNotFoundException,
                        SapphireObjectReplicaNotFoundException {
            logger.info("Pinning Sapphire object " + oid.toString() + " to " + server);
            try {
                kernel().moveKernelObjectToServer(server, oid);
            } catch (KernelObjectNotFoundException e) {
                logger.severe(e.getMessage());
                throw new Error("Could not find myself on this server!", e);
            }
        }

        /*
           Pin server policies in the chain.
           1) Checks if there is server policy to pin to the new host.
           2) Obtain the first server policy (farthest from app object) by moving the pointer in the chain.
           3) Navigate through the chain to find all server policy information that need to be removed after move.
           4) Copy the chain of server policy to the new host.
           5) Remove the server policies in the local chain that were moved.
        */
        public void sapphire_pin_to_server(
                SapphireServerPolicy serverPolicyStub, InetSocketAddress server)
                throws RemoteException, SapphireObjectNotFoundException,
                        SapphireObjectReplicaNotFoundException {
            List<SapphireServerPolicy> serverPoliciesToRemove =
                    new ArrayList<SapphireServerPolicy>();

            KernelOID serverOID = serverPolicyStub.$__getKernelOID();
            SapphireServerPolicy serverPolicy;
            try {
                serverPolicy =
                        (SapphireServerPolicy)
                                GlobalKernelReferences.nodeServer.getObject(serverOID);
            } catch (Exception e) {
                logger.severe(e.getMessage());
                throw new RemoteException("No server policy to pin to the server: " + server, e);
            }

            // Ensure that we start from the first Server Policy.
            while (serverPolicy.getPreviousServerPolicy() != null) {
                serverPolicy = serverPolicy.getPreviousServerPolicy();
            }
            SapphireServerPolicy firstServerPolicy = serverPolicy;

            /**
             * Create a list of associated ServerPolicies which needs to be explicitly removed from
             * the local KernelServer. These associated ServerPolicy KernelObjects will be moved to
             * the new Server when the first KernelObject is moved. The remaining KernelObject in
             * the local KernelServer should be explicitly removed The new KernelServer address
             * needs to be registered with the OMS explicitly for these associated KernelObjects.
             */
            while (serverPolicy.getNextServerPolicy() != null) {
                // First server policy will be removed when the object is moved; therefore, not
                // needed to be included in the removal list.
                // TODO(2018-9-28, Sungwook): Removal logic should be in the same place; therefore,
                // move the logic to  kernelserver.moveKernelObjectToServer()
                serverPolicy = serverPolicy.getNextServerPolicy();
                serverPoliciesToRemove.add(serverPolicy);
            }
            serverPolicy = firstServerPolicy;

            // Before pinning the Sapphire Object replica to the provided KernelServer, need to
            // update the Hostname.
            List<SapphirePolicyContainer> processedPolicyList = serverPolicy.getProcessedPolicies();
            Iterator<SapphirePolicyContainer> itr = processedPolicyList.iterator();
            KernelObjectStub tempServerPolicyStub = null;
            while (itr.hasNext()) {
                tempServerPolicyStub = itr.next().getServerPolicyStub();
                tempServerPolicyStub.$__updateHostname(server);
            }

            logger.info(
                    "(Starting) Pinning Sapphire object "
                            + serverPolicy.$__getKernelOID()
                            + " to "
                            + server);
            try {
                kernel().moveKernelObjectToServer(server, serverPolicy.$__getKernelOID());
            } catch (KernelObjectNotFoundException e) {
                logger.severe(e.getMessage());
                throw new Error("Could not find myself on this server!", e);
            } catch (SapphireObjectNotFoundException e) {
                logger.severe(e.getMessage());
                throw new Error("Could not find Sapphire object on this server!", e);
            } catch (SapphireObjectReplicaNotFoundException e) {
                logger.severe(e.getMessage());
                throw new Error("Could not find Sapphire replica on this server!", e);
            }

            // Register the moved associated KernelObjects to OMS with the new KernelServer address.
            // Then, remove the associated KernelObjects from the local KernelServer.
            // TODO(2018-9-28, Sungwook): Removal logic should be in the same place; therefore, move
            // the logic to  kernelserver.moveKernelObjectToServer()
            for (SapphireServerPolicy serverPolicyToRemove : serverPoliciesToRemove) {
                try {
                    oms().registerKernelObject(serverPolicyToRemove.$__getKernelOID(), server);
                    kernel().removeObject(serverPolicyToRemove.$__getKernelOID());
                } catch (KernelObjectNotFoundException e) {
                    logger.severe(e.getMessage());
                    throw new Error(
                            "Could not find object to remove in this server. Oid: "
                                    + serverPolicyToRemove.$__getKernelOID(),
                            e);
                }
            }
            logger.info(
                    "(Complete) Pinning Sapphire object "
                            + serverPolicy.$__getKernelOID()
                            + " to "
                            + server);
        }

        // TODO (2018-9-26, Sungwook) Remove after verification.
        public void sapphire_remove_replica() throws RemoteException {
            try {
                oms().unRegisterSapphireReplica(getReplicaId());
            } catch (SapphireObjectNotFoundException e) {
                /* Sapphire object not found */
                logger.severe(e.getMessage());
                // TODO (Sungwook, 2018-10-2): Investigate whether exception should be thrown.
            }
            KernelObjectFactory.delete($__getKernelOID());
        }

        public void sapphire_remove_replica(List<SapphirePolicyContainer> processedPolicies)
                throws RemoteException {
            try {
                for (SapphirePolicyContainer policyContainer : processedPolicies) {
                    SapphireServerPolicy sp = policyContainer.getServerPolicy();
                    oms().unRegisterSapphireReplica(sp.getReplicaId());
                }
            } catch (SapphireObjectNotFoundException e) {
                /* Sapphire object not found */
                logger.severe(e.getMessage());
            }
            KernelObjectFactory.delete($__getKernelOID());
        }

        /**
         * Internal function used to initialize the App Object
         *
         * @param spec
         * @param params
         */
        // TODO: not final (stub overrides it)
        public AppObjectStub $__initialize(SapphireObjectSpec spec, Object[] params) {
            logger.info(String.format("Creating app object '%s' with parameters %s", spec, params));

            AppObjectStub actualAppObject = null;

            try {
                String appStubClassName;
                if (spec.getLang() == Language.java) {
                    Class<?> appObjectClass = Class.forName(spec.getJavaClassName());
                    appStubClassName =
                            GlobalStubConstants.getAppPackageName(
                                            RMIUtil.getPackageName(appObjectClass))
                                    + "."
                                    + RMIUtil.getShortName(appObjectClass)
                                    + GlobalStubConstants.STUB_SUFFIX;
                } else {
                    appStubClassName =
                            "sapphire.appexamples.college.stubs." + spec.getName() + "_ClientStub";
                }

                Class<?> appObjectStubClass = Class.forName(appStubClassName);
                // Construct the list of classes of the arguments as Class[]
                if (params != null) {
                    Class<?>[] argClasses = Sapphire.getParamsClasses(params);
                    actualAppObject =
                            (AppObjectStub)
                                    appObjectStubClass
                                            .getConstructor(argClasses)
                                            .newInstance(params);

                } else {
                    actualAppObject = (AppObjectStub) appObjectStubClass.newInstance();
                }

                if (spec.getLang() == Language.java) {
                    actualAppObject.$__initialize(true);
                    appObject = new AppObject(actualAppObject);
                } else {
                    appObject = new AppObject(new GraalObject(spec, params));
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to initialize server policy", e);
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
        protected Map<String, SapphirePolicyConfig> configMap;
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

        public void setAppConfigAnnotation(Map<String, SapphirePolicyConfig> configMap) {
            this.configMap = configMap;
        }

        public Map<String, SapphirePolicyConfig> getAppConfigAnnotation() {
            return configMap;
        }

        public void setSapphireObjId(SapphireObjectID sapphireId) {
            sapphireObjId = sapphireId;
        }

        public SapphireObjectID getSapphireObjId() {
            return sapphireObjId;
        }

        protected void removeReplica(SapphireServerPolicy server)
                throws RemoteException, SapphireObjectReplicaNotFoundException,
                        SapphireObjectNotFoundException {
            server.sapphire_remove_replica();
            removeServer(server);
        }

        protected void updateReplicaHostName(
                SapphireServerPolicy serverPolicy, InetSocketAddress host) throws RemoteException {
            ArrayList<SapphireServerPolicy> servers = getServers();
            if (servers == null) {
                return;
            }

            for (Iterator<SapphireServerPolicy> itr = servers.iterator(); itr.hasNext(); ) {
                SapphireServerPolicy server = itr.next();
                if (server.$__getKernelOID().equals(serverPolicy.$__getKernelOID())) {
                    ((KernelObjectStub) server).$__updateHostname(host);
                    ((KernelObjectStub) serverPolicy).$__updateHostname(host);
                    break;
                }
            }
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

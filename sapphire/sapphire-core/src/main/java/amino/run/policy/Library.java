package amino.run.policy;

import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.app.NodeSelectorSpec;
import amino.run.common.*;
import amino.run.compiler.GlobalStubConstants;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.KernelObjectFactory;
import amino.run.kernel.common.KernelObjectNotFoundException;
import amino.run.kernel.common.KernelObjectStub;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.oms.OMSServer;
import amino.run.policy.Policy.ServerPolicy;
import amino.run.runtime.MicroService;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.harmony.rmi.common.RMIUtil;

public abstract class Library implements Upcalls {
    public abstract static class ClientPolicyLibrary implements ClientUpcalls {

        /*
         * INTERNAL FUNCTIONS (Used by Amino Microservice DMs)
         */
    }

    public abstract static class ServerPolicyLibrary implements ServerUpcalls {
        protected AppObject appObject;
        protected KernelOID oid;
        protected ReplicaID replicaId;
        protected Policy.GroupPolicy group;
        protected MicroServiceSpec spec;
        protected Map<String, SapphirePolicyConfig> configMap;

        static Logger logger = Logger.getLogger(ServerPolicyLibrary.class.getName());

        // ServerPolicy that precedes the current policy in the server side chain - this order is
        // reverse in the client side.
        protected Policy.ServerPolicy previousServerPolicy;

        // List of ServerPolicies that should be created in the chain after the current one when
        // creating replicas.
        // These nested part of chain where the last one created will be called by KernelServer
        // (farthest from actual app object).
        // It means these were the last in order in the client side of chain. New groups should be
        // created for this list of chain.
        protected List<String> nextPolicyNames = new ArrayList<>();

        // List of ServerPolicies that were created previously. They are upper level in group
        // hierarchy. Therefore, this list of chain
        // should not create new group policies. When creating replicas, group stub information
        // stored in this chain will be copied over
        // to the new replicas so that they can reference the same group stubs.
        protected List<PolicyContainer> processedPolicies = new ArrayList<PolicyContainer>();

        // Indicates whether this policy is the inner most policy of the chain.
        protected boolean isLastPolicy = false;

        private OMSServer oms() {
            return GlobalKernelReferences.nodeServer.oms;
        }

        private KernelServerImpl kernel() {
            return GlobalKernelReferences.nodeServer;
        }

        /*
         * SAPPHIRE API FOR SERVER POLICIES
         */
        public List<PolicyContainer> getProcessedPolicies() {
            return this.processedPolicies;
        }

        public ServerPolicy getPreviousServerPolicy() {
            return this.previousServerPolicy;
        }

        public void setPreviousServerPolicy(Policy.ServerPolicy serverPolicy) {
            this.previousServerPolicy = serverPolicy;
        }

        public void setNextPolicyNames(List<String> nextPolicyNames) {
            this.nextPolicyNames = nextPolicyNames;
        }

        /**
         * Is this the last server policy instance in the chain? Last policy is the inner most
         * policy in the chain.
         *
         * @return true if this is the last policy.
         */
        public boolean isLastPolicy() {
            return this.isLastPolicy;
        }

        public void setIsLastPolicy(boolean isLastPolicy) {
            this.isLastPolicy = isLastPolicy;
        }

        public void setProcessedPolicies(List<PolicyContainer> processedPolicies) {
            this.processedPolicies = processedPolicies;
        }

        public void setMicroServiceSpec(MicroServiceSpec spec) {
            this.spec = spec;
        }

        public MicroServiceSpec getMicroServiceSpec() {
            return this.spec;
        }

        @Override
        public void onCreate(Policy.GroupPolicy group, MicroServiceSpec spec) {
            this.group = group;
            this.spec = spec;
            if (spec != null && spec.getDmList() != null) {
                this.configMap = Utils.fromDMSpecListToFlatConfigMap(spec.getDmList());
            }
        }

        /**
         * Creates a replica chain. There are two big steps in this: 1) Creates a replica chain from
         * outermost policy up to this policy. This is upstream policies of this one (and including
         * this one). Since group policies were already created by upstream policies, this part of
         * the chain points to the already created group policy. 2) Creates a replica chain for
         * downsteam policies. Downstream policies create new group policies as those group policies
         * belong to themselves. Details of each step is described in the code.
         *
         * @param region
         * @return A replica (server stub) it just created.
         * @throws RemoteException
         */
        public ServerPolicy replicate(String region) throws RemoteException {
            List<PolicyContainer> processedPoliciesReplica = new ArrayList<>();
            int outerPolicySize = processedPolicies.size();

            try {
                MicroServiceID soid = getReplicaId().getOID();

                // Gets the names of policies that were already created (outer policies).
                List<String> policyNames =
                        MultiDMConstructionHelper.getPolicyNames(processedPolicies);

                // 1. Creates a new replica policy chain from already created policies before this
                // policy (outer policies). Specifically, create instances from outermost up to this
                // policy. Note that the newly created policy instances will point to already
                // created group policies.
                for (int i = 0; i < outerPolicySize; i++) {
                    MicroService.createConnectedPolicy(
                            processedPolicies.get(i).groupPolicy,
                            policyNames,
                            processedPoliciesReplica,
                            soid,
                            spec);
                    policyNames.remove(0);
                }

                // 2. Clones appObject in the outermost policy of the already created chain
                // (original one), and assigns to the outermost policy of this replica chain.
                ServerPolicy originalPolicy = processedPolicies.get(0).serverPolicy;
                ServerPolicy replicaPolicy = processedPoliciesReplica.get(0).serverPolicy;
                MicroService.cloneAppObject(replicaPolicy, originalPolicy.getAppObject());

                // 3. Creates a rest of the replica policy chain from the next of this policy
                // (inner)
                // to the innermost policy. Note that it does not include the current policy. This
                // creates new group policies as well.
                policyNames = new ArrayList<>(this.nextPolicyNames);
                int innerPolicySize = this.nextPolicyNames.size();
                for (int j = outerPolicySize; j < innerPolicySize + outerPolicySize; j++) {
                    MicroService.createConnectedPolicy(
                            null, policyNames, processedPoliciesReplica, soid, spec);
                    policyNames.remove(0);
                }

                // 4. Executes GroupPolicy.onCreate() in the chain starting from the inner most
                // instance up to outer policy of this replica. This is because group policy of this
                // one and outer policies have been already executed. i.e.,) DM1->DM2->DM3->DM4, and
                // if this DM is DM2, it will execute group policy for DM3 & DM4 only.
                for (int k = innerPolicySize + outerPolicySize - 1; k >= outerPolicySize; k--) {
                    Policy.GroupPolicy groupPolicyStub =
                            processedPoliciesReplica.get(k).groupPolicy;
                    ServerPolicy stub =
                            (ServerPolicy) processedPoliciesReplica.get(k).serverPolicyStub;
                    groupPolicyStub.onCreate(region, stub, spec);
                }
            } catch (RemoteException e) {
                terminate(processedPolicies);
                logger.severe(e.getMessage());
                throw new Error("Could not create a replica of " + appObject.getObject(), e);
            } catch (Exception e) {
                logger.severe(e.getMessage());
                throw new Error("Unknown exception occurred!", e);
            }

            return (ServerPolicy)
                    processedPoliciesReplica.get(outerPolicySize - 1).serverPolicyStub;
        }

        public AppObject getAppObject() {
            return appObject;
        }

        /**
         * Pin server policy chain to a given server. 1) Obtain the first server policy (farthest
         * from app object) 2) Navigate through the chain to find all server policies that need to
         * be removed after move. Update the respective stub objects with new host name. Coalesce
         * all the server policy objects in chain. 3) Copy the chain of server policy to the new
         * host. 4) Remove the server policies in the local chain that were moved.
         *
         * @param server
         * @throws RemoteException
         * @throws MicroServiceNotFoundException
         * @throws MicroServiceReplicaNotFoundException
         */
        public void pin_to_server(InetSocketAddress server)
                throws RemoteException, MicroServiceNotFoundException,
                        MicroServiceReplicaNotFoundException {
            ServerPolicy serverPolicy = (ServerPolicy) this;

            // Ensure that we start from the first Server Policy.
            // TODO: Consider using a reference to the innermost policy directly instead of going
            // through the chain.
            while (serverPolicy.getPreviousServerPolicy() != null) {
                serverPolicy = serverPolicy.getPreviousServerPolicy();
            }

            // Before pinning the MicroService Object replica to the provided KernelServer, need to
            // update the Hostname.
            List<PolicyContainer> processedPolicyList = serverPolicy.getProcessedPolicies();
            Iterator<PolicyContainer> itr = processedPolicyList.iterator();
            while (itr.hasNext()) {
                PolicyContainer container = itr.next();
                ServerPolicy tempServerPolicy = container.serverPolicy;
                container.serverPolicyStub.$__updateHostname(server);

                /* AppObject holds the previous DM's server policy stub(instead of So stub) in case of DM chain on the
                server side. Update host name in the server stub within AppObject */
                if (tempServerPolicy.getAppObject().getObject() instanceof KernelObjectStub) {
                    ((KernelObjectStub) tempServerPolicy.getAppObject().getObject())
                            .$__updateHostname(server);
                }
            }

            logger.info(
                    "Started pinning kernel object "
                            + serverPolicy.$__getKernelOID()
                            + " to "
                            + server);
            try {
                kernel().moveKernelObjectToServer(serverPolicy, server);
            } catch (KernelObjectNotFoundException e) {
                String msg = "Could not find myself on this server!";
                logger.severe(msg);
                throw new Error(msg, e);
            } catch (MicroServiceNotFoundException e) {
                String msg = "Could not find MicroService object on this server!";
                logger.severe(msg);
                throw new Error(msg, e);
            } catch (MicroServiceReplicaNotFoundException e) {
                String msg = "Could not find MicroService replica on this server!";
                logger.severe(msg);
                throw new Error(msg, e);
            }

            logger.info(
                    "Finished pinning kernel object "
                            + serverPolicy.$__getKernelOID()
                            + " to "
                            + server);
        }

        public void terminate() throws RemoteException {
            try {
                GlobalKernelReferences.nodeServer.oms.unRegisterReplica(getReplicaId());
            } catch (MicroServiceNotFoundException e) {
                /* MicroService object not found */
                logger.severe(e.getMessage());
                // TODO (Sungwook, 2018-10-2): Investigate whether exception should be thrown.
            }
            KernelObjectFactory.delete($__getKernelOID());
        }

        public void terminate(List<PolicyContainer> processedPolicies) throws RemoteException {
            try {
                for (PolicyContainer policyContainer : processedPolicies) {
                    ServerPolicy sp = policyContainer.serverPolicy;
                    oms().unRegisterReplica(sp.getReplicaId());
                }
            } catch (MicroServiceNotFoundException e) {
                /* MicroService object not found */
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
        public AppObjectStub $__initialize(MicroServiceSpec spec, Object[] params) {
            logger.info(String.format("Creating app object '%s' with parameters %s", spec, params));

            AppObjectStub actualAppObject = null;
            try {
                if (spec.getLang() == Language.java) {
                    Class<?> appObjectClass = Class.forName(spec.getJavaClassName());
                    String appStubClassName =
                            GlobalStubConstants.getAppPackageName(
                                            RMIUtil.getPackageName(appObjectClass))
                                    + "."
                                    + RMIUtil.getShortName(appObjectClass)
                                    + GlobalStubConstants.STUB_SUFFIX;

                    Class<?> appObjectStubClass = Class.forName(appStubClassName);
                    // Construct the list of classes of the arguments as Class[]
                    if (params != null) {
                        Class<?>[] argClasses = MicroService.getParamsClasses(params);
                        actualAppObject =
                                (AppObjectStub)
                                        appObjectStubClass
                                                .getConstructor(argClasses)
                                                .newInstance(params);
                    } else {
                        actualAppObject = (AppObjectStub) appObjectStubClass.newInstance();
                    }

                    actualAppObject.$__initialize(true);
                    appObject = new AppObject(actualAppObject);
                } else {
                    String stubClassName = spec.getJavaClassName();
                    if (stubClassName.isEmpty()) {
                        throw new RuntimeException("stub class name missing for application");
                    }

                    Class<?> appObjectStubClass = Class.forName(spec.getJavaClassName());
                    // Construct the list of classes of the arguments as Class[]
                    // TODO: Currently all polyglot application stub should have default
                    // constructor. Fix it
                    Object appStubObject = appObjectStubClass.newInstance();
                    ((GraalObject) appStubObject).$__initializeGraal(spec, params);
                    actualAppObject = (AppObjectStub) appStubObject;
                    actualAppObject.$__initialize(true);

                    appObject = new AppObject(actualAppObject);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to initialize server policy", e);
            }
            return actualAppObject;
        }

        public String getRegion() {
            return kernel().getRegion();
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

        public void setReplicaId(ReplicaID rid) {
            replicaId = rid;
        }

        public ReplicaID getReplicaId() {
            return replicaId;
        }
    }

    public abstract static class GroupPolicyLibrary implements GroupUpcalls {
        protected String appObjectClassName;
        protected ArrayList<Object> params;
        protected KernelOID oid;
        protected MicroServiceID microServiceId;

        static Logger logger = Logger.getLogger(GroupPolicyLibrary.class.getName());

        protected OMSServer oms() {
            return GlobalKernelReferences.nodeServer.oms;
        }

        /*
         * SAPPHIRE API FOR GROUP POLICIES
         */

        public ArrayList<String> getRegions() throws RemoteException {
            return oms().getRegions();
        }

        /**
         * Gets the list of servers in from nodeSelector or region.
         *
         * @param nodeSelector
         * @param region
         * @return list of server addresses
         * @throws RemoteException
         */
        // TODO: Remove region parameter after spec is applied to all DMs and scripts.
        public List<InetSocketAddress> getAddressList(NodeSelectorSpec nodeSelector, String region)
                throws RemoteException {
            List<InetSocketAddress> serversInRegion;

            if (null != nodeSelector) { // spec takes priority over region
                serversInRegion = oms().getServers(nodeSelector);
            } else {
                if (region != null && !region.isEmpty()) {
                    nodeSelector = new NodeSelectorSpec();
                    nodeSelector.addAndLabel(region);
                    serversInRegion = oms().getServers(nodeSelector);
                } else {
                    serversInRegion = oms().getServers(null);
                }
            }
            return serversInRegion;
        }

        public void $__setKernelOID(KernelOID oid) {
            this.oid = oid;
        }

        public KernelOID $__getKernelOID() {
            return this.oid;
        }

        public void setMicroServiceId(MicroServiceID microServiceId) {
            this.microServiceId = microServiceId;
        }

        public MicroServiceID getSapphireObjId() {
            return microServiceId;
        }

        /**
         * Notifies server policies to exit. Each server policy should do three tasks: 1) remove
         * itself from {@code KernelObjectManager} on local kernel server, 2) remove itself of OMS's
         * {@code KernelObjectManager}, and 3) remove replica ID from OMS.
         *
         * <p><strong>Warning:</strong> Do not try to call OMS to unregister the microservice.
         * {@link OMSServer#delete(MicroServiceID)} is the public entry point to delete a
         * microservice. OMS will take care of deleting microservice at {@link
         * amino.run.oms.OMSServerImpl#delete(MicroServiceID)}.
         *
         * @throws RemoteException
         */
        public void onDestroy() throws RemoteException {
            /* Delete all the servers */
            ArrayList<ServerPolicy> servers = getServers();

            for (Iterator<ServerPolicy> itr = servers.iterator(); itr.hasNext(); ) {
                Policy.ServerPolicy server = itr.next();
                try {
                    server.terminate();
                    itr.remove();
                } catch (Exception e) {

                }
            }

            KernelObjectFactory.delete($__getKernelOID());
        }
    }
}

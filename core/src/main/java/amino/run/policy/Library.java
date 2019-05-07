package amino.run.policy;

import static amino.run.kernel.server.KernelServerImpl.REGION_KEY;

import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.app.NodeSelectorSpec;
import amino.run.app.NodeSelectorTerm;
import amino.run.app.Operator;
import amino.run.app.Requirement;
import amino.run.common.AppObject;
import amino.run.common.AppObjectStub;
import amino.run.common.GraalObject;
import amino.run.common.MicroServiceID;
import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.common.MultiDMConstructionHelper;
import amino.run.common.ReplicaID;
import amino.run.common.Utils;
import amino.run.compiler.GlobalStubConstants;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.KernelObjectFactory;
import amino.run.kernel.common.KernelObjectNotFoundException;
import amino.run.kernel.metric.RPCMetric;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.oms.OMSServer;
import amino.run.policy.Policy.ServerPolicy;
import amino.run.runtime.MicroService;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.harmony.rmi.common.RMIUtil;

public abstract class Library implements Upcalls {
    /**
     * AppContext is a placeholder class to hold app method name and params. Instance of this class
     * is returned from {@link amino.run.policy.Library.ClientPolicyLibrary#extractAppContext}
     */
    public static class AppContext {
        private String appMethod;
        private ArrayList<Object> appParams;

        public AppContext(String appMethod, ArrayList<Object> appParams) {
            this.appMethod = appMethod;
            this.appParams = appParams;
        }

        public String getAppMethod() {
            return appMethod;
        }

        public ArrayList<Object> getAppParams() {
            return appParams;
        }
    }

    public abstract static class ClientPolicyLibrary implements ClientUpcalls {
        /* Depth is set to 0 for the first DM client, 1 for second DM client and so on for the rest
        of the DM clients along the complete chain */
        private int clientDepth = 0;

        /*
         * API FOR CLIENT POLICIES
         */

        /**
         * Extract App method name and the parameters from the received param stack
         *
         * @param method
         * @param params
         * @return AppContext
         */
        public AppContext extractAppContext(String method, ArrayList<Object> params) {
            String appMethod = method;
            ArrayList<Object> appParams = params;

            /* Received params stack will be in nested form as shown below :
             * [methodName, ArrayList<object> = [methodName, ArrayList<Object> = [methodName, ArrayList<Object> ... ]]]
             * Inner most method name and params are that of app ones. Parse and fetch them */
            int depth = clientDepth;
            while (depth != 0) {
                assert (params.size() == 2);
                appMethod = (String) params.get(0);
                appParams = (ArrayList<Object>) params.get(1);
                params = appParams;
                depth--;
            }

            return new AppContext(appMethod, appParams);
        }

        /*
         * INTERNAL FUNCTIONS (Used by runtime system)
         */
        public void setClientDepth(int clientDepth) {
            this.clientDepth = clientDepth;
        }
    }

    public abstract static class ServerPolicyLibrary implements ServerUpcalls {
        protected AppObject appObject;
        protected KernelOID oid;
        protected ReplicaID replicaId;
        private MicroServiceSpec spec;
        // Whether to skip pinning this microservice (usually primary replica).
        // Group policy that sets this property should pin this microservice itself.
        protected boolean skipPinning;

        private static final Logger logger = Logger.getLogger(ServerPolicyLibrary.class.getName());

        // List of ServerPolicies that should be created in the chain after the current one when
        // creating replicas.
        // These nested part of chain where the last one created will be called by KernelServer
        // (farthest from actual app object).
        // It means these were the last in order in the client side of chain. New groups should be
        // created for this list of chain.
        protected List<String> nextPolicyNames = new ArrayList<String>();

        // List of ServerPolicies that were created previously. They are upper level in group
        // hierarchy. Therefore, this list of chain
        // should not create new group policies. When creating replicas, group stub information
        // stored in this chain will be copied over
        // to the new replicas so that they can reference the same group stubs.
        protected List<PolicyContainer> processedPolicies = new ArrayList<PolicyContainer>();

        // Indicates whether this policy is the inner most policy of the chain.
        protected boolean isLastPolicy = false;

        /* Group policy of immediate next DM beneath this server policy */
        private KernelOID childGroupId;

        /* Group policy which has created this server policy */
        private KernelOID parentGroupId;

        private OMSServer oms() {
            return GlobalKernelReferences.nodeServer.oms;
        }

        private KernelServerImpl kernel() {
            return GlobalKernelReferences.nodeServer;
        }

        /*
         * API FOR SERVER POLICIES
         */
        public List<PolicyContainer> getProcessedPolicies() {
            return this.processedPolicies;
        }

        public void setNextPolicyNames(List<String> nextPolicyNames) {
            this.nextPolicyNames = nextPolicyNames;
        }

        /*
         * Set to skip pinning this from default policy. DM that sets this should pin this policy itself.
         * Currently, used by LoadBalancedFrontendPolicy.
         */
        public void setToSkipPinning() {
            this.skipPinning = true;
        }

        /*
         * Return whether pinning this microservice by default policy should be skipped since the group
         * policy of the DM itself will pin this microservice.
         */
        public boolean shouldSkipPinning() {
            return this.skipPinning;
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

        public void setSpec(MicroServiceSpec spec) {
            this.spec = spec;
        }

        public MicroServiceSpec getSpec() {
            return spec;
        }

        public KernelOID getParentGroupId() {
            return parentGroupId;
        }

        public void setParentGroupId(KernelOID parentGroupOid) {
            parentGroupId = parentGroupOid;
        }

        public KernelOID getChildGroupId() {
            return childGroupId;
        }

        public void setChildGroupId(KernelOID childGroupOid) {
            childGroupId = childGroupOid;
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
            List<PolicyContainer> processedPoliciesReplica = new ArrayList<PolicyContainer>();
            int outerPolicySize = processedPolicies.size();
            ServerPolicy serverStub;
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
                            getGroup(),
                            policyNames,
                            processedPoliciesReplica,
                            soid,
                            getSpec());
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
                policyNames = new ArrayList<String>(this.nextPolicyNames);
                int innerPolicySize = this.nextPolicyNames.size();
                for (int j = outerPolicySize; j < innerPolicySize + outerPolicySize; j++) {
                    MicroService.createConnectedPolicy(
                            null, null, policyNames, processedPoliciesReplica, soid, getSpec());
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
                    groupPolicyStub.onCreate(region, stub);
                }

                /* Clone the server policy to be returned */
                serverStub =
                        (ServerPolicy)
                                processedPoliciesReplica.get(outerPolicySize - 1).serverPolicyStub;
                serverStub = (ServerPolicy) Utils.ObjectCloner.deepCopy(serverStub);

                /* Remove the next DM client link for all server policy stubs on server side */
                for (PolicyContainer container : processedPoliciesReplica) {
                    container.serverPolicyStub.$__setNextClientPolicy(null);
                }
            } catch (RemoteException e) {
                logger.severe(e.getMessage());
                if (!processedPoliciesReplica.isEmpty()) {
                    terminate(processedPoliciesReplica.get(0).serverPolicy);
                }
                throw new Error("Could not create a replica of " + appObject.getObject(), e);
            } catch (Exception e) {
                logger.severe(e.getMessage());
                throw new Error("Unknown exception occurred!", e);
            }

            return serverStub;
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

            /* Update the host name in the stub objects */
            for (PolicyContainer container : serverPolicy.getProcessedPolicies()) {
                container.serverPolicyStub.$__updateHostname(server);
            }

            logger.info(
                    "Finished pinning kernel object "
                            + serverPolicy.$__getKernelOID()
                            + " to "
                            + server);
        }

        /**
         * Terminates this server policy
         *
         * @throws RemoteException
         */
        public void terminate() throws RemoteException {
            terminate((ServerPolicy) this);
        }

        /**
         * Terminates the server policy
         *
         * @param server
         * @throws RemoteException
         */
        public void terminate(ServerPolicy server) throws RemoteException {
            try {
                if (server.getChildGroupId() != null) {
                    oms().deleteGroupPolicy(
                                    server.getReplicaId().getOID(), server.getChildGroupId());
                }

                for (PolicyContainer policyContainer : server.processedPolicies) {
                    ServerPolicy temp = policyContainer.serverPolicy;

                    if (!temp.getParentGroupId().equals(server.getGroup().$__getKernelOID())) {
                        continue;
                    }

                    KernelObjectFactory.delete(temp.$__getKernelOID());
                    oms().unRegisterReplica(temp.getReplicaId());
                }
            } catch (MicroServiceNotFoundException e) {
                /* MicroService object not found */
                logger.severe(e.getMessage());
                // TODO (Sungwook, 2018-10-2): Investigate whether exception should be thrown.
            }
        }

        /**
         * Get and returns the collected RPC metrics from the kernel object. And clears the metrics
         * collected so far.
         *
         * @return Map of RPC metrics for all clients collected after last notification until now
         * @throws KernelObjectNotFoundException
         */
        public Map<UUID, RPCMetric> getRPCMetrics() throws KernelObjectNotFoundException {
            ConcurrentHashMap<UUID, RPCMetric> metrics =
                    kernel().getKernelObject($__getKernelOID()).getMetrics();
            ConcurrentHashMap<UUID, RPCMetric> cloneCopy;
            /* TODO: Better to avoid the clone copy. Need to check if we can return cumulative metrics instead of
            metrics from last expiry. */
            synchronized (metrics) {
                cloneCopy = new ConcurrentHashMap<UUID, RPCMetric>(metrics);
                metrics.clear();
            }
            return cloneCopy;
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

        /**
         * Gets the DM policy configuration
         *
         * @param configName
         * @return Policy configuration
         */
        public PolicyConfig getPolicyConfig(String configName) {
            Map<String, PolicyConfig> configMap =
                    Utils.fromDMSpecListToFlatConfigMap(getSpec().getDmList());
            return configMap.get(configName);
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
        private static final Logger logger = Logger.getLogger(GroupPolicyLibrary.class.getName());
        protected String appObjectClassName;
        protected ArrayList<Object> params;
        protected KernelOID oid;
        protected MicroServiceID microServiceId;
        private MicroServiceSpec spec;

        protected OMSServer oms() {
            return GlobalKernelReferences.nodeServer.oms;
        }

        /*
         * API FOR GROUP POLICIES
         */

        public ArrayList<String> getRegions() throws RemoteException {
            return oms().getRegions();
        }

        /**
         * Gets the DM policy configuration
         *
         * @param configName
         * @return Policy configuration
         */
        public PolicyConfig getPolicyConfig(String configName) {
            Map<String, PolicyConfig> configMap =
                    Utils.fromDMSpecListToFlatConfigMap(getSpec().getDmList());
            return configMap.get(configName);
        }

        /**
         * Gets the list of servers in from nodeSelector or region.
         *
         * @param region
         * @return list of server addresses
         * @throws RemoteException
         */
        // TODO: Remove region parameter after spec is applied to all DMs and scripts.
        public List<InetSocketAddress> getAddressList(String region) throws RemoteException {
            List<InetSocketAddress> serversInRegion;
            NodeSelectorSpec nodeSelector = getSpec().getNodeSelectorSpec();
            if (null != nodeSelector) { // spec takes priority over region
                serversInRegion = oms().getServers(nodeSelector);
            } else {
                if (region != null && !region.isEmpty()) {
                    NodeSelectorTerm regionTerm = new NodeSelectorTerm();
                    regionTerm.addMatchRequirements(
                            new Requirement(
                                    REGION_KEY, Operator.Equal, Collections.singletonList(region)));
                    nodeSelector = new NodeSelectorSpec();
                    nodeSelector.addNodeSelectorTerms(regionTerm);
                    serversInRegion = oms().getServers(nodeSelector);
                } else {
                    serversInRegion = oms().getServers(null);
                }
            }
            return serversInRegion;
        }

        /**
         * Updates the microservice metrics to OMS
         *
         * @param replicaId
         * @param metrics
         * @throws RemoteException
         */
        public void updateMetric(ReplicaID replicaId, Map<UUID, RPCMetric> metrics)
                throws RemoteException {
            try {
                oms().updateMetric(replicaId, metrics);
            } catch (MicroServiceNotFoundException e) {
                logger.warning(
                        String.format(
                                "Microservice %s not found. Exception: %s", replicaId.getOID(), e));
            } catch (MicroServiceReplicaNotFoundException e) {
                logger.warning(
                        String.format(
                                "Microservice replica %s not found. Exception: %s", replicaId, e));
            }
        }

        public void $__setKernelOID(KernelOID oid) {
            this.oid = oid;
        }

        public KernelOID $__getKernelOID() {
            return this.oid;
        }

        public void setMicroServiceId(MicroServiceID id) {
            microServiceId = id;
        }

        public MicroServiceID getMicroServiceId() {
            return microServiceId;
        }

        public void setSpec(MicroServiceSpec spec) {
            this.spec = spec;
        }

        public MicroServiceSpec getSpec() {
            return spec;
        }
    }
}

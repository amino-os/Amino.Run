package amino.run.runtime;

import static amino.run.policy.Policy.ClientPolicy;
import static amino.run.policy.Policy.GroupPolicy;
import static amino.run.policy.Policy.ServerPolicy;

import amino.run.app.DMSpec;
import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.common.AppObject;
import amino.run.common.AppObjectStub;
import amino.run.common.MicroServiceCreationException;
import amino.run.common.MicroServiceID;
import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.common.MultiDMConstructionHelper;
import amino.run.common.Notification;
import amino.run.common.ReplicaID;
import amino.run.common.Utils;
import amino.run.compiler.GlobalStubConstants;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.KernelObjectFactory;
import amino.run.kernel.common.KernelObjectNotCreatedException;
import amino.run.kernel.common.KernelObjectNotFoundException;
import amino.run.kernel.common.KernelObjectStub;
import amino.run.policy.DefaultPolicy;
import amino.run.policy.Policy;
import amino.run.policy.PolicyContainer;
import amino.run.runtime.util.PolicyCreationHelper;
import java.io.IOException;
import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.harmony.rmi.common.RMIUtil;

/**
 * Used by the developer to create a MicroService given the Application Object class and the Policy
 * Object class.
 *
 * @author aaasz
 */
// TODO: There are many methods that can be moved to helper or util classes. Move them into
// appropriate classes.
public class MicroService {
    private static final Logger logger = Logger.getLogger(MicroService.class.getName());

    /**
     * Creates a microservice.
     *
     * @param spec MicroService specification
     * @param args parameters to microservice constructor
     * @return microservice stub
     */
    public static Object new_(MicroServiceSpec spec, Object... args)
            throws MicroServiceCreationException {
        AppObjectStub appStub = null;
        try {
            logger.info("Creating microservice for spec:" + spec);
            if (spec.getLang() == Language.java && spec.getDmList().isEmpty()) {
                Class<?> appObjectClass = Class.forName(spec.getJavaClassName());
                return new_(appObjectClass, args);
            }

            /* Get the region of current server */
            String region = GlobalKernelReferences.nodeServer.getRegion();
            return createPolicyChain(spec, region, args);
        } catch (Exception e) {
            String msg = String.format("Failed to create microservice '%s'", spec);
            logger.log(Level.SEVERE, msg, e);
            throw new MicroServiceCreationException(msg, e);
        }
    }

    /**
     * WARN: This method only works for Java microservice. This method has been deprecated. Please
     * use {@link #new_(MicroServiceSpec, Object...)}. We keep this method because we have Java demo
     * apps that call this method directly.
     *
     * @param appObjectClass the class of the app object
     * @param args the arguments to app object constructor
     * @return the App Object stub
     * @deprecated Please use {@link #new_(MicroServiceSpec, Object...)}
     */
    public static Object new_(Class<?> appObjectClass, Object... args) {
        try {
            MicroServiceSpec spec =
                    MicroServiceSpec.newBuilder()
                            .setLang(Language.java)
                            .setJavaClassName(appObjectClass.getName())
                            .create();

            /* Get the region of current server */
            String region = GlobalKernelReferences.nodeServer.getRegion();

            AppObjectStub appStub = createPolicyChain(spec, region, args);
            logger.info("MicroService Object created: " + appObjectClass.getName());
            return appStub;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create microservice:", e);
            return null;
        }
    }

    /**
     * Creates a complete policy chain using the spec. 1. Creates an app object stub. 2. Creates
     * client, stub, group and server policy instance for each DM in the chain. 3. Links them
     * (stub->nextClient, server -> outerServer). 4. Executes onCreate for group policy from
     * innermost to outermost. 5. Pins the original Microservice. 6. Returns appStub which points to
     * the first client policy in the chain.
     *
     * @param spec MicroService spec
     * @param region Region
     * @param appArgs Arguments for application object
     * @return client side appObjectStub
     * @throws IOException
     * @throws CloneNotSupportedException
     * @throws MicroServiceCreationException
     */
    public static AppObjectStub createPolicyChain(
            MicroServiceSpec spec, String region, Object[] appArgs)
            throws IOException, CloneNotSupportedException, MicroServiceCreationException {
        List<PolicyContainer> processedPolicies = new ArrayList<PolicyContainer>();

        if (spec.getDmList().isEmpty()) {
            // Adds default DM when there is no DMs specified in the spec.
            spec = setDefaultDMSpec(spec);
        }

        /* Register for a microservice Id from OMS */
        MicroServiceID microServiceID =
                GlobalKernelReferences.nodeServer.oms.registerMicroService();

        /* Create a policy chain based on policy names in the spec */
        List<String> policyNames = MultiDMConstructionHelper.getPolicyNameChain(spec);

        for (int i = 0; i < spec.getDmList().size(); i++) {
            createConnectedPolicy(null, null, policyNames, processedPolicies, microServiceID, spec);
            policyNames.remove(0);
        }

        ServerPolicy serverPolicy = processedPolicies.get(0).serverPolicy;
        ClientPolicy clientPolicy = processedPolicies.get(0).clientPolicy;

        // Creates an appObject and allocate to the outermost policy.
        AppObjectStub appStub = initAppStub(spec, serverPolicy, appArgs);
        // TODO(multi-lang): We may need to create a clone for non-java app object stub.
        appStub = spec.getLang() == Language.java ? createClientAppStub(appStub) : appStub;
        appStub.$__initialize(microServiceID, clientPolicy);

        /* Execute GroupPolicy.onCreate() in the chain starting from inner most instance */
        // TODO: Can node selection constraints be passed to the next (inner) group policy in an
        // iterative way?
        for (int j = processedPolicies.size() - 1; j >= 0; j--) {
            GroupPolicy groupPolicyStub = processedPolicies.get(j).groupPolicy;
            ServerPolicy serverPolicyStub =
                    (ServerPolicy) processedPolicies.get(j).serverPolicyStub;
            groupPolicyStub.onCreate(region, serverPolicyStub);
        }

        /* Remove the next DM client link for all server policy stubs on server side */
        for (PolicyContainer container : processedPolicies) {
            container.serverPolicyStub.$__setNextClientPolicy(null);
        }

        return appStub;
    }

    /**
     * Creates policy instances for client, server, stub, group policy and appObject, and link them
     * to outer policies as needed. This is called by Kernel. Sets the next client policy for stub;
     * sets the inner server policy to outer the server policy. New appObject is created for the
     * outermost server policy; for other server policies, appObject is set as a stub to the outer
     * server policy. Returns the list of processed policies which include the newly created policy
     * instances.
     *
     * @param groupPolicy group policy is null when kernel creates AminoMicroservice; existing group
     *     policy is passed for outer policies when crearting a replica from Library.
     * @param parentGroupPolicy Group policy creating the policy object
     * @param policyNames a list of policy names that are not created yet. Only the first one in the
     *     list is created in this method. The rest is set as next policy names in the server
     *     policy.
     * @param processedPolicies Policies processed so far (created and linked)
     * @param microServiceID Object ID registred to OMS which is one per AminoMicroservice chain
     *     (all replicas of the chain will have the same object ID.
     * @param spec Microservice spec
     * @return list of processed policies so far
     * @throws MicroServiceCreationException thrown when policy object creation fails
     */
    // TODO: which is better choice? split for each of Kernel initiated and Library initiated or
    // combine them?
    public static List<PolicyContainer> createConnectedPolicy(
            GroupPolicy groupPolicy,
            GroupPolicy parentGroupPolicy,
            List<String> policyNames,
            List<PolicyContainer> processedPolicies,
            MicroServiceID microServiceID,
            MicroServiceSpec spec)
            throws MicroServiceCreationException {
        if (groupPolicy == null) {
            groupPolicy =
                    PolicyCreationHelper.createGroupPolicy(
                            policyNames.get(0), microServiceID, spec);
        }

        processedPolicies =
                createPolicyInstance(
                        microServiceID,
                        groupPolicy,
                        parentGroupPolicy,
                        policyNames,
                        processedPolicies,
                        spec);

        // index of currently processing policy. i.e.,) if there were 2 outer DMs created already,
        // idx will be 3.
        int idx = processedPolicies.size() - 1;

        if (idx > 0) {
            /* Check and get the previous DM's container if available. So that, server side and client side chain links can
            be updated between the current DM and previous DM. If the previous DM's container is not available (idx > 0),
            then DM/Policy being created is either for single DM based SO or It is the first DM/Policy in Multi DM based SO
            (i.e., last mile server policy to SO */
            PolicyContainer currentSPC = processedPolicies.get(idx);
            ServerPolicy serverPolicy = currentSPC.serverPolicy;
            // Previous server policy stub object acts as MicroService Object(SO) to the current
            // server policy. Outermost policy is linked to an actual app object;hence, it is
            // not needed here.
            PolicyContainer outerSPC = processedPolicies.get(idx - 1);
            KernelObjectStub outerStub = outerSPC.serverPolicyStub;
            ServerPolicy outerSP = outerSPC.serverPolicy;

            if (parentGroupPolicy == null) {
                outerSP.setChildGroupId(serverPolicy.getGroup().$__getKernelOID());
            }

            // Links this serverPolicy to stub for outer policy.
            serverPolicy.$__initialize(new AppObject(outerStub));
            outerStub.$__setNextClientPolicy(currentSPC.clientPolicy);
        }

        return processedPolicies;
    }

    /**
     * Creates policy object for client, server and stub: 1) instantiates them based on the given
     * policyName, 2) sets the serverStub in the client policy, 3) sets the groupPolicyStub from the
     * input on the server policy, 4) registers this replica, 5) and sets the next policy names and
     * processed policies. Returns the processed policies which is a list of policies created so
     * far.
     *
     * @param microServiceID MicroService ID
     * @param groupPolicyStub Group Policy stub
     * @param parentGroupPolicyStub Group policy creating the policy object
     * @param policyNamesToCreate name of polices that need to be created (this and inner policies)
     * @param processedPolicies Policies processed so far (created and linked)
     * @param spec Microservice spec
     * @return list of policies that had been created so far including the one just created here
     * @throws MicroServiceCreationException thrown when policy object creation fails
     */
    public static List<PolicyContainer> createPolicyInstance(
            MicroServiceID microServiceID,
            GroupPolicy groupPolicyStub,
            GroupPolicy parentGroupPolicyStub,
            List<String> policyNamesToCreate,
            List<PolicyContainer> processedPolicies,
            MicroServiceSpec spec)
            throws MicroServiceCreationException {
        String policyName = policyNamesToCreate.get(0);
        try {
            HashMap<String, Class<?>> policyMap = PolicyCreationHelper.getPolicyMap(policyName);

            /* Get the policy used by the MicroService Object we need to create */
            Class<?> serverPolicyClass = policyMap.get(PolicyCreationHelper.ServerPolicyClass);
            Class<?> clientPolicyClass = policyMap.get(PolicyCreationHelper.ClientPolicyClass);

            /* Create the Kernel Object for the Server Policy, and get the Server Policy Stub */
            ServerPolicy serverPolicyStub = (ServerPolicy) getPolicyStub(serverPolicyClass);

            /* Create the Client Policy Object */
            ClientPolicy client = (ClientPolicy) clientPolicyClass.newInstance();

            /* Initialize the server policy and return a local pointer to the object itself */
            Policy.ServerPolicy serverPolicy = initializeServerPolicy(serverPolicyStub);

            registerReplica(microServiceID, serverPolicy, serverPolicyStub);

            /* Link everything together */
            // TODO: client is unncessary for outer policies of a replica.
            client.onCreate(groupPolicyStub);
            client.setClientDepth(processedPolicies.size());

            // Note that subList is non serializable; hence, the new list creation.
            List<String> nextPolicyNames = new ArrayList<String>(policyNamesToCreate);

            // Remove itself from the next policy names.
            nextPolicyNames.remove(0);
            serverPolicy.setNextPolicyNames(nextPolicyNames);
            serverPolicyStub.setIsLastPolicy(nextPolicyNames.size() == 0);

            /* Updates the list of processed policies. */
            PolicyContainer processedPolicy = new PolicyContainer(policyName);
            processedPolicy.groupPolicy = groupPolicyStub;
            processedPolicy.serverPolicy = serverPolicy;
            processedPolicy.serverPolicyStub = ((KernelObjectStub) serverPolicyStub);
            processedPolicy.clientPolicy = client;
            processedPolicies.add(processedPolicy);

            /* Create a copy to set processed policies up to this point. */
            List<PolicyContainer> processedPoliciesSoFar = new ArrayList(processedPolicies);
            serverPolicy.setProcessedPolicies(processedPoliciesSoFar);
            serverPolicyStub.setProcessedPolicies(processedPoliciesSoFar);

            KernelOID parentGroupOid = groupPolicyStub.$__getKernelOID();
            if (parentGroupPolicyStub != null) {
                parentGroupOid = parentGroupPolicyStub.$__getKernelOID();
            }

            serverPolicy.setParentGroupId(parentGroupOid);
            serverPolicy.setSpec(spec);

            /* Execute onCreate for ServerPolicy */
            serverPolicy.onCreate(groupPolicyStub);
        } catch (KernelObjectNotCreatedException e) {
            logger.severe("Failed while creating stub for " + policyName);
            throw new MicroServiceCreationException(e);
        } catch (ClassNotFoundException e) {
            logger.severe("Failed while creating stub for " + policyName);
            throw new MicroServiceCreationException(e);
        } catch (KernelObjectNotFoundException e) {
            logger.severe("Failed while creating server policy for " + policyName);
            throw new MicroServiceCreationException(e);
        } catch (MicroServiceNotFoundException e) {
            logger.severe("Failed while registering a replica for " + policyName);
            throw new MicroServiceCreationException(e);
        } catch (MicroServiceReplicaNotFoundException e) {
            logger.severe("Failed while registering a replica for " + policyName);
            throw new MicroServiceCreationException(e);
        } catch (RemoteException e) {
            logger.severe("Failed while registering a replica for " + policyName);
            throw new MicroServiceCreationException(e);
        } catch (IllegalAccessException e) {
            logger.severe("Failed while instantiating client class for " + policyName);
            throw new MicroServiceCreationException(e);
        } catch (InstantiationException e) {
            logger.severe("Failed while instantiating client class for " + policyName);
            throw new MicroServiceCreationException(e);
        }

        return processedPolicies;
    }

    /**
     * Deletes the given microservice
     *
     * @param stub
     */
    public static void delete_(Object stub) {
        if (!(stub instanceof AppObjectStub)) {
            throw new RuntimeException("Tried to delete invalid microservice");
        }

        MicroServiceID microServiceId = ((AppObjectStub) stub).$__getMicroServiceId();
        try {
            GlobalKernelReferences.nodeServer.oms.delete(microServiceId);
        } catch (MicroServiceNotFoundException e) {
            /* Ignore it. It might have happened that microservice is already deleted and still hold reference */
            logger.warning(String.format("%s is not found. Probably deleted.", microServiceId));
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete microservice.", e);
        }
    }

    /**
     * Creates the group policy instance returns group policy object Stub
     *
     * @param policyClass
     * @param microServiceId
     * @return Returns group policy object stub
     * @throws RemoteException
     * @throws ClassNotFoundException
     * @throws KernelObjectNotCreatedException
     * @throws MicroServiceNotFoundException
     */
    // TODO: Duplicate name as in the one in PolicyCreationHelper but this is called by OMS.
    public static Policy.GroupPolicy createGroupPolicy(
            Class<?> policyClass, MicroServiceID microServiceId, MicroServiceSpec spec)
            throws ClassNotFoundException, KernelObjectNotCreatedException {
        Policy.GroupPolicy groupPolicyStub = (GroupPolicy) getPolicyStub(policyClass);
        try {
            GroupPolicy groupPolicy = initializeGroupPolicy(groupPolicyStub);
            groupPolicyStub.setMicroServiceId(microServiceId);
            groupPolicy.setMicroServiceId(microServiceId);
            groupPolicy.setSpec(spec);
        } catch (KernelObjectNotFoundException e) {
            logger.severe(
                    "Failed to find the group kernel object created just before it. Exception info: "
                            + e);
            throw new KernelObjectNotCreatedException("Failed to find the kernel object", e);
        }

        return groupPolicyStub;
    }

    /**
     * Deletes the group policy instance
     *
     * @param kernelOid
     */
    public static void deleteGroupPolicy(KernelOID kernelOid) {
        KernelObjectFactory.delete(kernelOid);
    }

    /**
     * Notify group policy about the event
     *
     * @param kernelOid
     * @param notification
     * @throws KernelObjectNotFoundException
     * @throws RemoteException
     */
    public static void notifyGroupPolicy(KernelOID kernelOid, Notification notification)
            throws KernelObjectNotFoundException, RemoteException {
        GlobalKernelReferences.nodeServer.notifyKernelObject(kernelOid, notification);
    }

    /* Returns a pointer to the given MicroService Object */
    // TODO: how to implement this ?
    public static Object this_(amino.run.app.MicroService so) {

        AppObjectStub appObject = (AppObjectStub) so;
        return null;
    }

    // TODO: Move the following methods to PolicyCreateionHelper class
    public static KernelObjectStub getPolicyStub(Class<?> policyClass)
            throws ClassNotFoundException, KernelObjectNotCreatedException {
        String policyStubClassName =
                GlobalStubConstants.getPolicyPackageName()
                        + "."
                        + RMIUtil.getShortName(policyClass)
                        + GlobalStubConstants.STUB_SUFFIX;
        KernelObjectStub policyStub = KernelObjectFactory.create(policyStubClassName);
        return policyStub;
    }

    public static Policy.GroupPolicy initializeGroupPolicy(GroupPolicy groupPolicyStub)
            throws KernelObjectNotFoundException {
        KernelOID groupOID = ((KernelObjectStub) groupPolicyStub).$__getKernelOID();
        GroupPolicy groupPolicy =
                (Policy.GroupPolicy) GlobalKernelReferences.nodeServer.getObject(groupOID);
        groupPolicy.$__setKernelOID(groupOID);
        return groupPolicy;
    }

    private static ServerPolicy initializeServerPolicy(Policy.ServerPolicy serverPolicyStub)
            throws KernelObjectNotFoundException {
        KernelOID serverOID = ((KernelObjectStub) serverPolicyStub).$__getKernelOID();
        ServerPolicy serverPolicy =
                (ServerPolicy) GlobalKernelReferences.nodeServer.getObject(serverOID);
        serverPolicy.$__setKernelOID(serverOID);
        return serverPolicy;
    }

    public static AppObjectStub createClientAppStub(AppObjectStub template)
            throws CloneNotSupportedException {
        AppObjectStub obj = (AppObjectStub) template.$__clone();

        // clean up inherited fields from application object to be GC'd
        Field[] fields = obj.getClass().getSuperclass().getFields();
        for (Field f : fields) {
            if (!f.getType().isPrimitive()) {
                f.setAccessible(true);
                try {
                    f.set(obj, null);
                } catch (IllegalAccessException e) {
                    // We set accessible so should not happen, but also
                    // only for performance, so not critical to succeed
                }
            }
        }

        // Route requests through DM
        obj.$__initialize(false);
        return obj;
    }

    private static Class<?> getParamClassStripStub(Object param) throws ClassNotFoundException {
        String paramClassName = param.getClass().getName();
        int index = paramClassName.lastIndexOf("_");

        if (index == -1) return Class.forName(paramClassName);

        if (paramClassName.substring(index).equals(GlobalStubConstants.STUB_SUFFIX))
            /* TODO: Is it correct all times ? */
            paramClassName = param.getClass().getSuperclass().getName();

        return Class.forName(paramClassName);
    }

    /**
     * Processes MicroService replica by registering for a replica ID and handler for the replica to
     * OMS.
     *
     * @param microServiceId MicroService ID
     * @param serverPolicy ServerPolicy
     * @param serverPolicyStub ServerPolicy stub
     * @throws MicroServiceNotFoundException
     * @throws MicroServiceReplicaNotFoundException
     * @throws RemoteException
     */
    private static void registerReplica(
            MicroServiceID microServiceId,
            ServerPolicy serverPolicy,
            final ServerPolicy serverPolicyStub)
            throws MicroServiceNotFoundException, MicroServiceReplicaNotFoundException,
                    RemoteException {
        /* Register for a replica ID from OMS */
        ReplicaID replicaId = GlobalKernelReferences.nodeServer.oms.registerReplica(microServiceId);

        serverPolicyStub.setReplicaId(replicaId);
        serverPolicy.setReplicaId(replicaId);

        EventHandler replicaHandler =
                new EventHandler(
                        GlobalKernelReferences.nodeServer.getLocalHost(),
                        new ArrayList() {
                            {
                                add(serverPolicyStub);
                            }
                        });

        /* Register the handler for this replica to OMS */
        GlobalKernelReferences.nodeServer.oms.setReplicaDispatcher(replicaId, replicaHandler);
    }

    /**
     * Initializes server policy and stub with app object.
     *
     * @param spec microservice spec
     * @param serverPolicy server policy
     * @param appArgs app arguments
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static AppObjectStub initAppStub(
            MicroServiceSpec spec, ServerPolicy serverPolicy, Object[] appArgs) {
        return serverPolicy.$__initialize(spec, appArgs);
    }

    /**
     * Clones the given appObject and returns the clone.
     *
     * @param serverPolicy server policy
     * @param appObject app Object
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static AppObject cloneAppObject(ServerPolicy serverPolicy, AppObject appObject)
            throws ClassNotFoundException, IOException {
        appObject = (AppObject) Utils.ObjectCloner.deepCopy(appObject);
        serverPolicy.$__initialize(appObject);
        return appObject;
    }

    public static Class<?>[] getParamsClasses(Object[] params) throws ClassNotFoundException {
        ArrayList<Class<?>> argClassesList = new ArrayList<Class<?>>();
        for (Object param : params) {
            argClassesList.add(getParamClassStripStub(param));
        }
        Class<?>[] argClasses = new Class<?>[argClassesList.size()];
        return argClassesList.toArray(argClasses);
    }

    /**
     * Sets the DMSpec with a default DM and returns it.
     *
     * @param spec
     * @return
     */
    private static MicroServiceSpec setDefaultDMSpec(MicroServiceSpec spec) {
        List<DMSpec> list = new ArrayList<DMSpec>();
        DMSpec dmSpec = new DMSpec();
        dmSpec.setName(DefaultPolicy.class.getName());
        list.add(dmSpec);
        spec.setDmList(list);
        return spec;
    }
}

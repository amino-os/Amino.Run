package amino.run.runtime;

import static amino.run.policy.Policy.ClientPolicy;
import static amino.run.policy.Policy.GroupPolicy;
import static amino.run.policy.Policy.ServerPolicy;

import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.app.SapphireObject;
import amino.run.common.AppObject;
import amino.run.common.AppObjectStub;
import amino.run.common.MicroServiceCreationException;
import amino.run.common.MicroServiceID;
import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.common.ReplicaID;
import amino.run.common.Utils;
import amino.run.common.*;
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
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.harmony.rmi.common.RMIUtil;

/**
 * Used by the developer to create a Sapphire Object given the Application Object class and the
 * Policy Object class.
 *
 * @author aaasz
 */
// TODO: too many helper/util classes in this class. Split them into appropriate classes.
public class Sapphire {
    static Logger logger = Logger.getLogger(Sapphire.class.getName());

    /**
     * Creates a sapphire object.
     *
     * @param spec Sapphire object specification
     * @param args parameters to sapphire object constructor
     * @return sapphire object stub
     */
    public static Object new_(MicroServiceSpec spec, Object... args)
            throws MicroServiceCreationException {
        AppObjectStub appStub = null;
        try {
            logger.info("Creating object for spec:" + spec);
            if (spec.getLang() == Language.java && spec.getDmList().isEmpty()) {
                Class<?> appObjectClass = Class.forName(spec.getJavaClassName());
                return new_(appObjectClass, args);
            }

            List<String> policyNameChain = MultiDMConstructionHelper.getPolicyNameChain(spec);

            if (policyNameChain.size() == 0) {
                policyNameChain.add(DefaultPolicy.class.getName());
            }

            /* Get the region of current server */
            String region = GlobalKernelReferences.nodeServer.getRegion();
            return createPolicyChain(spec, policyNameChain, region, args);
        } catch (Exception e) {
            String msg = String.format("Failed to create sapphire object '%s'", spec);
            logger.log(Level.SEVERE, msg, e);
            throw new MicroServiceCreationException(msg, e);
        }
    }

    /**
     * WARN: This method only works for Java sapphire object. This method has been deprecated.
     * Please use {@link #new_(MicroServiceSpec, Object...)}. We keep this method because we have
     * Java demo apps that call this method directly.
     *
     * @param appObjectClass the class of the app object
     * @param args the arguments to app object constructor
     * @return the App Object stub
     * @deprecated Please use {@link #new_(MicroServiceSpec, Object...)}
     */
    public static Object new_(Class<?> appObjectClass, Object... args) {
        try {
            Annotation[] annotations = appObjectClass.getAnnotations();
            List<String> policyNameChain =
                    MultiDMConstructionHelper.getPolicyNameChain(annotations);

            if (policyNameChain.size() == 0) {
                policyNameChain.add(DefaultPolicy.class.getName());
            }

            MicroServiceSpec spec =
                    MicroServiceSpec.newBuilder()
                            .setLang(Language.java)
                            .setJavaClassName(appObjectClass.getName())
                            .create();

            /* Get the region of current server */
            String region = GlobalKernelReferences.nodeServer.getRegion();
            AppObjectStub appStub = createPolicyChain(spec, policyNameChain, region, args);
            logger.info("Sapphire Object created: " + appObjectClass.getName());
            return appStub;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create sapphire object:", e);
            return null;
        }
    }

    /**
     * Creates a complete policy chain using the spec. 1. Creates an app object stub. 2. Creates
     * client, stub and server policy instance for each DM in the chain. 3. Links them. 4. Executes
     * onCreate for group policy from innermost to outermost.
     *
     * @param spec Sapphire object spec
     * @param policyNameChain List of policies that need to be created
     * @param region Region
     * @param appArgs Arguments for application object
     * @return client side appObjectStub
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws KernelObjectNotFoundException
     * @throws KernelObjectNotCreatedException
     * @throws MicroServiceNotFoundException
     * @throws MicroServiceReplicaNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws CloneNotSupportedException
     */
    public static AppObjectStub createPolicyChain(
            MicroServiceSpec spec, List<String> policyNameChain, String region, Object[] appArgs)
            throws IOException, ClassNotFoundException, KernelObjectNotFoundException,
                    KernelObjectNotCreatedException, MicroServiceNotFoundException,
                    MicroServiceReplicaNotFoundException, InstantiationException,
                    IllegalAccessException, CloneNotSupportedException {
        if (policyNameChain == null || policyNameChain.size() == 0) return null;
        AppObjectStub appStub = null;
        ServerPolicy outerServerPolicy = null;
        KernelObjectStub outerServerPolicyStub = null;
        List<SapphirePolicyContainer> processedPolicies = new ArrayList<>();

        /* Register for a sapphire object Id from OMS */
        MicroServiceID microServiceID =
                GlobalKernelReferences.nodeServer.oms.registerSapphireObject();

        /* Create a policy chain for the primary */
        for (int i = 0; i < policyNameChain.size(); i++) {
            List<String> policiesToCreate =
                    new ArrayList(policyNameChain.subList(i, policyNameChain.size()));

            HashMap<String, Class<?>> policyMap = getPolicyMap(policyNameChain.get(i));
            Class<?> sapphireGroupPolicyClass = policyMap.get("sapphireGroupPolicyClass");

            /* Create the Kernel Object for the Group Policy and get the Group Policy Stub from OMS */
            GroupPolicy groupPolicyStub =
                    GlobalKernelReferences.nodeServer.oms.createGroupPolicy(
                            sapphireGroupPolicyClass, microServiceID);

            createPolicyInstance(
                    microServiceID,
                    groupPolicyStub,
                    policyMap,
                    policiesToCreate,
                    processedPolicies,
                    spec);

            ServerPolicy serverPolicy = processedPolicies.get(i).getServerPolicy();
            ClientPolicy clientPolicy = processedPolicies.get(i).getClientPolicy();

            /* Check and get the previous DM's container if available. So that, server side and client side chain links can
            be updated between the current DM and previous DM. If the previous DM's container is not available,
            then DM/Policy being created is either for single DM based SO or It is the first DM/Policy in Multi DM based SO
            (i.e., last mile server policy to SO */
            // TODO: See if below logic can be separated out to a method and shared with replicate()
            if (i == 0) {
                serverPolicy.$__initialize(spec, appArgs);
                appStub = (AppObjectStub) serverPolicy.sapphire_getAppObject().getObject();
                // TODO(multi-lang): We may need to create a clone for non-java app object stub.
                appStub = spec.getLang() == Language.java ? createClientAppStub(appStub) : appStub;
                appStub.$__initialize(clientPolicy);
            } else {
                /* Previous server policy stub object acts as Sapphire Object(SO) to the current server policy */
                serverPolicy.$__initialize(
                        new AppObject(Utils.ObjectCloner.deepCopy(outerServerPolicyStub)));
                outerServerPolicy.setPreviousServerPolicy(serverPolicy);
                outerServerPolicyStub.$__setNextClientPolicy(clientPolicy);
            }

            outerServerPolicyStub = processedPolicies.get(i).getServerPolicyStub();
            outerServerPolicy = serverPolicy;
        }

        /* Execute GroupPolicy.onCreate() in the chain starting from inner most instance */
        // TODO: Can node selection constraints be passed to the next (inner) group policy in
        // an iterative way?
        for (int j = processedPolicies.size() - 1; j >= 0; j--) {
            GroupPolicy groupPolicyStub = processedPolicies.get(j).getGroupPolicyStub();
            ServerPolicy serverPolicyStub =
                    (ServerPolicy) processedPolicies.get(j).getServerPolicyStub();
            groupPolicyStub.onCreate(region, serverPolicyStub, spec);
        }

        return appStub;
    }

    /**
     * Creates a policy instance for client, server and stub.
     *
     * @param microServiceID MicroService ID
     * @param groupPolicyStub Group Policy stub
     * @param policyMap policy map for client, server and group policy based on input policy name
     * @param policyNamesToCreate name of polices that need to be created (this and inner policies)
     * @param processedPolicies Policies processed so far (created and linked)
     * @param spec Sapphire object spec
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws KernelObjectNotFoundException
     * @throws KernelObjectNotCreatedException
     * @throws MicroServiceNotFoundException
     * @throws MicroServiceReplicaNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static void createPolicyInstance(
            MicroServiceID microServiceID,
            GroupPolicy groupPolicyStub,
            HashMap<String, Class<?>> policyMap,
            List<String> policyNamesToCreate,
            List<PolicyContainer> processedPolicies,
            MicroServiceSpec spec)
            throws IOException, ClassNotFoundException, KernelObjectNotFoundException,
                    KernelObjectNotCreatedException, MicroServiceNotFoundException,
                    MicroServiceReplicaNotFoundException, InstantiationException,
                    IllegalAccessException {
        String policyName = policyNamesToCreate.get(0);

        /* Get the policy used by the Sapphire Object we need to create */
        Class<?> sapphireServerPolicyClass = policyMap.get("sapphireServerPolicyClass");
        Class<?> sapphireClientPolicyClass = policyMap.get("sapphireClientPolicyClass");

        /* Create the Kernel Object for the Server Policy, and get the Server Policy Stub */
        ServerPolicy serverPolicyStub = (ServerPolicy) getPolicyStub(sapphireServerPolicyClass);

        /* Create the Client Policy Object */
        ClientPolicy client = (ClientPolicy) sapphireClientPolicyClass.newInstance();

        /* Initialize the server policy and return a local pointer to the object itself */
        Policy.ServerPolicy serverPolicy = initializeServerPolicy(serverPolicyStub);

        registerSapphireReplica(microServiceID, serverPolicy, serverPolicyStub);

        /* Link everything together */
        // TODO: client is unncessary for outer policies of a replica.
        client.setServer(serverPolicyStub);
        client.onCreate(groupPolicyStub, spec);

        // Note that subList is non serializable; hence, the new list creation.
        // TODO: next policies won't be needed if sapphire_replicate() is updated to use
        // iterative logic.
        policyNamesToCreate.remove(0);
        serverPolicy.setNextPolicyNames(policyNamesToCreate);
        serverPolicyStub.setIsLastPolicy(policyNamesToCreate.size() == 0);

        PolicyContainer processedPolicy = new PolicyContainer(policyName, groupPolicyStub);
        processedPolicy.setServerPolicy(serverPolicy);
        processedPolicy.setServerPolicyStub((KernelObjectStub) serverPolicyStub);
        processedPolicy.setClientPolicy(client);
        processedPolicies.add(processedPolicy);

        // Create a copy to set processed policies up to this point.
        List<PolicyContainer> processedPoliciesSoFar = new ArrayList(processedPolicies);
        serverPolicy.setProcessedPolicies(processedPoliciesSoFar);
        serverPolicyStub.setProcessedPolicies(processedPoliciesSoFar);

        serverPolicy.onCreate(groupPolicyStub, spec);
    }

    /**
     * Deletes the given sapphire object
     *
     * @param stub
     */
    public static void delete_(Object stub) {
        if (!(stub instanceof AppObjectStub)) {
            throw new RuntimeException("Tried to delete invalid sapphire object");
        }

        MicroServiceID microServiceId = null;
        try {
            AppObjectStub appObjectStub = (AppObjectStub) stub;
            Field field =
                    appObjectStub
                            .getClass()
                            .getDeclaredField(GlobalStubConstants.APPSTUB_POLICY_CLIENT_FIELD_NAME);
            field.setAccessible(true);
            Policy.ClientPolicy clientPolicy = (Policy.ClientPolicy) field.get(appObjectStub);
            microServiceId = clientPolicy.getGroup().getSapphireObjId();
            GlobalKernelReferences.nodeServer.oms.delete(microServiceId);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Tried to delete invalid sapphire object.", e);
        } catch (MicroServiceNotFoundException e) {
            /* Ignore it. It might have happened that sapphire object is already deleted and still hold reference */
            logger.warning(String.format("%s is not found. Probably deleted.", microServiceId));
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete sapphire object.", e);
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
    public static Policy.GroupPolicy createGroupPolicy(
            Class<?> policyClass, SapphireObjectID sapphireObjId)
            throws ClassNotFoundException, KernelObjectNotCreatedException {
        Policy.GroupPolicy groupPolicyStub = (GroupPolicy) getPolicyStub(policyClass);
        try {
            GroupPolicy groupPolicy = initializeGroupPolicy(groupPolicyStub);
            groupPolicyStub.setSapphireObjId(microServiceId);
            groupPolicy.setSapphireObjId(microServiceId);
        } catch (KernelObjectNotFoundException e) {
            logger.severe(
                    "Failed to find the group kernel object created just before it. Exception info: "
                            + e);
            throw new KernelObjectNotCreatedException("Failed to find the kernel object", e);
        }

        return groupPolicyStub;
    }

    /* Returns a pointer to the given Sapphire Object */
    // TODO: how to implement this ?
    public static Object this_(SapphireObject so) {

        AppObjectStub appObject = (AppObjectStub) so;
        return null;
    }

    /**
     * Constructs a policy map for each client, server and group policy based on input policy name.
     *
     * @param policyName
     * @return hash map for policies
     * @throws Exception
     */
    public static HashMap<String, Class<?>> getPolicyMap(String policyName)
            throws ClassNotFoundException {
        HashMap<String, Class<?>> policyMap = new HashMap<String, Class<?>>();
        Class<?> policy = Class.forName(policyName);

        /* Extract the policy component classes (server, client and group) */
        Class<?>[] policyClasses = policy.getDeclaredClasses();

        /* TODO (Sungwook, 2018-10-2) Collapse into a smaller code for statements below
        E.g. policyClass in (Server, Client, Group) {..}
        */
        for (Class<?> c : policyClasses) {
            if (Policy.ServerPolicy.class.isAssignableFrom(c)) {
                policyMap.put("sapphireServerPolicyClass", c);
                continue;
            }
            if (Policy.ClientPolicy.class.isAssignableFrom(c)) {
                policyMap.put("sapphireClientPolicyClass", c);
                continue;
            }
            if (Policy.GroupPolicy.class.isAssignableFrom(c)) {
                policyMap.put("sapphireGroupPolicyClass", c);
                continue;
            }
        }

        /* If no policies specified use the defaults */
        if (!policyMap.containsKey("sapphireServerPolicyClass"))
            policyMap.put("sapphireServerPolicyClass", DefaultPolicy.DefaultServerPolicy.class);
        if (!policyMap.containsKey("sapphireClientPolicyClass"))
            policyMap.put("sapphireClientPolicyClass", DefaultPolicy.DefaultClientPolicy.class);
        if (!policyMap.containsKey("sapphireGroupPolicyClass"))
            policyMap.put("sapphireGroupPolicyClass", DefaultPolicy.DefaultGroupPolicy.class);

        return policyMap;
    }

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
     * Processes Sapphire replica by registering for a replica ID and handler for the replica to
     * OMS.
     *
     * @param microServiceId Sapphire object ID
     * @param serverPolicy ServerPolicy
     * @param serverPolicyStub ServerPolicy stub
     * @throws MicroServiceNotFoundException
     * @throws MicroServiceReplicaNotFoundException
     * @throws RemoteException
     */
    private static void registerSapphireReplica(
            MicroServiceID microServiceId, ServerPolicy serverPolicy, ServerPolicy serverPolicyStub)
            throws MicroServiceNotFoundException, MicroServiceReplicaNotFoundException,
                    RemoteException {
        /* Register for a replica ID from OMS */
        ReplicaID replicaId =
                GlobalKernelReferences.nodeServer.oms.registerSapphireReplica(microServiceId);

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
        GlobalKernelReferences.nodeServer.oms.setSapphireReplicaDispatcher(
                sapphireReplicaId, replicaHandler);
    }

    public static Class<?>[] getParamsClasses(Object[] params) throws ClassNotFoundException {
        ArrayList<Class<?>> argClassesList = new ArrayList<Class<?>>();
        for (Object param : params) {
            argClassesList.add(getParamClassStripStub(param));
        }
        Class<?>[] argClasses = new Class<?>[argClassesList.size()];
        return argClassesList.toArray(argClasses);
    }
}

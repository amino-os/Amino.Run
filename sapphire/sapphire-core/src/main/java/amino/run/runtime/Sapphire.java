package amino.run.runtime;

import static amino.run.policy.Policy.ClientPolicy;
import static amino.run.policy.Policy.GroupPolicy;
import static amino.run.policy.Policy.ServerPolicy;

import amino.run.app.DMSpec;
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
import amino.run.compiler.GlobalStubConstants;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.KernelObjectFactory;
import amino.run.kernel.common.KernelObjectNotCreatedException;
import amino.run.kernel.common.KernelObjectNotFoundException;
import amino.run.kernel.common.KernelObjectStub;
import amino.run.kernel.common.KernelObjectStubNotCreatedException;
import amino.run.policy.DefaultPolicy;
import amino.run.policy.Policy;
import amino.run.policy.PolicyContainer;
import amino.run.policy.Upcalls;
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

            List<PolicyContainer> processedPolicies = new ArrayList<>();
            List<PolicyContainer> policyNameChain = getPolicyNameChain(spec);

            if (policyNameChain.size() == 0) {
                String defaultPolicyName = DefaultPolicy.class.getName();
                policyNameChain.add(new PolicyContainer(defaultPolicyName, null));
            }

            /* Register for a sapphire object Id from OMS */
            MicroServiceID microServiceId =
                    GlobalKernelReferences.nodeServer.oms.registerSapphireObject();

            /* Get the region of current server */
            String region = GlobalKernelReferences.nodeServer.getRegion();
            appStub =
                    createPolicy(
                            microServiceId, spec, policyNameChain, processedPolicies, region, args);
        } catch (Exception e) {
            String msg = String.format("Failed to create sapphire object '%s'", spec);
            logger.log(Level.SEVERE, msg, e);
            throw new MicroServiceCreationException(msg, e);
        }

        return appStub;
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
            Map<String, Upcalls.SapphirePolicyConfig> configMap =
                    Utils.toSapphirePolicyConfig(annotations);
            List<PolicyContainer> processedPolicies = new ArrayList<PolicyContainer>();
            List<PolicyContainer> policyNameChain = getPolicyNameChain(annotations);

            if (policyNameChain.size() == 0) {
                String defaultPolicyName = DefaultPolicy.class.getName();
                policyNameChain.add(new PolicyContainer(defaultPolicyName, null));
            }

            MicroServiceSpec spec =
                    MicroServiceSpec.newBuilder()
                            .setLang(Language.java)
                            .setJavaClassName(appObjectClass.getName())
                            .create();

            /* Register for a sapphire object Id from OMS */
            MicroServiceID microServiceId =
                    GlobalKernelReferences.nodeServer.oms.registerSapphireObject();

            /* Get the region of current server */
            String region = GlobalKernelReferences.nodeServer.getRegion();
            AppObjectStub appStub =
                    createPolicy(
                            microServiceId, spec, policyNameChain, processedPolicies, region, args);
            logger.info("Sapphire Object created: " + appObjectClass.getName());
            return appStub;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create sapphire object:", e);
            return null;
        }
    }

    /**
     * Creates a policy name chain based on annotations and return list of PolicyContainer.
     *
     * @param annotations Annotations that contain chain of policy names.
     * @return List of PolicyContainer with the policy names parsed from annotations.
     * @throws Exception
     */
    private static List<PolicyContainer> getPolicyNameChain(Annotation[] annotations) {
        List<PolicyContainer> policyNameChain = new ArrayList<PolicyContainer>();

        for (Annotation annotation : annotations) {
            if (annotation instanceof SapphireConfiguration) {
                String[] policyAnnotations = ((SapphireConfiguration) annotation).Policies();
                for (String policyAnnotation : policyAnnotations) {
                    String[] policyNames = policyAnnotation.split(",");
                    for (String policyName : policyNames) {
                        policyNameChain.add(new PolicyContainer(policyName.trim(), null));
                    }
                }
            }
        }

        return policyNameChain;
    }

    private static List<PolicyContainer> getPolicyNameChain(MicroServiceSpec spec) {
        List<PolicyContainer> policyNameChain = new ArrayList<PolicyContainer>();

        for (DMSpec dm : spec.getDmList()) {
            PolicyContainer c = new PolicyContainer(dm.getName(), null);
            policyNameChain.add(c);
        }

        return policyNameChain;
    }

    /**
     * Creates app object stub along with instantiation of policies and stubs, appends policies
     * processed for given policyNameChain to existing processed policy list and returns app object
     * stub to client.
     *
     * @param microServiceId Sapphire object Id
     * @param spec Sapphire object spec
     * @param policyNameChain List of policies that need to be created
     * @param processedPolicies List of policies that were already created. Policies created with
     *     this invocation gets appended to existing list
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
    public static AppObjectStub createPolicy(
            MicroServiceID microServiceId,
            MicroServiceSpec spec,
            List<PolicyContainer> policyNameChain,
            List<PolicyContainer> processedPolicies,
            String region,
            Object[] appArgs)
            throws IOException, ClassNotFoundException, KernelObjectNotFoundException,
                    KernelObjectNotCreatedException, MicroServiceNotFoundException,
                    MicroServiceReplicaNotFoundException, InstantiationException,
                    IllegalAccessException, CloneNotSupportedException {
        if (policyNameChain == null || policyNameChain.size() == 0) return null;
        String policyName = policyNameChain.get(0).getPolicyName();
        GroupPolicy existingGroupPolicy = policyNameChain.get(0).getGroupPolicyStub();
        AppObject appObject = null;

        /* Get the annotations added for the Application class. */

        /* Get the policy used by the Sapphire Object we need to create */
        HashMap<String, Class<?>> policyMap = getPolicyMap(policyName);
        Class<?> sapphireServerPolicyClass = policyMap.get("sapphireServerPolicyClass");
        Class<?> sapphireClientPolicyClass = policyMap.get("sapphireClientPolicyClass");
        Class<?> sapphireGroupPolicyClass = policyMap.get("sapphireGroupPolicyClass");

        /* Create and the Kernel Object for the Group Policy and get the Group Policy Stub
        Note that group policy does not need to update hostname because it only applies to
        individual server in multi-policy scenario */
        GroupPolicy groupPolicyStub;
        if (existingGroupPolicy == null) {
            /* Create the Kernel Object for the Group Policy and get the Group Policy Stub from OMS */
            groupPolicyStub =
                    GlobalKernelReferences.nodeServer.oms.createGroupPolicy(
                            sapphireGroupPolicyClass, microServiceId);
        } else {
            groupPolicyStub = existingGroupPolicy;
            /* Get the app object to be cloned from the base server */
            appObject = policyNameChain.get(0).getServerPolicy().sapphire_getAppObject();
        }

        /* Create the Kernel Object for the Server Policy, and get the Server Policy Stub */
        Policy.ServerPolicy serverPolicyStub =
                (ServerPolicy) getPolicyStub(sapphireServerPolicyClass);

        /* Create the Client Policy Object */
        ClientPolicy client = (Policy.ClientPolicy) sapphireClientPolicyClass.newInstance();

        /* Initialize the server policy and return a local pointer to the object itself */
        Policy.ServerPolicy serverPolicy = initializeServerPolicy(serverPolicyStub);
        registerSapphireReplica(microServiceId, serverPolicy, serverPolicyStub);

        /* Link everything together */
        client.setServer(serverPolicyStub);
        client.onCreate(groupPolicyStub, spec);

        PolicyContainer prevContainer = null;
        /* Check and get the previous DM's container if available. So that, server side and client side chain links can
        be updated between the current DM and previous DM. If the previous DM's container is not available,
        then DM/Policy being created is either for single DM based SO or It is the first DM/Policy in Multi DM based SO
        (i.e., last mile server policy to SO */
        if (!processedPolicies.isEmpty()) {
            prevContainer = processedPolicies.get(processedPolicies.size() - 1);
        }

        if (prevContainer != null) {
            initServerPolicy(serverPolicy, prevContainer, client);
        } else {
            initAppStub(spec, serverPolicy, appArgs, appObject);
        }

        // Note that subList is non serializable; hence, the new list creation.
        List<PolicyContainer> nextPoliciesToCreate =
                new ArrayList<>(policyNameChain.subList(1, policyNameChain.size()));

        serverPolicy.onCreate(groupPolicyStub, spec);
        serverPolicy.setNextPolicies(nextPoliciesToCreate);
        serverPolicyStub.setIsLastPolicy(nextPoliciesToCreate.size() == 0);

        PolicyContainer processedPolicy = new PolicyContainer(policyName, groupPolicyStub);
        processedPolicy.setServerPolicy(serverPolicy);
        processedPolicy.setServerPolicyStub((KernelObjectStub) serverPolicyStub);
        processedPolicies.add(processedPolicy);

        // Create a copy to set processed policies up to this point.
        List<PolicyContainer> processedPoliciesSoFar = new ArrayList<>(processedPolicies);
        serverPolicy.setProcessedPolicies(processedPoliciesSoFar);
        serverPolicyStub.setProcessedPolicies(processedPoliciesSoFar);

        if (nextPoliciesToCreate.size() != 0) {
            createPolicy(
                    microServiceId, spec, nextPoliciesToCreate, processedPolicies, region, appArgs);
        }

        AppObjectStub appStub = null;

        // server policy stub at this moment has the full policy chain; safe to add to group
        if (existingGroupPolicy == null) {
            groupPolicyStub.onCreate(region, serverPolicyStub, spec);

            /* Build client side appObjectStub from appObjectStub within AppObject of first DM's server policy in chain
            and inject its client policy into it. An instance of Client side AppObjectStub is created at the end of
            successful creation of SO. */
            if (processedPolicies.get(0).getServerPolicy() == serverPolicy) {
                appStub = (AppObjectStub) serverPolicy.sapphire_getAppObject().getObject();
                // TODO(multi-lang): We may need to create a clone for non-java app object stub.
                appStub = spec.getLang() == Language.java ? createClientAppStub(appStub) : appStub;
                appStub.$__initialize(client);
            }
        }

        // TODO: Add a pin_to_server logic here if not pinned by the last DM. This is needed for DMs
        // that do not pin the default DM. It can be added once passing down node selection
        // constraint is implemented.
        return appStub;
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
            Class<?> policyClass, MicroServiceID microServiceId)
            throws RemoteException, ClassNotFoundException, KernelObjectNotCreatedException,
                    MicroServiceNotFoundException {
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

    public static KernelObjectStub getPolicyStub(Class<?> policyClass, KernelOID oid)
            throws KernelObjectStubNotCreatedException {
        String policyStubClassName =
                GlobalStubConstants.getPolicyPackageName()
                        + "."
                        + RMIUtil.getShortName(policyClass)
                        + GlobalStubConstants.STUB_SUFFIX;
        KernelObjectStub policyStub;
        try {
            policyStub = KernelObjectFactory.createStub(Class.forName(policyStubClassName), oid);
        } catch (Exception e) {
            throw new KernelObjectStubNotCreatedException(
                    "Failed to create a policy stub object", e);
        }

        return policyStub;
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
        // paramClassName = paramClassName.substring(0, index);

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
                replicaId, replicaHandler);
    }

    /**
     * Connects the link between server policy in the chain.
     *
     * @param serverPolicy server policy
     * @param prevContainer previous DM's container
     * @param clientPolicy client policy
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static void initServerPolicy(
            ServerPolicy serverPolicy,
            PolicyContainer prevContainer,
            Policy.ClientPolicy clientPolicy)
            throws IOException, ClassNotFoundException {
        KernelObjectStub prevServerPolicyStub = prevContainer.getServerPolicyStub();

        /* Previous server policy stub object acts as Sapphire Object(SO) to the current server policy */
        serverPolicy.$__initialize(
                new AppObject(Utils.ObjectCloner.deepCopy(prevServerPolicyStub)));
        prevContainer.getServerPolicy().setPreviousServerPolicy(serverPolicy);
        prevServerPolicyStub.$__setNextClientPolicy(clientPolicy);
    }

    /**
     * Initializes server policy and stub with app object.
     *
     * @param spec sapphire object spec
     * @param serverPolicy server policy
     * @param appArgs app arguments
     * @param appObject app Object
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private static void initAppStub(
            MicroServiceSpec spec, ServerPolicy serverPolicy, Object[] appArgs, AppObject appObject)
            throws ClassNotFoundException, IOException {
        if (appObject != null) {
            appObject = (AppObject) Utils.ObjectCloner.deepCopy(appObject);
            serverPolicy.$__initialize(appObject);
        } else {
            serverPolicy.$__initialize(spec, appArgs);
        }
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

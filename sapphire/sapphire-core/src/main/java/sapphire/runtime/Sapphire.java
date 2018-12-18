package sapphire.runtime;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.harmony.rmi.common.RMIUtil;
import sapphire.app.DMSpec;
import sapphire.app.Language;
import sapphire.app.SapphireObject;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.*;
import sapphire.compiler.GlobalStubConstants;
import sapphire.kernel.common.*;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.DefaultSapphirePolicy.DefaultClientPolicy;
import sapphire.policy.DefaultSapphirePolicy.DefaultGroupPolicy;
import sapphire.policy.DefaultSapphirePolicy.DefaultServerPolicy;
import sapphire.policy.SapphirePolicy.SapphireClientPolicy;
import sapphire.policy.SapphirePolicy.SapphireGroupPolicy;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;
import sapphire.policy.SapphirePolicyContainer;
import sapphire.policy.SapphirePolicyUpcalls;

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
    public static Object new_(SapphireObjectSpec spec, Object... args)
            throws SapphireObjectCreationException {
        AppObjectStub appStub = null;
        try {
            logger.info("Creating object for spec:" + spec);
            if (spec.getLang() == Language.java && spec.getDmList().isEmpty()) {
                Class<?> appObjectClass = Class.forName(spec.getJavaClassName());
                return new_(appObjectClass, args);
            }

            List<SapphirePolicyContainer> processedPolicies = new ArrayList<>();
            List<SapphirePolicyContainer> policyNameChain = getPolicyNameChain(spec);
            Map<String, SapphirePolicyUpcalls.SapphirePolicyConfig> configMap =
                    Utils.fromDMSpecListToFlatConfigMap(spec.getDmList());

            if (policyNameChain.size() == 0) {
                String defaultPolicyName = DefaultSapphirePolicy.class.getName();
                policyNameChain.add(new SapphirePolicyContainer(defaultPolicyName, null));
            }

            /* Register for a sapphire object Id from OMS */
            SapphireObjectID sapphireObjId =
                    GlobalKernelReferences.nodeServer.oms.registerSapphireObject();

            List<SapphirePolicyContainer> policyList =
                    createPolicy(
                            sapphireObjId,
                            spec,
                            configMap,
                            new boolean[1],
                            policyNameChain,
                            processedPolicies,
                            "",
                            args);

            appStub = policyList.get(0).getServerPolicy().sapphire_getAppObjectStub();
        } catch (Exception e) {
            String msg = String.format("Failed to create sapphire object '%s'", spec);
            logger.log(Level.SEVERE, msg, e);
            throw new SapphireObjectCreationException(msg, e);
        }

        return appStub;
    }

    /**
     * WARN: This method only works for Java sapphire object. This method has been deprecated.
     * Please use {@link #new_(SapphireObjectSpec, Object...)}. We keep this method because we have
     * Java demo apps that call this method directly.
     *
     * @param appObjectClass the class of the app object
     * @param args the arguments to app object constructor
     * @return the App Object stub
     * @deprecated Please use {@link #new_(SapphireObjectSpec, Object...)}
     */
    public static Object new_(Class<?> appObjectClass, Object... args) {
        try {
            Annotation[] annotations = appObjectClass.getAnnotations();
            Map<String, SapphirePolicyUpcalls.SapphirePolicyConfig> configMap =
                    Utils.toSapphirePolicyConfig(annotations);
            List<SapphirePolicyContainer> processedPolicies =
                    new ArrayList<SapphirePolicyContainer>();
            List<SapphirePolicyContainer> policyNameChain = getPolicyNameChain(annotations);

            if (policyNameChain.size() == 0) {
                String defaultPolicyName = DefaultSapphirePolicy.class.getName();
                policyNameChain.add(new SapphirePolicyContainer(defaultPolicyName, null));
            }

            SapphireServerPolicy previousServerPolicy = null;
            KernelObjectStub previousServerPolicyStub = null;

            SapphireObjectSpec spec =
                    SapphireObjectSpec.newBuilder()
                            .setLang(Language.java)
                            .setJavaClassName(appObjectClass.getName())
                            .create();

            /* Register for a sapphire object Id from OMS */
            SapphireObjectID sapphireObjId =
                    GlobalKernelReferences.nodeServer.oms.registerSapphireObject();

            List<SapphirePolicyContainer> policyList =
                    createPolicy(
                            sapphireObjId,
                            spec,
                            configMap,
                            new boolean[1],
                            policyNameChain,
                            processedPolicies,
                            "",
                            args);

            AppObjectStub appStub = policyList.get(0).getServerPolicy().sapphire_getAppObjectStub();
            logger.info("Sapphire Object created: " + appObjectClass.getName());
            return appStub;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create sapphire object:", e);
            return null;
        }
    }

    /**
     * Creates a policy name chain based on annotations and return list of SapphirePolicyContainer.
     *
     * @param annotations Annotations that contain chain of policy names.
     * @return List of SapphirePolicyContainer with the policy names parsed from annotations.
     * @throws Exception
     */
    private static List<SapphirePolicyContainer> getPolicyNameChain(Annotation[] annotations) {
        List<SapphirePolicyContainer> policyNameChain = new ArrayList<SapphirePolicyContainer>();

        for (Annotation annotation : annotations) {
            if (annotation instanceof SapphireConfiguration) {
                String[] policyAnnotations = ((SapphireConfiguration) annotation).Policies();
                for (String policyAnnotation : policyAnnotations) {
                    String[] policyNames = policyAnnotation.split(",");
                    for (String policyName : policyNames) {
                        policyNameChain.add(new SapphirePolicyContainer(policyName.trim(), null));
                    }
                }
            }
        }

        return policyNameChain;
    }

    private static List<SapphirePolicyContainer> getPolicyNameChain(SapphireObjectSpec spec) {
        List<SapphirePolicyContainer> policyNameChain = new ArrayList<SapphirePolicyContainer>();

        for (DMSpec dm : spec.getDmList()) {
            SapphirePolicyContainer c = new SapphirePolicyContainer(dm.getName(), null);
            policyNameChain.add(c);
        }

        return policyNameChain;
    }

    /**
     * Creates app object stub along with instantiation of policies and stubs, and returns processed
     * policies for given policyNameChain.
     *
     * @param sapphireObjId Sapphire object Id
     * @param spec Sapphire object spec
     * @param configMap Sapphire policy configuration map
     * @param chainPinned Flag indicating chain is pinned
     * @param policyNameChain List of policies that need to be created
     * @param processedPolicies List of policies that were already created
     * @param region Region
     * @param appArgs Arguments for application object
     * @return processedPolicies
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws KernelObjectNotFoundException
     * @throws KernelObjectNotCreatedException
     * @throws SapphireObjectNotFoundException
     * @throws SapphireObjectReplicaNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws CloneNotSupportedException
     */
    public static List<SapphirePolicyContainer> createPolicy(
            SapphireObjectID sapphireObjId,
            SapphireObjectSpec spec,
            Map<String, SapphirePolicyUpcalls.SapphirePolicyConfig> configMap,
            boolean[] chainPinned,
            List<SapphirePolicyContainer> policyNameChain,
            List<SapphirePolicyContainer> processedPolicies,
            String region,
            Object[] appArgs)
            throws IOException, ClassNotFoundException, KernelObjectNotFoundException,
                    KernelObjectNotCreatedException, SapphireObjectNotFoundException,
                    SapphireObjectReplicaNotFoundException, InstantiationException,
                    IllegalAccessException, CloneNotSupportedException {
        if (policyNameChain == null || policyNameChain.size() == 0) return null;
        String policyName = policyNameChain.get(0).getPolicyName();
        SapphireGroupPolicy existingGroupPolicy = policyNameChain.get(0).getGroupPolicyStub();
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
        SapphireGroupPolicy groupPolicyStub;
        if (existingGroupPolicy == null) {
            /* Create the Kernel Object for the Group Policy and get the Group Policy Stub from OMS */
            groupPolicyStub =
                    GlobalKernelReferences.nodeServer.oms.createGroupPolicy(
                            sapphireGroupPolicyClass, sapphireObjId, configMap);
        } else {
            groupPolicyStub = existingGroupPolicy;
            /* Get the app object to be cloned from the base server */
            appObject = policyNameChain.get(0).getServerPolicy().sapphire_getAppObject();
        }

        /* Create the Kernel Object for the Server Policy, and get the Server Policy Stub */
        SapphireServerPolicy serverPolicyStub =
                (SapphireServerPolicy) getPolicyStub(sapphireServerPolicyClass);

        /* Create the Client Policy Object */
        SapphireClientPolicy client =
                (SapphireClientPolicy) sapphireClientPolicyClass.newInstance();

        /* Initialize the server policy and return a local pointer to the object itself */
        SapphireServerPolicy serverPolicy = initializeServerPolicy(serverPolicyStub);

        registerSapphireReplica(sapphireObjId, serverPolicy, serverPolicyStub);

        /* Link everything together */
        client.setServer(serverPolicyStub);
        client.onCreate(groupPolicyStub, spec);

        SapphirePolicyContainer prevContainer = null;
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
            initAppStub(spec, serverPolicy, client, appArgs, appObject);
        }

        // Note that subList is non serializable; hence, the new list creation.
        List<SapphirePolicyContainer> nextPoliciesToCreate =
                new ArrayList<SapphirePolicyContainer>(
                        policyNameChain.subList(1, policyNameChain.size()));

        serverPolicy.onCreate(groupPolicyStub, spec);
        serverPolicy.setNextPolicies(nextPoliciesToCreate);

        SapphirePolicyContainer processedPolicy =
                new SapphirePolicyContainer(policyName, groupPolicyStub);
        processedPolicy.setServerPolicy(serverPolicy);
        processedPolicy.setServerPolicyStub((KernelObjectStub) serverPolicyStub);
        processedPolicies.add(processedPolicy);

        // Create a copy to set processed policies up to this point.
        List<SapphirePolicyContainer> processedPoliciesSoFar =
                new ArrayList<SapphirePolicyContainer>(processedPolicies);
        serverPolicy.setProcessedPolicies(processedPoliciesSoFar);
        serverPolicyStub.setProcessedPolicies(processedPoliciesSoFar);

        if (nextPoliciesToCreate.size() != 0) {
            // TODO: hacks for demo
            if (policyName != null && policyName.contains("DHT")) {
                if (region == null || region.isEmpty()) {
                    List<String> regions = GlobalKernelReferences.nodeServer.oms.getRegions();
                    logger.info("Regions available for DHT: " + regions);
                    region = regions.get(0);
                }
            }
            createPolicy(
                    sapphireObjId,
                    spec,
                    configMap,
                    chainPinned,
                    nextPoliciesToCreate,
                    processedPolicies,
                    region,
                    appArgs);
        }

        // server policy stub at this moment has the full policy chain; safe to add to group
        if (existingGroupPolicy == null) {
            serverPolicyStub.setAlreadyPinned(chainPinned[0]);
            groupPolicyStub.onCreate(region, serverPolicyStub, spec);
            chainPinned[0] = true;
            groupPolicyStub.addServer(serverPolicyStub);
        }

        return processedPolicies;
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

        SapphireObjectID sapphireObjId = null;
        try {
            AppObjectStub appObjectStub = (AppObjectStub) stub;
            Field field =
                    appObjectStub
                            .getClass()
                            .getDeclaredField(GlobalStubConstants.APPSTUB_POLICY_CLIENT_FIELD_NAME);
            field.setAccessible(true);
            SapphireClientPolicy clientPolicy = (SapphireClientPolicy) field.get(appObjectStub);
            sapphireObjId = clientPolicy.getGroup().getSapphireObjId();
            GlobalKernelReferences.nodeServer.oms.deleteSapphireObject(sapphireObjId);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Tried to delete invalid sapphire object.", e);
        } catch (SapphireObjectNotFoundException e) {
            /* Ignore it. It might have happened that sapphire object is already deleted and still hold reference */
            logger.warning(String.format("%s is not found. Probably deleted.", sapphireObjId));
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete sapphire object.", e);
        }
    }

    /**
     * Creates the group policy instance returns group policy object Stub
     *
     * @param policyClass
     * @param sapphireObjId
     * @param configMap
     * @return Returns group policy object stub
     * @throws RemoteException
     * @throws ClassNotFoundException
     * @throws KernelObjectNotCreatedException
     * @throws SapphireObjectNotFoundException
     */
    public static SapphireGroupPolicy createGroupPolicy(
            Class<?> policyClass,
            SapphireObjectID sapphireObjId,
            Map<String, SapphirePolicyUpcalls.SapphirePolicyConfig> configMap)
            throws RemoteException, ClassNotFoundException, KernelObjectNotCreatedException,
                    SapphireObjectNotFoundException {
        SapphireGroupPolicy groupPolicyStub = (SapphireGroupPolicy) getPolicyStub(policyClass);
        try {
            SapphireGroupPolicy groupPolicy = initializeGroupPolicy(groupPolicyStub);
            groupPolicyStub.setSapphireObjId(sapphireObjId);
            groupPolicy.setSapphireObjId(sapphireObjId);
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
            if (SapphireServerPolicy.class.isAssignableFrom(c)) {
                policyMap.put("sapphireServerPolicyClass", c);
                continue;
            }
            if (SapphireClientPolicy.class.isAssignableFrom(c)) {
                policyMap.put("sapphireClientPolicyClass", c);
                continue;
            }
            if (SapphireGroupPolicy.class.isAssignableFrom(c)) {
                policyMap.put("sapphireGroupPolicyClass", c);
                continue;
            }
        }

        /* If no policies specified use the defaults */
        if (!policyMap.containsKey("sapphireServerPolicyClass"))
            policyMap.put("sapphireServerPolicyClass", DefaultServerPolicy.class);
        if (!policyMap.containsKey("sapphireClientPolicyClass"))
            policyMap.put("sapphireClientPolicyClass", DefaultClientPolicy.class);
        if (!policyMap.containsKey("sapphireGroupPolicyClass"))
            policyMap.put("sapphireGroupPolicyClass", DefaultGroupPolicy.class);

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

    public static SapphireGroupPolicy initializeGroupPolicy(SapphireGroupPolicy groupPolicyStub)
            throws KernelObjectNotFoundException {
        KernelOID groupOID = ((KernelObjectStub) groupPolicyStub).$__getKernelOID();
        SapphireGroupPolicy groupPolicy =
                (SapphireGroupPolicy) GlobalKernelReferences.nodeServer.getObject(groupOID);
        groupPolicy.$__setKernelOID(groupOID);
        return groupPolicy;
    }

    private static SapphireServerPolicy initializeServerPolicy(
            SapphireServerPolicy serverPolicyStub) throws KernelObjectNotFoundException {
        KernelOID serverOID = ((KernelObjectStub) serverPolicyStub).$__getKernelOID();
        SapphireServerPolicy serverPolicy =
                (SapphireServerPolicy) GlobalKernelReferences.nodeServer.getObject(serverOID);
        serverPolicy.$__setKernelOID(serverOID);
        return serverPolicy;
    }

    private static AppObjectStub getAppStub(
            SapphireObjectSpec spec, SapphireServerPolicy serverPolicy, Object[] args)
            throws CloneNotSupportedException {
        AppObjectStub appObjectStub = serverPolicy.$__initialize(spec, args);
        // TODO(multi-lang): We may need to create a clone for non-java app object stub.
        return spec.getLang() == Language.java ? createClientAppStub(appObjectStub) : appObjectStub;
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
     * @param sapphireObjId Sapphire object ID
     * @param serverPolicy SapphireServerPolicy
     * @param serverPolicyStub ServerPolicy stub
     * @throws SapphireObjectNotFoundException
     * @throws SapphireObjectReplicaNotFoundException
     * @throws RemoteException
     */
    private static void registerSapphireReplica(
            SapphireObjectID sapphireObjId,
            SapphireServerPolicy serverPolicy,
            SapphireServerPolicy serverPolicyStub)
            throws SapphireObjectNotFoundException, SapphireObjectReplicaNotFoundException,
                    RemoteException {
        /* Register for a replica ID from OMS */
        SapphireReplicaID sapphireReplicaId =
                GlobalKernelReferences.nodeServer.oms.registerSapphireReplica(sapphireObjId);

        serverPolicyStub.setReplicaId(sapphireReplicaId);
        serverPolicy.setReplicaId(sapphireReplicaId);

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
            SapphireServerPolicy serverPolicy,
            SapphirePolicyContainer prevContainer,
            SapphireClientPolicy clientPolicy)
            throws IOException, ClassNotFoundException {
        serverPolicy.setSapphireObjectSpec(prevContainer.getServerPolicy().getSapphireObjectSpec());

        KernelObjectStub prevServerPolicyStub = prevContainer.getServerPolicyStub();

        /* Previous server policy stub object acts as Sapphire Object(SO) to the current server policy */
        serverPolicy.$__initialize(
                new AppObject(Utils.ObjectCloner.deepCopy(prevServerPolicyStub)));
        prevServerPolicyStub.$__setNextClientPolicy(clientPolicy);
    }

    /**
     * Initializes server policy and stub with app object.
     *
     * @param spec sapphire object spec
     * @param serverPolicy server policy
     * @param clientPolicy client policy
     * @param appArgs app arguments
     * @param appObject app Object
     * @throws ClassNotFoundException
     * @throws CloneNotSupportedException
     * @throws IOException
     */
    // TODO (merge):
    private static void initAppStub(
            SapphireObjectSpec spec,
            SapphireServerPolicy serverPolicy,
            SapphireClientPolicy clientPolicy,
            Object[] appArgs,
            AppObject appObject)
            throws ClassNotFoundException, CloneNotSupportedException, IOException {

        AppObjectStub appStub;
        if (appObject != null) {
            appObject = (AppObject) Utils.ObjectCloner.deepCopy(appObject);
            serverPolicy.$__initialize(appObject);
        } else {
            appStub = getAppStub(spec, serverPolicy, appArgs);
            appStub.$__initialize(clientPolicy);
            serverPolicy.$__initialize(appStub);
        }

        serverPolicy.setSapphireObjectSpec(spec);
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

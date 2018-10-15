package sapphire.runtime;

import static sapphire.policy.SapphirePolicyUpcalls.SapphirePolicyConfig;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.harmony.rmi.common.RMIUtil;
import sapphire.app.DMSpec;
import sapphire.app.Language;
import sapphire.app.SapphireObject;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.*;
import sapphire.compiler.GlobalStubConstants;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectNotCreatedException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.common.KernelObjectStubNotCreatedException;
import sapphire.kernel.server.KernelObject;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.DefaultSapphirePolicy.DefaultClientPolicy;
import sapphire.policy.DefaultSapphirePolicy.DefaultGroupPolicy;
import sapphire.policy.DefaultSapphirePolicy.DefaultServerPolicy;
import sapphire.policy.SapphirePolicy.SapphireClientPolicy;
import sapphire.policy.SapphirePolicy.SapphireGroupPolicy;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;
import sapphire.policy.SapphirePolicyContainer;

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
    public static Object new_(SapphireObjectSpec spec, Object... args) {
        AppObjectStub appStub = null;
        try {
            logger.info("Creating object for spec:" + spec);
            if (spec.getLang() == Language.java && spec.getDmList().isEmpty()) {
                Class<?> appObjectClass = Class.forName(spec.getJavaClassName());
                return new_(appObjectClass, args);
            }

            List<SapphirePolicyContainer> processedPolicies = new ArrayList<>();
            List<SapphirePolicyContainer> policyNameChain = getPolicyNameChain(spec);
            Map<String, SapphirePolicyConfig> configMap =
                    Utils.fromDMSpecListToFlatConfigMap(spec.getDmList());

            if (policyNameChain.size() == 0) {
                String defaultPolicyName = DefaultSapphirePolicy.class.getName();
                policyNameChain.add(new SapphirePolicyContainer(defaultPolicyName, null));
            }

            SapphireServerPolicy previousServerPolicy = null;
            KernelObjectStub previousServerPolicyStub = null;

            /* Register for a sapphire object Id from OMS */
            SapphireObjectID sapphireObjId =
                    GlobalKernelReferences.nodeServer.oms.registerSapphireObject();

            List<SapphirePolicyContainer> policyList =
                    createPolicy(
                            sapphireObjId,
                            spec,
                            null,
                            configMap,
                            policyNameChain,
                            processedPolicies,
                            previousServerPolicy,
                            previousServerPolicyStub,
                            args);

            appStub = policyList.get(0).getServerPolicy().sapphire_getAppObjectStub();
        } catch (Exception e) {
            logger.log(
                    Level.SEVERE, String.format("Failed to create sapphire object '%s'", spec), e);
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
            Map<String, SapphirePolicyConfig> configMap = Utils.toSapphirePolicyConfig(annotations);
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
                            null,
                            configMap,
                            policyNameChain,
                            processedPolicies,
                            previousServerPolicy,
                            previousServerPolicyStub,
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
     * @param spec Sapphire object spec
     * @param policyNameChain List of policies that need to be created
     * @param processedPolicies List of policies that were already created
     * @param previousServerPolicy ServerPolicy that was created just before and needs to be linked
     * @param previousServerPolicyStub ServerPolicyStub that was created just before and needs to be
     *     linked
     * @param appArgs arguments for application object
     * @return processedPolicies
     * @throws Exception
     */
    public static List<SapphirePolicyContainer> createPolicy(
            SapphireObjectID sapphireObjId,
            SapphireObjectSpec spec,
            AppObject appObject,
            Map<String, SapphirePolicyConfig> configMap,
            List<SapphirePolicyContainer> policyNameChain,
            List<SapphirePolicyContainer> processedPolicies,
            SapphireServerPolicy previousServerPolicy,
            KernelObjectStub previousServerPolicyStub,
            Object[] appArgs)
            throws RemoteException, ClassNotFoundException, KernelObjectNotFoundException,
                    KernelObjectNotCreatedException, SapphireObjectNotFoundException,
                    SapphireObjectReplicaNotFoundException, InstantiationException,
                    InvocationTargetException, IllegalAccessException, CloneNotSupportedException {

        if (policyNameChain == null || policyNameChain.size() == 0) return null;
        String policyName = policyNameChain.get(0).getPolicyName();
        SapphireGroupPolicy existingGroupPolicy = policyNameChain.get(0).getGroupPolicyStub();
        AppObjectStub appStub = null;

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
        }

        /* Create the Kernel Object for the Server Policy, and get the Server Policy Stub */
        SapphireServerPolicy serverPolicyStub =
                (SapphireServerPolicy) getPolicyStub(sapphireServerPolicyClass);

        /* Create the Client Policy Object */
        SapphireClientPolicy client =
                (SapphireClientPolicy) sapphireClientPolicyClass.newInstance();

        /* Retreive the groupPolicy from the groupPolicyStub */
        SapphireGroupPolicy groupPolicy = (SapphireGroupPolicy) groupPolicyStub;

        /* Initialize the server policy and return a local pointer to the object itself */
        SapphireServerPolicy serverPolicy = initializeServerPolicy(serverPolicyStub);

        registerSapphireReplica(sapphireObjId, serverPolicy, serverPolicyStub);

        /* Link everything together */
        client.setServer(serverPolicyStub);
        client.onCreate(groupPolicyStub, configMap);

        if (previousServerPolicy != null) {
            initServerPolicy(
                    serverPolicy,
                    serverPolicyStub,
                    previousServerPolicy,
                    previousServerPolicyStub,
                    client);
        } else {
            initAppStub(spec, serverPolicy, serverPolicyStub, client, appArgs, appObject);
        }

        // Note that subList is non serializable; hence, the new list creation.
        List<SapphirePolicyContainer> nextPoliciesToCreate =
                new ArrayList<SapphirePolicyContainer>(
                        policyNameChain.subList(1, policyNameChain.size()));

        serverPolicy.onCreate(groupPolicyStub, configMap);
        serverPolicy.setNextPolicies(nextPoliciesToCreate);

        SapphirePolicyContainer processedPolicy =
                new SapphirePolicyContainer(policyName, groupPolicyStub);
        processedPolicy.setServerPolicy(serverPolicy);
        processedPolicy.setServerPolicyStub((KernelObjectStub) serverPolicyStub);
        processedPolicy.setKernelOID(serverPolicy.$__getKernelOID());
        processedPolicies.add(processedPolicy);

        // Create a copy to set processed policies up to this point.
        List<SapphirePolicyContainer> processedPoliciesSoFar =
                new ArrayList<SapphirePolicyContainer>(processedPolicies);
        serverPolicy.setProcessedPolicies(processedPoliciesSoFar);
        serverPolicyStub.setProcessedPolicies(processedPoliciesSoFar);

        if (existingGroupPolicy == null) {
            groupPolicy.onCreate(serverPolicyStub, configMap);
        }

        previousServerPolicy = serverPolicy;
        previousServerPolicyStub = (KernelObjectStub) serverPolicyStub;

        if (nextPoliciesToCreate.size() != 0) {
            createPolicy(
                    sapphireObjId,
                    spec,
                    null,
                    configMap,
                    nextPoliciesToCreate,
                    processedPolicies,
                    previousServerPolicy,
                    previousServerPolicyStub,
                    appArgs);
        }

        // server policy stub at this moment has the full policy chain; safe to add to group
        if (existingGroupPolicy == null) {
            groupPolicy.addServer(serverPolicyStub);
        }

        String ko = "";
        for (SapphirePolicyContainer policyContainer : processedPolicies) {
            ko += String.valueOf(policyContainer.getKernelOID()) + ",";
        }

        logger.log(Level.INFO, "OID from processed policies at " + policyName + " : " + ko);

        return processedPolicies;
    }

    /**
     * An internal helper method for sapphire object creation.
     *
     * <p>// TODO(multi-lang): Remove annotations from parameter list. // We pass in both
     * annotations and DMSpec temporarily to make // existing codes happy.
     *
     * @param spec
     * @param args
     * @param pc
     * @param annotations
     * @param configMap
     * @return
     * @throws Exception
     */
    private static Object newHelper_(
            SapphireObjectSpec spec,
            Object[] args,
            PolicyComponents pc,
            Annotation[] annotations,
            Map<String, SapphirePolicyConfig> configMap)
            throws Exception {
        /* Register for a sapphire object Id from OMS */
        SapphireObjectID sapphireObjId =
                GlobalKernelReferences.nodeServer.oms.registerSapphireObject();

        /* Create the Kernel Object for the Group Policy and get the Group Policy Stub from OMS */
        SapphireGroupPolicy groupPolicyStub =
                GlobalKernelReferences.nodeServer.oms.createGroupPolicy(
                        pc.groupPolicyClass, sapphireObjId, configMap);

        /* Register for a replica Id from OMS */
        SapphireReplicaID sapphireReplicaId =
                GlobalKernelReferences.nodeServer.oms.registerSapphireReplica(sapphireObjId);

        /* Create the Kernel Object for the Server Policy, and get the Server Policy Stub */
        final SapphireServerPolicy serverPolicyStub =
                (SapphireServerPolicy) getPolicyStub(pc.serverPolicyClass);

        /* Create the Client Policy Object */
        SapphireClientPolicy client = (SapphireClientPolicy) pc.clientPolicyClass.newInstance();

        /* Initialize the server policy and return a local pointer to the object itself */
        SapphireServerPolicy serverPolicy = initializeServerPolicy(serverPolicyStub);
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

        /* Create the App Object and return the App Stub */
        AppObjectStub appStub = getAppStub(spec, serverPolicy, args);

        /* Link everything together */
        client.setServer(serverPolicyStub);
        client.onCreate(groupPolicyStub, configMap);
        appStub.$__initialize(client);
        serverPolicy.onCreate(groupPolicyStub, configMap);
        groupPolicyStub.onCreate(serverPolicyStub, configMap);

        logger.info("Sapphire Object created: " + spec);
        return appStub;
    }

    private static PolicyComponents getPolicyComponents(Class<?> policy) {
        PolicyComponents pc = new PolicyComponents();
        Class<?>[] policyClasses = policy.getDeclaredClasses();
        for (Class<?> c : policyClasses) {
            if (SapphireServerPolicy.class.isAssignableFrom(c)) {
                pc.serverPolicyClass = c;
                continue;
            }
            if (SapphireClientPolicy.class.isAssignableFrom(c)) {
                pc.clientPolicyClass = c;
                continue;
            }
            if (SapphireGroupPolicy.class.isAssignableFrom(c)) {
                pc.groupPolicyClass = c;
                continue;
            }
        }

        return pc;
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
     * @return Returns group policy object stub
     * @throws RemoteException
     * @throws ClassNotFoundException
     * @throws KernelObjectNotCreatedException
     * @throws SapphireObjectNotFoundException
     */
    public static SapphireGroupPolicy createGroupPolicy(
            Class<?> policyClass,
            SapphireObjectID sapphireObjId,
            Map<String, SapphirePolicyConfig> configMap)
            throws RemoteException, ClassNotFoundException, KernelObjectNotCreatedException,
                    SapphireObjectNotFoundException {
        SapphireGroupPolicy groupPolicyStub = (SapphireGroupPolicy) getPolicyStub(policyClass);
        try {
            SapphireGroupPolicy groupPolicy = initializeGroupPolicy(groupPolicyStub);
            groupPolicyStub.setSapphireObjId(sapphireObjId);
            groupPolicy.setSapphireObjId(sapphireObjId);

            EventHandler sapphireHandler =
                    new EventHandler(
                            GlobalKernelReferences.nodeServer.getLocalHost(),
                            new ArrayList() {
                                {
                                    add(groupPolicyStub);
                                }
                            });

            /* Register the handler for the sapphire object */
            GlobalKernelReferences.nodeServer.oms.setSapphireObjectDispatcher(
                    sapphireObjId, sapphireHandler);
        } catch (KernelObjectNotFoundException e) {
            logger.severe(
                    "Failed to find the group kernel object created just before it. Exception info: "
                            + e);
            throw new KernelObjectNotCreatedException("Failed to find the kernel object", e);
        } catch (SapphireObjectNotFoundException e) {
            logger.warning("Failed to find sapphire object. Exception info: " + e);
            KernelObjectFactory.delete(groupPolicyStub.$__getKernelOID());
            throw e;
        }

        return groupPolicyStub;
    }

    /* Returns a pointer to the given Sapphire Object */
    // TODO: how to implement this ?
    public static Object this_(SapphireObject so) {

        AppObjectStub appObject = (AppObjectStub) so;
        return null;
    }

    // TODO: Not needed with the usage of annotations for multi policy chain declaration.
    // TODO: Can be removed.
    /* Returns the policy used by the Sapphire Object */
    private static Class<?> getPolicy(Type[] genericInterfaces) throws Exception {

        for (Type t : genericInterfaces) {
            if (t instanceof ParameterizedType) {
                ParameterizedType extInterfaceType = (ParameterizedType) t;
                Class<?> tClass = (Class<?>) extInterfaceType.getRawType();

                if (!tClass.getName().equals("sapphire.app.SapphireObject")) continue;

                Type[] tt = extInterfaceType.getActualTypeArguments();
                return (Class<?>) tt[0];
            } else if (!((Class<?>) t).getName().equals("sapphire.app.SapphireObject")) continue;
            else return DefaultSapphirePolicy.class;
        }
        // Shouldn't get here
        throw new Exception("The Object doesn't implement the SapphireObject interface.");
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

    /**
     * Returns a list of sapphire policy classes - one for each DM.
     *
     * @param dmList a list of DMs
     * @return a list of sapphire policy classes
     * @throws Exception
     */
    private static List<Class<?>> getPolicies(List<DMSpec> dmList) throws Exception {
        if (dmList == null || dmList.isEmpty()) {
            return Arrays.asList(DefaultSapphirePolicy.class);
        }

        List<Class<?>> policyClasses = new ArrayList<>();
        for (DMSpec dm : dmList) {
            Class<?> clazz = Class.forName(dm.getName());
            policyClasses.add(clazz);
        }

        return policyClasses;
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
            throws IllegalAccessException, CloneNotSupportedException {
        AppObjectStub appObjectStub = serverPolicy.$__initialize(spec, args);
        // TODO(multi-lang): We may need to create a clone for non-java app object stub.
        return spec.getLang() == Language.java ? extractAppStub(appObjectStub) : appObjectStub;
    }

    public static AppObjectStub extractAppStub(AppObjectStub appObject)
            throws CloneNotSupportedException, IllegalAccessException {
        // Return shallow copy of the kernel object
        AppObjectStub obj = (AppObjectStub) appObject.$__clone();

        // Replace all superclass fields with null
        Field[] fields = obj.getClass().getSuperclass().getFields();
        for (Field f : fields) {
            f.setAccessible(true);
            f.set(obj, null);
        }

        // Replace the values in stub with new values - is this necessary?

        // Update the directInvocation
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
     * @param serverPolicyStub server policy stub
     * @param prevServerPolicy previous server policy
     * @param prevServerPolicyStub previous server policy stub
     * @param clientPolicy client policy
     * @throws KernelObjectNotFoundException
     */
    private static void initServerPolicy(
            SapphireServerPolicy serverPolicy,
            SapphireServerPolicy serverPolicyStub,
            SapphireServerPolicy prevServerPolicy,
            KernelObjectStub prevServerPolicyStub,
            SapphireClientPolicy clientPolicy)
            throws KernelObjectNotFoundException {
        serverPolicyStub.$__initialize(prevServerPolicy.sapphire_getAppObject());
        serverPolicy.$__initialize(prevServerPolicy.sapphire_getAppObject());

        KernelObject previousServerPolicyKernelObject =
                GlobalKernelReferences.nodeServer.getKernelObject(
                        prevServerPolicyStub.$__getKernelOID());
        serverPolicy.setNextServerKernelObject(previousServerPolicyKernelObject);
        serverPolicy.setNextServerPolicy(prevServerPolicy);

        prevServerPolicy.setPreviousServerPolicy(serverPolicy);
        prevServerPolicyStub.$__setNextClientPolicy(clientPolicy);
    }

    /**
     * Initializes server policy and stub with app object.
     *
     * @param spec
     * @param serverPolicy server policy
     * @param serverPolicyStub server policy stub
     * @param clientPolicy client policy
     * @param appArgs app arguments
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws CloneNotSupportedException
     */
    // TODO (merge):
    private static void initAppStub(
            SapphireObjectSpec spec,
            SapphireServerPolicy serverPolicy,
            SapphireServerPolicy serverPolicyStub,
            SapphireClientPolicy clientPolicy,
            Object[] appArgs,
            AppObject appObject)
            throws ClassNotFoundException, IllegalAccessException, CloneNotSupportedException {
        AppObjectStub appStub;

        if (appObject != null) {
            serverPolicyStub.$__initialize(appObject);
            serverPolicy.$__initialize(appObject);
        } else {
            appStub = getAppStub(spec, serverPolicy, appArgs);
            appStub.$__initialize(clientPolicy);
            serverPolicy.$__initialize(appStub);
            serverPolicyStub.$__initialize(appStub);
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

    private static class PolicyComponents {
        private Class<?> clientPolicyClass = DefaultClientPolicy.class;
        private Class<?> groupPolicyClass = DefaultGroupPolicy.class;
        private Class<?> serverPolicyClass = DefaultServerPolicy.class;
    }
}

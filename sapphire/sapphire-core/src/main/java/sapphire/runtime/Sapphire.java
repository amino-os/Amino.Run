package sapphire.runtime;

import static sapphire.policy.SapphirePolicyUpcalls.SapphirePolicyConfig;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
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
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.DefaultSapphirePolicy.DefaultClientPolicy;
import sapphire.policy.DefaultSapphirePolicy.DefaultGroupPolicy;
import sapphire.policy.DefaultSapphirePolicy.DefaultServerPolicy;
import sapphire.policy.SapphirePolicy.SapphireClientPolicy;
import sapphire.policy.SapphirePolicy.SapphireGroupPolicy;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;
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
    public static Object new_(SapphireObjectSpec spec, Object... args) {
        AppObjectStub appStub = null;
        try {
            logger.info("Creating object for spec:" + spec);

            if (spec.getLang() == Language.java && spec.getDmList().isEmpty()) {
                Class<?> appObjectClass = Class.forName(spec.getJavaClassName());
                return new_(appObjectClass, args);
            } else {
                List<DMSpec> dmList = spec.getDmList();

                // Since multi-DM is not enabled, we have only one policy.
                List<Class<?>> policies = getPolicies(dmList);
                Class<?> policy = policies.get(0);

                PolicyComponents pc = getPolicyComponents(policy);

                Map<String, Map<String, SapphirePolicyConfig>> map =
                        Utils.fromDMSpecListToConfigMap(dmList);

                // Since multi-DM is not enabled. We only have one sapphire policy.
                Map<String, SapphirePolicyConfig> configMap = map.get(policy.getName());
                return newHelper_(spec, args, pc, null, configMap);
            }
        } catch (Exception e) {
            logger.log(
                    Level.SEVERE, String.format("Failed to create sapphire object '%s'", spec), e);
        }

        return appStub;
    }

    /**
     * WARN: This method only works for Java sapphire object. This method has been deprecated.
     * Please use {@link #new_(SapphireObjectSpec, Object...)}
     *
     * <p>We have Java demo apps that call this method directly to create sapphire objects. This
     * method has to stay until we have updated all demo apps.
     *
     * @param appObjectClass
     * @param args
     * @return The App Object Stub
     * @deprecated please use {@link #new_(SapphireObjectSpec, Object...)}
     */
    public static Object new_(Class<?> appObjectClass, Object... args) {
        try {
            Class<?> policy = getPolicy(appObjectClass.getGenericInterfaces());
            PolicyComponents pc = getPolicyComponents(policy);
            Annotation[] annotations = appObjectClass.getAnnotations();

            Map<String, SapphirePolicyUpcalls.SapphirePolicyConfig> configMap =
                    Utils.toSapphirePolicyConfig(annotations);

            // Some demo app will call this new_ directly.
            // Create a temporary spec so that we can reuse newHelper_.
            SapphireObjectSpec spec =
                    SapphireObjectSpec.newBuilder()
                            .setLang(Language.java)
                            .setJavaClassName(appObjectClass.getCanonicalName())
                            .create();
            return newHelper_(spec, args, pc, annotations, configMap);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create sapphire object:", e);
            return null;
        }
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
            groupPolicy.setAppConfigAnnotation(configMap);

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
    public static Object this_(SapphireObject<?> so) {

        AppObjectStub appObject = (AppObjectStub) so;
        return null;
    }

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
            throws Exception {
        AppObjectStub appObjectStub = serverPolicy.$__initialize(spec, args);
        return spec.getLang() == Language.java ? extractAppStub(appObjectStub) : appObjectStub;
    }

    public static AppObjectStub extractAppStub(AppObjectStub appObject) throws Exception {
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

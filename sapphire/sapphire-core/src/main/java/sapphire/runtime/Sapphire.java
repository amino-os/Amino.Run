package sapphire.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.apache.harmony.rmi.common.RMIUtil;
import sapphire.app.SapphireObject;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireReplicaID;
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

/**
 * Used by the developer to create a Sapphire Object given the Application Object class and the
 * Policy Object class.
 *
 * @author aaasz
 */
public class Sapphire {
    static Logger logger = Logger.getLogger(Sapphire.class.getName());

    /**
     * Creates a Sapphire Object: [App Object + App Object Stub + Kernel Object (Server Policy) +
     * Kernel Object Stub + Client Policy + Group Policy]
     *
     * @param appObjectClass
     * @param args
     * @return The App Object Stub
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws KernelObjectNotCreatedException
     */
    public static Object new_(Class<?> appObjectClass, Object... args) {
        try {

            /* Get the policy used by the Sapphire Object we need to create */
            Class<?> policy = getPolicy(appObjectClass.getGenericInterfaces());

            /* Extract the policy component classes (server, client and group) */
            Class<?>[] policyClasses = policy.getDeclaredClasses();

            Class<?> sapphireServerPolicyClass = null;
            Class<?> sapphireClientPolicyClass = null;
            Class<?> sapphireGroupPolicyClass = null;

            for (Class<?> c : policyClasses) {
                if (SapphireServerPolicy.class.isAssignableFrom(c)) {
                    sapphireServerPolicyClass = c;
                    continue;
                }
                if (SapphireClientPolicy.class.isAssignableFrom(c)) {
                    sapphireClientPolicyClass = c;
                    continue;
                }
                if (SapphireGroupPolicy.class.isAssignableFrom(c)) {
                    sapphireGroupPolicyClass = c;
                    continue;
                }
            }

            /* If no policies specified use the defaults */
            if (sapphireServerPolicyClass == null)
                sapphireServerPolicyClass = DefaultServerPolicy.class;
            if (sapphireClientPolicyClass == null)
                sapphireClientPolicyClass = DefaultClientPolicy.class;
            if (sapphireGroupPolicyClass == null)
                sapphireGroupPolicyClass = DefaultGroupPolicy.class;

            /* Register for a sapphire object Id from OMS */
            SapphireObjectID sapphireObjId =
                    GlobalKernelReferences.nodeServer.oms.registerSapphireObject();

            /* Create the Kernel Object for the Group Policy and get the Group Policy Stub from OMS */
            SapphireGroupPolicy groupPolicyStub =
                    GlobalKernelReferences.nodeServer.oms.createGroupPolicy(
                            sapphireGroupPolicyClass,
                            sapphireObjId,
                            appObjectClass.getAnnotations());

            /* Register for a replica Id from OMS */
            SapphireReplicaID sapphireReplicaId =
                    GlobalKernelReferences.nodeServer.oms.registerSapphireReplica(sapphireObjId);

            /* Create the Kernel Object for the Server Policy, and get the Server Policy Stub */
            final SapphireServerPolicy serverPolicyStub =
                    (SapphireServerPolicy) getPolicyStub(sapphireServerPolicyClass);

            /* Create the Client Policy Object */
            SapphireClientPolicy client =
                    (SapphireClientPolicy) sapphireClientPolicyClass.newInstance();

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
            AppObjectStub appStub = getAppStub(appObjectClass, serverPolicy, args);

            /* Link everything together */
            Annotation[] annotations = appObjectClass.getAnnotations();
            client.setServer(serverPolicyStub);
            client.onCreate(groupPolicyStub, annotations);
            appStub.$__initialize(client);
            serverPolicy.onCreate(groupPolicyStub, annotations);
            groupPolicyStub.onCreate(serverPolicyStub, annotations);

            logger.info("Sapphire Object created: " + appObjectClass.getName());
            return appStub;
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: Need to cleanup all the allocated resources
            return null;
            // throw new AppObjectNotCreatedException();
        }
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
            Class<?> policyClass, SapphireObjectID sapphireObjId, Annotation[] appConfigAnnotation)
            throws RemoteException, ClassNotFoundException, KernelObjectNotCreatedException,
                    SapphireObjectNotFoundException {
        SapphireGroupPolicy groupPolicyStub = (SapphireGroupPolicy) getPolicyStub(policyClass);
        try {
            SapphireGroupPolicy groupPolicy = initializeGroupPolicy(groupPolicyStub);
            groupPolicyStub.setSapphireObjId(sapphireObjId);
            groupPolicy.setSapphireObjId(sapphireObjId);
            groupPolicy.setAppConfigAnnotation(appConfigAnnotation);

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

    public static AppObjectStub getAppStub(
            Class<?> appObjectClass, SapphireServerPolicy serverPolicy, Object[] args)
            throws Exception {
        String appStubClassName =
                GlobalStubConstants.getAppPackageName(RMIUtil.getPackageName(appObjectClass))
                        + "."
                        + RMIUtil.getShortName(appObjectClass)
                        + GlobalStubConstants.STUB_SUFFIX;
        return extractAppStub(serverPolicy.$__initialize(Class.forName(appStubClassName), args));
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
}

package sapphire.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.apache.harmony.rmi.common.RMIUtil;
import sapphire.app.SapphireObject;
import sapphire.common.AppObject;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireReplicaID;
import sapphire.common.SapphireSoStub;
import sapphire.compiler.GlobalStubConstants;
import sapphire.kernel.common.DMConfigInfo;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectNotCreatedException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.common.SapphireObjectInfo;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.DefaultSapphirePolicy.DefaultClientPolicy;
import sapphire.policy.DefaultSapphirePolicy.DefaultGroupPolicy;
import sapphire.policy.DefaultSapphirePolicy.DefaultServerPolicy;
import sapphire.policy.SapphirePolicy;
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

    public static class DMPolicy {
        Annotation[] dmAnnotations;
        SapphireClientPolicy clientPolicy;
        SapphireServerPolicy serverPolicy;
        SapphireServerPolicy serverPolicyStub;
        SapphireGroupPolicy groupPolicyStub;

        DMPolicy(
                SapphireClientPolicy client,
                SapphireServerPolicy server,
                SapphireServerPolicy serverStub,
                SapphireGroupPolicy groupStub,
                Annotation[] annotations) {
            clientPolicy = client;
            serverPolicy = server;
            serverPolicyStub = serverStub;
            groupPolicyStub = groupStub;
            dmAnnotations = annotations;
        }
    }

    public static DMPolicy createDM(
            InetSocketAddress clientPolicyKernelServerHost,
            Class<?> sapphireClientPolicyClass,
            Class<?> sapphireServerPolicyClass,
            Class<?> sapphireGroupPolicyClass,
            SapphireReplicaID sapphireReplicaId,
            Annotation[] annotations)
            throws KernelObjectNotFoundException, KernelObjectNotCreatedException,
                    ClassNotFoundException, RemoteException, IllegalAccessException,
                    InstantiationException, SapphireObjectNotFoundException {

        /* Create the Kernel Object for the Group Policy and get the Group Policy Stub from OMS */
        SapphireGroupPolicy groupPolicyStub =
                GlobalKernelReferences.nodeServer.oms.createGroupPolicy(
                        sapphireGroupPolicyClass, sapphireReplicaId.getOID());
        groupPolicyStub.setSapphireObjId(sapphireReplicaId.getOID());

        /* Create the Kernel Object for the Server Policy, and get the Server Policy Stub */
        final SapphireServerPolicy serverPolicyStub =
                (SapphireServerPolicy) getPolicyStub(sapphireServerPolicyClass);

        /* Initialize the server policy and return a local pointer to the object itself */
        SapphireServerPolicy serverPolicy = initializeServerPolicy(serverPolicyStub);
        serverPolicyStub.setReplicaId(sapphireReplicaId);
        serverPolicy.setReplicaId(sapphireReplicaId);

        /* Create the Client Policy Object */
        SapphireClientPolicy client = null;

        /* Create instance of client object on the local kernel server and embed it in appobj stub
        only when client policy is going to reside in the same application process. Otherwise,
        client policy instance is not created here. It has to be created as and when app
        acquires/attach to SO stub(it is handled from OMS process directly)  */
        /* host address passed is local kernel host addr. Need not check it explicitly. Just null
        validation is enough */
        if (null != clientPolicyKernelServerHost) {
            client = (SapphireClientPolicy) sapphireClientPolicyClass.newInstance();
            /* Link everything together */
            client.setServer(serverPolicyStub);
            client.onCreate(groupPolicyStub, annotations);
        }

        return new DMPolicy(client, serverPolicy, serverPolicyStub, groupPolicyStub, annotations);
    }

    public static SapphireSoStub CreateAppObjectAndLinkToDM(
            String runtimeType,
            String clientPolicyName,
            String serverPolicyName,
            String groupPolicyName,
            SapphireReplicaID sapphireReplicaId,
            SapphireObjectID parentSapphireObjId,
            byte[] opaqueObject)
            throws ClassNotFoundException, RemoteException, SapphireObjectNotFoundException,
                    KernelObjectNotFoundException, IllegalAccessException, InstantiationException,
                    KernelObjectNotCreatedException {
        Annotation[] annotations =
                new Annotation[] {}; // TODO: This config should come from caller */

        if (clientPolicyName.isEmpty()) clientPolicyName = DefaultClientPolicy.class.getName();
        if (serverPolicyName.isEmpty()) serverPolicyName = DefaultServerPolicy.class.getName();
        if (groupPolicyName.isEmpty()) groupPolicyName = DefaultGroupPolicy.class.getName();

        DMPolicy dm =
                createDM(
                        null,
                        Class.forName(clientPolicyName),
                        Class.forName(serverPolicyName),
                        Class.forName(groupPolicyName),
                        sapphireReplicaId,
                        annotations);

        dm.serverPolicy.$__initialize(new AppObject(opaqueObject, runtimeType, sapphireReplicaId));
        dm.serverPolicy.onCreate(dm.groupPolicyStub, annotations);

        try {
            dm.groupPolicyStub.onCreate(dm.serverPolicyStub, annotations);
        } catch (Exception e) {
            // TODO: Need to add the cleanup to reverse the operations
            e.printStackTrace();
            return null;
        }

        final SapphirePolicy.SapphireServerPolicy serverStub = dm.serverPolicyStub;

        EventHandler replicaHandler =
                new EventHandler(
                        GlobalKernelReferences.nodeServer.getLocalHost(),
                        new ArrayList() {
                            {
                                add(serverStub);
                            }
                        });

        GlobalKernelReferences.nodeServer.oms.setSapphireReplicaDispatcher(
                sapphireReplicaId, replicaHandler);

        /* return the byte stream received from runtime */
        return new SapphireSoStub.SapphireSoStubBuilder()
                .setSapphireObjId(sapphireReplicaId.getOID())
                .setParentSapphireObjId(parentSapphireObjId)
                .setOpaqueObject(opaqueObject)
                .setdmAnnotations(annotations)
                .setClientPolicyName(clientPolicyName)
                .setServerPolicy(dm.serverPolicyStub)
                .setGroupPolicy(dm.groupPolicyStub)
                .create();
    }

    public static SapphireReplicaID createInnerSo(
            String className,
            String runtimeType,
            DMConfigInfo dmConfig,
            SapphireObjectID parentSapphireObjId,
            byte[] opaqueObject)
            throws RemoteException, SapphireObjectNotFoundException, ClassNotFoundException,
                    KernelObjectNotCreatedException, InstantiationException,
                    KernelObjectNotFoundException, IllegalAccessException {

        /* register sapphire object and a replica created on this kernel server */
        SapphireObjectID sapphireObjId =
                GlobalKernelReferences.nodeServer.oms.registerSapphireObject();
        SapphireReplicaID sapphireReplicaId =
                GlobalKernelReferences.nodeServer.oms.registerSapphireReplica(sapphireObjId);

        SapphireSoStub sapphireSoStub =
                CreateAppObjectAndLinkToDM(
                        runtimeType,
                        dmConfig.getClientPolicy(),
                        dmConfig.getServerPolicy(),
                        dmConfig.getGroupPolicy(),
                        sapphireReplicaId,
                        parentSapphireObjId,
                        opaqueObject);

        /* Set the inner SO to OMS */
        GlobalKernelReferences.nodeServer.oms.setInnerSapphireObject(sapphireSoStub);

        logger.info("Sapphire Object created: " + className);

        return sapphireReplicaId;
    }

    public static SapphireSoStub createSo(
            String className, String runtimeType, String constructorName, byte[] args)
            throws RemoteException, ClassNotFoundException, KernelObjectNotCreatedException,
                    KernelObjectNotFoundException, SapphireObjectNotFoundException,
                    InstantiationException, IllegalAccessException {

        /* register sapphire object and a replica created on this kernel server */
        SapphireObjectID sapphireObjId =
                GlobalKernelReferences.nodeServer.oms.registerSapphireObject();
        SapphireReplicaID sapphireReplicaId =
                GlobalKernelReferences.nodeServer.oms.registerSapphireReplica(sapphireObjId);

        byte[] opaqueObject = new byte[0];
        SapphireObjectID parentSapphireObjId = null;
        SapphireObjectInfo sapphireObjInfo = null;

        /* Create SO on runtime */
        if (runtimeType.equalsIgnoreCase("java")) {
            /* Call java runtime */
            sapphireObjInfo =
                    GlobalKernelReferences.nodeServer
                            .getGrpcClientToJavaRuntime()
                            .createSapphireReplica(
                                    className,
                                    sapphireReplicaId,
                                    parentSapphireObjId,
                                    opaqueObject,
                                    constructorName,
                                    args);
        } else if (runtimeType.equalsIgnoreCase("go")) {
            /* TODO: Call go runtime */
        }

        /* Create DM policy with info received from runtime */
        SapphireSoStub soStub =
                CreateAppObjectAndLinkToDM(
                        runtimeType,
                        sapphireObjInfo.getDmConfigInfo().getClientPolicy(),
                        sapphireObjInfo.getDmConfigInfo().getServerPolicy(),
                        sapphireObjInfo.getDmConfigInfo().getGroupPolicy(),
                        sapphireReplicaId,
                        parentSapphireObjId,
                        sapphireObjInfo.getOpaqueObject());

        logger.info("Sapphire Object created: " + className);
        return soStub;
    }

    /**
     * Creates a Sapphire Object: [App Object + App Object Stub + Kernel Object (Server Policy) +
     * Kernel Object Stub + Client Policy + Group Policy]
     *
     * @param appObjectClass
     * @param args
     * @param sapphirePolicyClass
     * @param policyArgs
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

            Annotation[] annotations = appObjectClass.getAnnotations();

            /* register sapphire object and a replica created on this kernel server */
            SapphireObjectID sapphireObjId =
                    GlobalKernelReferences.nodeServer.oms.registerSapphireObject();
            SapphireReplicaID sapphireReplicaId =
                    GlobalKernelReferences.nodeServer.oms.registerSapphireReplica(sapphireObjId);

            DMPolicy dm =
                    createDM(
                            GlobalKernelReferences.nodeServer.getLocalHost(),
                            sapphireClientPolicyClass,
                            sapphireServerPolicyClass,
                            sapphireGroupPolicyClass,
                            sapphireReplicaId,
                            annotations);

            final SapphirePolicy.SapphireServerPolicy serverStub = dm.serverPolicyStub;

            EventHandler replicaHandler =
                    new EventHandler(
                            GlobalKernelReferences.nodeServer.getLocalHost(),
                            new ArrayList() {
                                {
                                    add(serverStub);
                                }
                            });

            GlobalKernelReferences.nodeServer.oms.setSapphireReplicaDispatcher(
                    sapphireReplicaId, replicaHandler);

            /* Create the App Object and return the App Stub */
            AppObjectStub appStub = getAppStub(appObjectClass, dm.serverPolicy, args);
            dm.serverPolicy.onCreate(dm.groupPolicyStub, annotations);
            dm.groupPolicyStub.onCreate(dm.serverPolicyStub, annotations);

            appStub.$__initialize(dm.clientPolicy);

            logger.info("Sapphire Object created: " + appObjectClass.getName());
            return appStub;
        } catch (Exception e) {
            // TODO: Need to add the cleanup to reverse the operations
            e.printStackTrace();
            return null;
            // throw new AppObjectNotCreatedException();
        }
    }

    public static void delete_(SapphireObjectID sapphireObjId, EventHandler handler)
            throws RemoteException, SapphireObjectNotFoundException {
        deletePolicyObjects(handler);
        GlobalKernelReferences.nodeServer.oms.unRegisterSapphireObject(sapphireObjId);
    }

    public static void delete_(SapphireReplicaID sapphireReplicaId, EventHandler handler)
            throws RemoteException, SapphireObjectNotFoundException {
        deletePolicyObjects(handler);
        GlobalKernelReferences.nodeServer.oms.unRegisterSapphireReplica(sapphireReplicaId);
    }

    public static void deletePolicyObjects(EventHandler handler) throws RemoteException {
        List<Object> policies = handler.getObjects();
        KernelOID oid;

        for (Object policy : policies) {
            if (policy instanceof SapphirePolicy.SapphireServerPolicy) {
                oid = ((SapphirePolicy.SapphireServerPolicy) policy).$__getKernelOID();
            } else if (policy instanceof SapphirePolicy.SapphireGroupPolicy) {
                oid = ((SapphirePolicy.SapphireGroupPolicy) policy).$__getKernelOID();
            } else {
                logger.warning("unknown object instance");
                continue;
            }

            if (oid == null) {
                logger.warning("oid is null");
                continue;
            }

            try {
                GlobalKernelReferences.nodeServer.deleteKernelObject(oid);
            } catch (KernelObjectNotFoundException e) {
                e.printStackTrace();
            }
        }
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
            Class<?> appObjectClass, SapphireServerPolicy serverPolicy, Object[] args)
            throws Exception {
        String appStubClassName =
                GlobalStubConstants.getAppPackageName(RMIUtil.getPackageName(appObjectClass))
                        + "."
                        + RMIUtil.getShortName(appObjectClass)
                        + GlobalStubConstants.STUB_SUFFIX;
        return extractAppStub(serverPolicy.$__initialize(Class.forName(appStubClassName), args));
    }

    private static AppObjectStub extractAppStub(AppObjectStub appObject) throws Exception {
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

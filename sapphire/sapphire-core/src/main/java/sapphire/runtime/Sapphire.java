package sapphire.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.harmony.rmi.common.RMIUtil;
import sapphire.app.SapphireObject;
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
     * Creates a Sapphire Object: [App Object + App Object Stub + Kernel Object (Server Policy) +
     * Kernel Object Stub + Client Policy + Group Policy]
     *
     * @param appObjectClass
     * @param args
     * @return The App Object Stub
     * @throws Exception
     */
    public static Object new_(Class<?> appObjectClass, Object... args) {
        try {
            // Read annotation from this class.
            Annotation[] annotations = appObjectClass.getAnnotations();

            List<SapphirePolicyContainer> processedPolicies =
                    new ArrayList<SapphirePolicyContainer>();
            List<SapphirePolicyContainer> policyNameChain = getPolicyNameChain(annotations);

            if (policyNameChain.size() == 0) {
                // No annotations specifying policy name. Therefore, use default Sapphire policy.
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
                            appObjectClass,
                            null,
                            policyNameChain,
                            processedPolicies,
                            previousServerPolicy,
                            previousServerPolicyStub,
                            args);

            AppObjectStub appStub = policyList.get(0).getServerPolicy().sapphire_getAppObjectStub();
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

    /**
     * Creates app object stub along with instantiation of policies and stubs, and returns processed
     * policies for given policyNameChain.
     *
     * @param appObjectClass Name of the application object
     * @param appObject appObject
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
            Class<?> appObjectClass,
            AppObject appObject,
            List<SapphirePolicyContainer> policyNameChain,
            List<SapphirePolicyContainer> processedPolicies,
            SapphireServerPolicy previousServerPolicy,
            KernelObjectStub previousServerPolicyStub,
            Object[] appArgs)
            throws RemoteException, ClassNotFoundException, KernelObjectNotFoundException,
                    KernelObjectNotCreatedException, SapphireObjectNotFoundException,
                    SapphireObjectReplicaNotFoundException, InstantiationException,
                    IllegalAccessException, CloneNotSupportedException {

        if (policyNameChain == null || policyNameChain.size() == 0) return null;
        String policyName = policyNameChain.get(0).getPolicyName();
        SapphireGroupPolicy existingGroupPolicy = policyNameChain.get(0).getGroupPolicyStub();
        AppObjectStub appStub = null;

        /* Get the annotations added for the Application class. */
        Annotation[] annotations = appObjectClass.getAnnotations();

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
                            sapphireGroupPolicyClass,
                            sapphireObjId,
                            appObjectClass.getAnnotations());
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
        client.onCreate(groupPolicyStub, annotations);

        if (previousServerPolicy != null) {
            initServerPolicy(
                    serverPolicy,
                    serverPolicyStub,
                    previousServerPolicy,
                    previousServerPolicyStub,
                    client);
        } else {
            initAppStub(serverPolicy, serverPolicyStub, client, appObjectClass, appArgs, appObject);
        }

        // Note that subList is non serializable; hence, the new list creation.
        List<SapphirePolicyContainer> nextPoliciesToCreate =
                new ArrayList<SapphirePolicyContainer>(
                        policyNameChain.subList(1, policyNameChain.size()));

        serverPolicy.onCreate(groupPolicyStub, annotations);
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
            groupPolicy.onCreate(serverPolicyStub, annotations);
        }

        previousServerPolicy = serverPolicy;
        previousServerPolicyStub = (KernelObjectStub) serverPolicyStub;

        if (nextPoliciesToCreate.size() != 0) {
            createPolicy(
                    sapphireObjId,
                    appObjectClass,
                    null,
                    nextPoliciesToCreate,
                    processedPolicies,
                    previousServerPolicy,
                    previousServerPolicyStub,
                    appArgs);
        }

        String ko = "";
        for (SapphirePolicyContainer policyContainer : processedPolicies) {
            ko += String.valueOf(policyContainer.getKernelOID()) + ",";
        }

        logger.log(Level.INFO, "OID from processed policies at " + policyName + " : " + ko);

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
    public static Object this_(SapphireObject so) {

        AppObjectStub appObject = (AppObjectStub) so;
        return null;
    }

    // TODO: Not needed with the usage of annotations for multi policy chain declaration.
    // TODO: Can be removed.
    /* Returns the policy used by the Sapphire Object */
    /*
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
    */

    /* Returns the policy used by the Sapphire Object based on input class name */
    public static Class<?> getPolicy(String policyClassName) throws ClassNotFoundException {
        return Class.forName(policyClassName);
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
        Class<?> policy = getPolicy(policyName);

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

    public static AppObjectStub getAppStub(
            Class<?> appObjectClass, SapphireServerPolicy serverPolicy, Object[] args)
            throws CloneNotSupportedException, IllegalAccessException, ClassNotFoundException {
        String appStubClassName =
                GlobalStubConstants.getAppPackageName(RMIUtil.getPackageName(appObjectClass))
                        + "."
                        + RMIUtil.getShortName(appObjectClass)
                        + GlobalStubConstants.STUB_SUFFIX;
        return extractAppStub(serverPolicy.$__initialize(Class.forName(appStubClassName), args));
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
     * @param serverPolicy server policy
     * @param serverPolicyStub server policy stub
     * @param clientPolicy client policy
     * @param appObjectClass app object class to create a new app object stub
     * @param appArgs app arguments
     * @param appObject existing app object
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws CloneNotSupportedException
     */
    private static void initAppStub(
            SapphireServerPolicy serverPolicy,
            SapphireServerPolicy serverPolicyStub,
            SapphireClientPolicy clientPolicy,
            Class<?> appObjectClass,
            Object[] appArgs,
            AppObject appObject)
            throws ClassNotFoundException, IllegalAccessException, CloneNotSupportedException {
        AppObjectStub appStub;

        if (appObject != null) {
            serverPolicyStub.$__initialize(appObject);
            serverPolicy.$__initialize(appObject);
        } else {
            appStub = getAppStub(appObjectClass, serverPolicy, appArgs);
            appStub.$__initialize(clientPolicy);
            serverPolicy.$__initialize(appStub);
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

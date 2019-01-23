package amino.run.runtime;

import static amino.run.policy.Policy.ClientPolicy;
import static amino.run.policy.Policy.GroupPolicy;
import static amino.run.policy.Policy.ServerPolicy;

import amino.run.app.DMSpec;
import amino.run.app.Language;
import amino.run.app.SapphireObject;
import amino.run.app.SapphireObjectSpec;
import amino.run.common.AppObject;
import amino.run.common.AppObjectStub;
import amino.run.common.SapphireObjectCreationException;
import amino.run.common.SapphireObjectID;
import amino.run.common.SapphireObjectNotFoundException;
import amino.run.common.SapphireObjectReplicaNotFoundException;
import amino.run.common.SapphireReplicaID;
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
import amino.run.policy.SapphirePolicyContainer;
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
    public static Object new_(SapphireObjectSpec spec, Object... args)
            throws SapphireObjectCreationException {
        try {
            logger.info("Creating object for spec:" + spec);
            if (spec.getLang() == Language.java && spec.getDmList().isEmpty()) {
                Class<?> appObjectClass = Class.forName(spec.getJavaClassName());
                return new_(appObjectClass, args);
            }

            List<SapphirePolicyContainer> policyNameChain = getPolicyNameChain(spec);

            if (policyNameChain.size() == 0) {
                String defaultPolicyName = DefaultPolicy.class.getName();
                policyNameChain.add(new SapphirePolicyContainer(defaultPolicyName, null));
            }

            /* Get the region of current server */
            String region = GlobalKernelReferences.nodeServer.getRegion();
            return createPolicyChain(spec, policyNameChain, region, args);
        } catch (Exception e) {
            String msg = String.format("Failed to create sapphire object '%s'", spec);
            logger.log(Level.SEVERE, msg, e);
            throw new SapphireObjectCreationException(msg, e);
        }
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
            List<SapphirePolicyContainer> policyNameChain = getPolicyNameChain(annotations);

            if (policyNameChain.size() == 0) {
                String defaultPolicyName = DefaultPolicy.class.getName();
                policyNameChain.add(new SapphirePolicyContainer(defaultPolicyName, null));
            }

            SapphireObjectSpec spec =
                    SapphireObjectSpec.newBuilder()
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
     * Creates app object stub along with a policy chain associated to it.
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
     * @throws SapphireObjectNotFoundException
     * @throws SapphireObjectReplicaNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws CloneNotSupportedException
     */
    public static AppObjectStub createPolicyChain(
            SapphireObjectSpec spec,
            List<SapphirePolicyContainer> policyNameChain,
            String region,
            Object[] appArgs)
            throws IOException, ClassNotFoundException, KernelObjectNotFoundException,
                    KernelObjectNotCreatedException, SapphireObjectNotFoundException,
                    SapphireObjectReplicaNotFoundException, InstantiationException,
                    IllegalAccessException, CloneNotSupportedException {
        if (policyNameChain == null || policyNameChain.size() == 0) return null;
        AppObjectStub appStub = null;

        /* Register for a sapphire object Id from OMS */
        SapphireObjectID sapphireObjId =
                GlobalKernelReferences.nodeServer.oms.registerSapphireObject();

        ServerPolicy outerServerPolicy = null;
        KernelObjectStub outerServerPolicyStub = null;

        List<SapphirePolicyContainer> processedPolicies = new ArrayList<>();

        for (int i = 0; i < policyNameChain.size(); i++) {
            String policyName = policyNameChain.get(i).getPolicyName();

            /* Get the annotations added for the Application class. */

            /* Get the policy used by the Sapphire Object we need to create */
            HashMap<String, Class<?>> policyMap = getPolicyMap(policyName);
            Class<?> sapphireServerPolicyClass = policyMap.get("sapphireServerPolicyClass");
            Class<?> sapphireClientPolicyClass = policyMap.get("sapphireClientPolicyClass");
            Class<?> sapphireGroupPolicyClass = policyMap.get("sapphireGroupPolicyClass");

            /* Create and the Kernel Object for the Group Policy and get the Group Policy Stub.
            Note that group policy does not need to update hostname because it only applies to
            individual server in multi-policy scenario */
            Policy.GroupPolicy groupPolicyStub;
            /* Create the Kernel Object for the Group Policy and get the Group Policy Stub from OMS */
            groupPolicyStub =
                    GlobalKernelReferences.nodeServer.oms.createGroupPolicy(
                            sapphireGroupPolicyClass, sapphireObjId);

            /* Create the Kernel Object for the Server Policy, and get the Server Policy Stub */
            ServerPolicy serverPolicyStub = (ServerPolicy) getPolicyStub(sapphireServerPolicyClass);

            /* Create the Client Policy Object */
            ClientPolicy client = (ClientPolicy) sapphireClientPolicyClass.newInstance();

            /* Initialize the server policy and return a local pointer to the object itself */
            Policy.ServerPolicy serverPolicy = initializeServerPolicy(serverPolicyStub);

            registerSapphireReplica(sapphireObjId, serverPolicy, serverPolicyStub);

            /* Link everything together */
            client.setServer(serverPolicyStub);
            client.onCreate(groupPolicyStub, spec);

            /* Check and get the previous DM's container if available. So that, server side and client side chain links can
            be updated between the current DM and previous DM. If the previous DM's container is not available,
            then DM/Policy being created is either for single DM based SO or It is the first DM/Policy in Multi DM based SO
            (i.e., last mile server policy to SO */
            if (i == 0) {
                appStub = serverPolicy.$__initialize(spec, appArgs);
            } else {
                /* Previous server policy stub object acts as Sapphire Object(SO) to the current server policy */
                serverPolicy.$__initialize(
                        new AppObject(Utils.ObjectCloner.deepCopy(outerServerPolicyStub)));
                outerServerPolicy.setPreviousServerPolicy(serverPolicy);
                outerServerPolicyStub.$__setNextClientPolicy(client);
            }

            // Note that subList is non serializable; hence, the new list creation.
            // TODO: next policies won't be needed if sapphire_replicate() is updated to use
            // iterative logic.
            List<SapphirePolicyContainer> nextPoliciesToCreate =
                    new ArrayList(policyNameChain.subList(i + 1, policyNameChain.size()));
            serverPolicy.setNextPolicies(nextPoliciesToCreate);

            serverPolicy.onCreate(groupPolicyStub, spec);

            SapphirePolicyContainer processedPolicy =
                    new SapphirePolicyContainer(policyName, groupPolicyStub);
            processedPolicy.setServerPolicy(serverPolicy);
            processedPolicy.setServerPolicyStub((KernelObjectStub) serverPolicyStub);
            processedPolicies.add(processedPolicy);

            // Create a copy to set processed policies up to this point.
            List<SapphirePolicyContainer> processedPoliciesSoFar = new ArrayList(processedPolicies);
            serverPolicy.setProcessedPolicies(processedPoliciesSoFar);
            serverPolicyStub.setProcessedPolicies(processedPoliciesSoFar);

            outerServerPolicy = serverPolicy;
            outerServerPolicyStub = (KernelObjectStub) serverPolicyStub;

            if (i == 0) {
                appStub = (AppObjectStub) serverPolicy.sapphire_getAppObject().getObject();
                // TODO(multi-lang): We may need to create a clone for non-java app object stub.
                appStub = spec.getLang() == Language.java ? createClientAppStub(appStub) : appStub;
                appStub.$__initialize(client);
            }
        }

        /* Execute GroupPolicy.onCreate() in the chain from inner most instance */
        boolean alreadyPinned = false;
        for (int j = processedPolicies.size() - 1; j >= 0; j--) {
            GroupPolicy groupPolicyStub = processedPolicies.get(j).getGroupPolicyStub();
            ServerPolicy serverPolicyStub =
                    (ServerPolicy) processedPolicies.get(j).getServerPolicyStub();

            if (alreadyPinned) {
                serverPolicyStub.setAlreadyPinned(alreadyPinned);
            }

            if (serverPolicyStub.isAlreadyPinned()) {
                alreadyPinned = true;
            }
            groupPolicyStub.onCreate(region, serverPolicyStub, spec);
            groupPolicyStub.addServer(serverPolicyStub);
        }

        return appStub;
    }

    /**
     * Creates app object stub along with instantiation of policies and stubs, appends policies
     * processed for given policyNameChain to existing processed policy list and returns app object
     * stub to client.
     *
     * @param sapphireObjId Sapphire object Id
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
     * @throws SapphireObjectNotFoundException
     * @throws SapphireObjectReplicaNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws CloneNotSupportedException
     */
    public static AppObjectStub createPolicy(
            SapphireObjectID sapphireObjId,
            SapphireObjectSpec spec,
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
                            sapphireGroupPolicyClass, sapphireObjId);
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
            initAppStub(spec, serverPolicy, appArgs, appObject);
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
            createPolicy(
                    sapphireObjId, spec, nextPoliciesToCreate, processedPolicies, region, appArgs);
        }

        AppObjectStub appStub = null;

        // server policy stub at this moment has the full policy chain; safe to add to group
        if (existingGroupPolicy == null) {
            setIfAlreadyPinned(serverPolicyStub, processedPolicies, nextPoliciesToCreate.size());
            groupPolicyStub.onCreate(region, serverPolicyStub, spec);
            // TODO: Quinton: This looks like a bug.  Why is the server only added for the first
            // server?
            // OK - I think I've figured out why.  Because addServer is also invoked from
            // Library.sapphire_replicate
            // when additional replicas are created.
            // TODO: Remove calls to addServer from the DK.
            // addServer should only be called be called internally in the DM's.
            groupPolicyStub.addServer(serverPolicyStub);

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

        SapphireObjectID sapphireObjId = null;
        try {
            AppObjectStub appObjectStub = (AppObjectStub) stub;
            Field field =
                    appObjectStub
                            .getClass()
                            .getDeclaredField(GlobalStubConstants.APPSTUB_POLICY_CLIENT_FIELD_NAME);
            field.setAccessible(true);
            Policy.ClientPolicy clientPolicy = (Policy.ClientPolicy) field.get(appObjectStub);
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
    public static Policy.GroupPolicy createGroupPolicy(
            Class<?> policyClass, SapphireObjectID sapphireObjId)
            throws RemoteException, ClassNotFoundException, KernelObjectNotCreatedException,
                    SapphireObjectNotFoundException {
        Policy.GroupPolicy groupPolicyStub = (GroupPolicy) getPolicyStub(policyClass);
        try {
            GroupPolicy groupPolicy = initializeGroupPolicy(groupPolicyStub);
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
     * @param sapphireObjId Sapphire object ID
     * @param serverPolicy ServerPolicy
     * @param serverPolicyStub ServerPolicy stub
     * @throws SapphireObjectNotFoundException
     * @throws SapphireObjectReplicaNotFoundException
     * @throws RemoteException
     */
    private static void registerSapphireReplica(
            SapphireObjectID sapphireObjId,
            ServerPolicy serverPolicy,
            ServerPolicy serverPolicyStub)
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
            ServerPolicy serverPolicy,
            SapphirePolicyContainer prevContainer,
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
            SapphireObjectSpec spec,
            ServerPolicy serverPolicy,
            Object[] appArgs,
            AppObject appObject)
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

    /**
     * Checks the downstream policies by going through processedPolicies and sets the given
     * serverPolicyStub as being already pinned if any of downstream policies have set it as already
     * pinned. Note 'pinned' means the chain was migrated to a target Kernel Server by
     * sapphire_pin_to_server() at SapphirePolicyLibrary TODO: make pin_to_sapphire_server happen at
     * the last DM all the time.
     *
     * @param serverPolicyStub Stub to set as already pinned if downstream has already pinned.
     * @param processedPolicies All of the policies created in this policy chain.
     * @param sizeOfDownStreamPolicies number of policy instances that need to be checked if already
     *     pinned.
     */
    private static void setIfAlreadyPinned(
            ServerPolicy serverPolicyStub,
            List<SapphirePolicyContainer> processedPolicies,
            int sizeOfDownStreamPolicies) {
        int idx = 0, size = processedPolicies.size();

        // Indicates start of downstream policies.
        int startIdx = size - sizeOfDownStreamPolicies;
        try {
            // Set whether the chain was already pinned or not from downstream policies.
            for (SapphirePolicyContainer policyContainer : processedPolicies) {
                if (idx >= startIdx) {
                    // Start checking all the downstream policies only.
                    Policy.ServerPolicy sp = policyContainer.getServerPolicy();
                    if (sp.isAlreadyPinned()) {
                        String msg =
                                String.format(
                                        "Sapphire Object was already pinned by %s. Set as already pinned for %s",
                                        sp, serverPolicyStub);
                        logger.log(Level.INFO, msg);
                        serverPolicyStub.setAlreadyPinned(true);
                        return;
                    }
                }
                idx++;
            }
        } catch (Exception e) {
            logger.log(
                    Level.SEVERE,
                    String.format(
                            "Checking chain pinning failed. Size of processed policies = %d startIdx = %d",
                            size, startIdx));
            throw e;
        }
    }
}

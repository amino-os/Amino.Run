package sapphire.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.apache.harmony.rmi.common.RMIUtil;

import sapphire.app.SapphireObject;
import sapphire.common.AppObject;
import sapphire.common.AppObjectStub;
import sapphire.compiler.GlobalStubConstants;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectNotCreatedException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.server.KernelObject;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.DefaultSapphirePolicy.DefaultClientPolicy;
import sapphire.policy.DefaultSapphirePolicy.DefaultGroupPolicy;
import sapphire.policy.DefaultSapphirePolicy.DefaultServerPolicy;
import sapphire.policy.SapphirePolicy.SapphireClientPolicy;
import sapphire.policy.SapphirePolicy.SapphireGroupPolicy;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;
import sapphire.policy.SapphirePolicyContainer;
import sapphire.policy.SapphirePolicyContainerImpl;

/**
 * Used by the developer to create a Sapphire Object given
 * the Application Object class and the Policy Object class.
 * 
 * 
 * @author aaasz
 *
 */
public class Sapphire {
	static Logger logger = Logger.getLogger(Sapphire.class.getName());

	/**
	 * Creates a Sapphire Object:
	 *  [App Object + App Object Stub + Kernel Object (Server Policy) + Kernel Object Stub + Client Policy + Group Policy]
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
	public static Object new_(Class<?> appObjectClass, Object ... args) {
		try {
			// Read annotation from this class.
			Annotation[] annotations = appObjectClass.getAnnotations();
			List<SapphirePolicyContainer> DMchain = new ArrayList<SapphirePolicyContainer>();
			List<SapphirePolicyContainer> processedDMs = new ArrayList<SapphirePolicyContainer>();

			for (Annotation annotation : annotations) {
				if (annotation instanceof SapphireConfiguration) {
					String [] DMannotations = ((SapphireConfiguration) annotation).DMs();
					for (String DMannotation : DMannotations) {
						String [] DMs = DMannotation.split(",");
						for (String DM : DMs) {
							DMchain.add(new SapphirePolicyContainerImpl(DM.trim(), null));
						}
					}
				}
			}

			SapphireServerPolicy previousServerPolicy = null;
			KernelObjectStub previousServerPolicyStub = null;

			List<SapphirePolicyContainer> policyList = createPolicy(appObjectClass, null, DMchain, processedDMs, previousServerPolicy, previousServerPolicyStub, args);

			AppObjectStub appStub = policyList.get(0).getServerPolicy().sapphire_getAppObjectStub();
			logger.info("Sapphire Object created: " + appObjectClass.getName());
			return appStub;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
			//throw new AppObjectNotCreatedException();
		}
	}

	/**
	 * Creates app object stub along with instantiation of policies and stubs, and returns the app object stub for given DM chain.
	 * @param appObjectClass
	 * @param DMchain
	 * @param previousServerPolicy
	 * @param previousServerPolicyStub
	 * @param args
	 * @return AppObjectStub
	 * @throws Exception
	 */
	public static List<SapphirePolicyContainer> createPolicy(
			Class<?> appObjectClass,
			AppObject appObject,
			List<SapphirePolicyContainer> policyChain,
			List<SapphirePolicyContainer> processedPolicies,
			SapphireServerPolicy previousServerPolicy,
			KernelObjectStub previousServerPolicyStub,
			Object[] args) throws Exception {
		if (policyChain == null || policyChain.size() == 0) return null;
		String policyName = policyChain.get(0).getPolicyName();
		SapphireGroupPolicy existingGroupPolicy = policyChain.get(0).getGroupPolicy();
		AppObjectStub appStub = null;

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
			groupPolicyStub = (SapphireGroupPolicy) getPolicyStub(sapphireGroupPolicyClass);
		} else {
			groupPolicyStub = existingGroupPolicy;
		}

			/* Create the Kernel Object for the Server Policy, and get the Server Policy Stub */
		SapphireServerPolicy serverPolicyStub = (SapphireServerPolicy) getPolicyStub(sapphireServerPolicyClass);

			/* Create the Client Policy Object */
		SapphireClientPolicy client = (SapphireClientPolicy) sapphireClientPolicyClass.newInstance();

			/* Initialize the group policy and return a local pointer to the object itself */
		SapphireGroupPolicy groupPolicy = (existingGroupPolicy == null) ? initializeGroupPolicy(groupPolicyStub) : existingGroupPolicy;

			/* Initialize the server policy and return a local pointer to the object itself */
		SapphireServerPolicy serverPolicy = initializeServerPolicy(serverPolicyStub);

			/* Link everything together */
		client.setServer(serverPolicyStub);
		client.onCreate(groupPolicyStub);

		if (previousServerPolicy != null) {
			// non-first DMs references already created object.
			serverPolicyStub.$__initialize(previousServerPolicy.sapphire_getAppObject());
			serverPolicy.$__initialize(previousServerPolicy.sapphire_getAppObject());

			KernelObject previousServerPolicyKernelObject = (KernelObject)GlobalKernelReferences.nodeServer.getKernelObject(previousServerPolicyStub.$__getKernelOID());
			serverPolicy.setNextServerKernelObject(previousServerPolicyKernelObject);
			serverPolicy.setNextServerPolicy(previousServerPolicy);

			/* Sets the first server policy which will be remotely called by client side stub.
			This is needed for replication as it needs to copy the first server policy to other kernel server. */
			previousServerPolicy.setPreviousServerPolicy(serverPolicy);
			previousServerPolicyStub.$__setNextClientPolicy(client);
		} else {
			/* First DM needs to create an app stub.
			Note that only the last one in the server side needs to point to app object
			Other policies should not reference app object; otherwise, troubleshooting will be difficult. */

			// TODO: Change getAppStub to a different name; the following is different method from this one.
			if (appObject != null) {
				serverPolicyStub.$__initialize(appObject);
				serverPolicy.$__initialize(appObject);
			} else {
				appStub = getAppStub(appObjectClass, serverPolicy, args);
				appStub.$__initialize(client);
				serverPolicy.$__initialize(appStub);
			}
		}

		// Note that subList is non serializable; hence, the new list creation.
		List<SapphirePolicyContainer> nextPolicies = new ArrayList<SapphirePolicyContainer>(policyChain.subList(1, policyChain.size()));

		serverPolicy.onCreate(groupPolicyStub);
		serverPolicy.setNextDMs(nextPolicies);
		SapphirePolicyContainer newSapphirePolicyContainer = new SapphirePolicyContainerImpl(policyName, groupPolicy);
		serverPolicy.setThisPolicyContainer(newSapphirePolicyContainer);

		SapphirePolicyContainer processedPolicy = new SapphirePolicyContainerImpl(policyName, groupPolicy);
		processedPolicy.setServerPolicy(serverPolicy);
		processedPolicy.setServerPolicyStub((KernelObjectStub)serverPolicyStub);
		processedPolicies.add(processedPolicy);

		// Create a copy to set processed policies up to this point.
		List<SapphirePolicyContainer> processedPoliciesSoFar = new ArrayList<SapphirePolicyContainer>(processedPolicies);
		serverPolicy.setProcessedPolicies(processedPoliciesSoFar);
		serverPolicyStub.setProcessedPolicies(processedPoliciesSoFar);

		if (existingGroupPolicy == null) {
			groupPolicy.onCreate(serverPolicyStub);
		}

		previousServerPolicy = serverPolicy;
		previousServerPolicyStub = (KernelObjectStub)serverPolicyStub;

		if (nextPolicies.size() != 0) {
			createPolicy(appObjectClass, null, nextPolicies, processedPolicies, previousServerPolicy, previousServerPolicyStub, args);
		}

		return processedPolicies;
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
				ParameterizedType extInterfaceType = (ParameterizedType)t;
				Class<?> tClass = (Class<?>)extInterfaceType.getRawType();

				if (!tClass.getName().equals("sapphire.app.SapphireObject"))
					continue;

				Type[] tt = extInterfaceType.getActualTypeArguments();
				return (Class<?>) tt[0];
			}
			else if (!((Class<?>) t).getName().equals("sapphire.app.SapphireObject"))
				continue;
			else
				return DefaultSapphirePolicy.class;
		}

		// Shouldn't get here
		throw new Exception("The Object doesn't implement the SapphireObject interface.");
	}

	/* Returns the policy used by the Sapphire Object based on input class name */
	private static Class<?> getPolicy(String policyClassName) throws Exception {
		return Class.forName(policyClassName);
	}

	private static KernelObjectStub getPolicyStub(Class<?> policyClass)
			throws ClassNotFoundException, KernelObjectNotCreatedException {
		String policyStubClassName = GlobalStubConstants.getPolicyPackageName() + "." + RMIUtil.getShortName(policyClass) + GlobalStubConstants.STUB_SUFFIX;
		KernelObjectStub policyStub =  KernelObjectFactory.create(policyStubClassName);
		return policyStub;
	}

	private static SapphireGroupPolicy initializeGroupPolicy(SapphireGroupPolicy groupPolicyStub)
			throws KernelObjectNotFoundException {
		KernelOID groupOID = ((KernelObjectStub)groupPolicyStub).$__getKernelOID();
		SapphireGroupPolicy groupPolicy = (SapphireGroupPolicy) GlobalKernelReferences.nodeServer.getObject(groupOID);
		groupPolicy.$__setKernelOID(groupOID);
		return groupPolicy;
	}

	private static SapphireServerPolicy initializeServerPolicy(SapphireServerPolicy serverPolicyStub)
			throws KernelObjectNotFoundException {
		KernelOID serverOID = ((KernelObjectStub)serverPolicyStub).$__getKernelOID();
		SapphireServerPolicy serverPolicy = (SapphireServerPolicy) GlobalKernelReferences.nodeServer.getObject(serverOID);
		serverPolicy.$__setKernelOID(serverOID);
		return serverPolicy;
	}

	private static AppObjectStub getAppStub(Class<?> appObjectClass, SapphireServerPolicy serverPolicy, Object[] args)
			throws Exception {
		String appStubClassName = GlobalStubConstants.getAppPackageName(RMIUtil.getPackageName(appObjectClass)) + "." + RMIUtil.getShortName(appObjectClass) + GlobalStubConstants.STUB_SUFFIX;
		return extractAppStub(serverPolicy.$__initialize(Class.forName(appStubClassName), args));
	}

	private static AppObjectStub extractAppStub(AppObjectStub appObject) throws Exception {
		// Return shallow copy of the kernel object
		AppObjectStub obj = (AppObjectStub)appObject.$__clone();

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

		if (index == -1)
			return Class.forName(paramClassName);

		if (paramClassName.substring(index).equals(GlobalStubConstants.STUB_SUFFIX))
			/* TODO: Is it correct all times ? */
			paramClassName = param.getClass().getSuperclass().getName();
		//paramClassName = paramClassName.substring(0, index);

		return Class.forName(paramClassName);
	}

	/**
	 * Constructs a policy map for each client, server and group policy based on input policy name.
	 * @param policyName
	 * @return hash map for policies
	 * @throws Exception
	 */
	public static HashMap<String, Class<?>> getPolicyMap(String policyName) throws Exception {
		HashMap<String, Class<?>> policyMap = new HashMap<String, Class<?>>();
		Class<?> policy = getPolicy(policyName);

		/* Extract the policy component classes (server, client and group) */
		Class<?>[] policyClasses = policy.getDeclaredClasses();

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

	public static Class<?>[] getParamsClasses(Object[] params) throws ClassNotFoundException {
		ArrayList<Class<?>> argClassesList = new ArrayList<Class<?>>();
		for (Object param : params) {
			argClassesList.add(getParamClassStripStub(param));
		}
		Class<?>[] argClasses = new Class<?>[argClassesList.size()];
		return argClassesList.toArray(argClasses);
	}
}

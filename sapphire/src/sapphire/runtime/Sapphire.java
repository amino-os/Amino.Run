package sapphire.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.apache.harmony.rmi.common.RMIUtil;

import sapphire.app.SapphireObject;
import sapphire.common.AppObjectStub;
import sapphire.compiler.GlobalStubConstants;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectNotCreatedException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.DefaultSapphirePolicy.DefaultClientPolicy;
import sapphire.policy.DefaultSapphirePolicy.DefaultGroupPolicy;
import sapphire.policy.DefaultSapphirePolicy.DefaultServerPolicy;
import sapphire.policy.SapphirePolicy.SapphireClientPolicy;
import sapphire.policy.SapphirePolicy.SapphireGroupPolicy;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;

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

			/* Get the policy used by the Sapphire Object we need to create */
			Class<?> policy = getPolicy(appObjectClass.getGenericInterfaces());

			/* Extract the policy component classes (server, client and group) */
			Class<?> [] policyClasses = policy.getDeclaredClasses();

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

			/* Create and the Kernel Object for the Group Policy and get the Group Policy Stub */
			SapphireGroupPolicy groupPolicyStub = (SapphireGroupPolicy) getPolicyStub(sapphireGroupPolicyClass);

			/* Create the Kernel Object for the Server Policy, and get the Server Policy Stub */
			SapphireServerPolicy serverPolicyStub = (SapphireServerPolicy) getPolicyStub(sapphireServerPolicyClass);

			/* Create the Client Policy Object */
			SapphireClientPolicy client = (SapphireClientPolicy) sapphireClientPolicyClass.newInstance();

			/* Initialize the group policy and return a local pointer to the object itself */
			SapphireGroupPolicy groupPolicy = initializeGroupPolicy(groupPolicyStub);

			/* Initialize the server policy and return a local pointer to the object itself */
			SapphireServerPolicy serverPolicy = initializeServerPolicy(serverPolicyStub);

			/* Create the App Object and return the App Stub */
			AppObjectStub appStub = getAppStub(appObjectClass, serverPolicy, args);

			/* Link everything together */
			client.setServer(serverPolicyStub);
			client.onCreate(groupPolicyStub);
			appStub.$__initialize(client);
			serverPolicy.onCreate(groupPolicyStub);
			groupPolicy.onCreate(serverPolicyStub);

			logger.info("Sapphire Object created: " + appObjectClass.getName());
			return appStub;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
			//throw new AppObjectNotCreatedException();
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

	public static Class<?>[] getParamsClasses(Object[] params) throws ClassNotFoundException {
		ArrayList<Class<?>> argClassesList = new ArrayList<Class<?>>();
		for (Object param : params) {
			argClassesList.add(getParamClassStripStub(param));
		}
		Class<?>[] argClasses = new Class<?>[argClassesList.size()];
		return argClassesList.toArray(argClasses);
	}
}

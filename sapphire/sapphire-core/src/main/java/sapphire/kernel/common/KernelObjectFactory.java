package sapphire.kernel.common;

import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KernelObjectFactory {
	static Logger logger = Logger.getLogger(KernelObjectFactory.class.getName());

	public static KernelObjectStub create(String stubClassName) throws ClassNotFoundException, KernelObjectNotCreatedException {
		Class<?> stubClass = Class.forName(stubClassName);
		Class<?> kernelClass = stubClass.getSuperclass();
		
		KernelOID oid = GlobalKernelReferences.nodeServer.newKernelObject(kernelClass);
		return createStubWithOid(stubClassName, oid, GlobalKernelReferences.nodeServer.getLocalHost());
	}

	/**
	 * Create a policy stub with the specified oid
	 * @author Venugopal Reddy K 00900280 on 27/02/18
	 * @param stubClassName policy stub class name
	 * @param oid policy kernel oid
	 * @return policy stub
	 * @throws ClassNotFoundException
	 * @throws KernelObjectNotCreatedException
	 */
	public static KernelObjectStub createStubWithOid(String stubClassName, KernelOID oid, InetSocketAddress hostName) throws ClassNotFoundException, KernelObjectNotCreatedException {
		Class<?> stubClass = Class.forName(stubClassName);

		try {
			Constructor<?> cons = stubClass.getConstructor(Class.forName("sapphire.kernel.common.KernelOID"));
			KernelObjectStub stub = (KernelObjectStub) cons.newInstance(oid);
			logger.fine("Created Kernel Object Stub: " + stub);

			/* Host name is passed as null when kernel object(oid) is on remote host. So need to do global lookup from OMS */
			if (null == hostName) {
				hostName = GlobalKernelReferences.nodeServer.oms.lookupKernelObject(oid);
			}

			stub.$__updateHostname(hostName);
			return stub;
		}
		catch (Exception e) {
			logger.severe("Could not instantiate stub: " + e.getMessage());
			throw new KernelObjectNotCreatedException();
		}
	}
}

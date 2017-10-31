package sapphire.kernel.common;

import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KernelObjectFactory {

	public static KernelObjectStub create(String stubClassName) throws ClassNotFoundException, KernelObjectNotCreatedException {
		Logger logger = Logger.getLogger("sapphire.kernel.common.KernelObjectFactory");
		Class<?> stubClass = Class.forName(stubClassName);
		Class<?> kernelClass = stubClass.getSuperclass();
		
		KernelOID oid = GlobalKernelReferences.nodeServer.newKernelObject(kernelClass);
		try {
			Constructor<?> cons = stubClass.getConstructor(Class.forName("sapphire.kernel.common.KernelOID"));
			KernelObjectStub stub = (KernelObjectStub) cons.newInstance(oid);
			logger.fine("Created Kernel Object Stub: " + stub);
			stub.$__updateHostname(GlobalKernelReferences.nodeServer.getLocalHost());
			return stub;
		}
		catch (Exception e) {
			logger.severe("Could not instantiate stub: " + e.getMessage());
			throw new KernelObjectNotCreatedException();
		}
	}
}

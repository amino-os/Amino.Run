package sapphire.kernel.common;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.logging.Logger;

public class KernelObjectFactory {
    static Logger logger = Logger.getLogger(KernelObjectFactory.class.getName());

    /**
     * Create policy object and policy stub object for the given stub class name
     *
     * @param stubClassName
     * @return Returns policy stub object
     * @throws ClassNotFoundException
     * @throws KernelObjectNotCreatedException
     */
    public static KernelObjectStub create(String stubClassName)
            throws ClassNotFoundException, KernelObjectNotCreatedException {
        Class<?> stubClass = Class.forName(stubClassName);
        Class<?> kernelClass = stubClass.getSuperclass();

        KernelOID oid = GlobalKernelReferences.nodeServer.newKernelObject(kernelClass);
        try {
            return createStub(stubClass, oid);
        } catch (Exception e) {
            logger.severe("Could not instantiate stub: " + e.getMessage());
            throw new KernelObjectNotCreatedException();
        }
    }

    /**
     * Create policy stub object for the given policy stub class and kernel oid
     *
     * @param stubClass
     * @param oid
     * @return Returns policy stub object
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    public static KernelObjectStub createStub(Class<?> stubClass, KernelOID oid)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException, InstantiationException {
        Constructor<?> cons =
                stubClass.getConstructor(Class.forName("sapphire.kernel.common.KernelOID"));
        KernelObjectStub stub = (KernelObjectStub) cons.newInstance(oid);
        logger.fine("Created Kernel Object Stub: " + stub);
        stub.$__updateHostname(GlobalKernelReferences.nodeServer.getLocalHost());
        return stub;
    }

    /**
     * Delete kernel object with oid
     *
     * @param oid
     */
    public static void delete(KernelOID oid) {
        try {
            GlobalKernelReferences.nodeServer.deleteKernelObject(oid);
        } catch (RemoteException e) {
            throw new Error("Could not delete kernel object", e);
        } catch (KernelObjectNotFoundException e) {
            throw new Error("Could not find kernel object");
        }
    }
}

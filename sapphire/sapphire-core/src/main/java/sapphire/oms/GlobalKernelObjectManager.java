package sapphire.oms;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectNotFoundException;

/**
 * Tracks all kernel objects in this application.
 * @author iyzhang
 *
 */

public class GlobalKernelObjectManager {
    private ConcurrentHashMap<KernelOID,InetSocketAddress> kernelObjects;
    private Random oidGenerator;

    /**
     * Randomly generate a new kernel object id
     * @return
     */
    private KernelOID generateKernelOID() {
        return new KernelOID(oidGenerator.nextInt());
    }

    public GlobalKernelObjectManager() {
        kernelObjects = new ConcurrentHashMap<KernelOID,InetSocketAddress>();
        oidGenerator = new Random(new Date().getTime());
    }

    /**
     * Register a new kernel object
     * @param host
     * @return
     */
    public KernelOID register(InetSocketAddress host) {
        KernelOID oid = generateKernelOID();
        kernelObjects.put(oid, host);
        return oid;
    }
    
    /**
     * Move a kernel object by registering a new host for this object
     * @param oid
     * @param host
     * @throws KernelObjectNotFoundException
     */
    public void register(KernelOID oid, InetSocketAddress host) throws KernelObjectNotFoundException {
    	if (lookup(oid) != null) {
        	kernelObjects.put(oid, host);    		
    	}
    }
    
    /**
     * Find the host for a kernel object 
     * @param oid
     * @return
     * @throws KernelObjectNotFoundException
     */
    public InetSocketAddress lookup(KernelOID oid) throws KernelObjectNotFoundException {
    	InetSocketAddress host = kernelObjects.get(oid);
    	if (host == null) {
    		throw new KernelObjectNotFoundException("Not a valid kernel object id.");
    	}
    	return host;
    }
}

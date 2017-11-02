package sapphire.kernel.server;

import java.lang.reflect.Constructor;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectNotCreatedException;
import sapphire.kernel.common.KernelObjectNotFoundException;

/** 
 * Manages kernel objects in the Sapphire server. 
 * Keeps references for all local kernel objects
 * 
 * @author iyzhang
 *
 */
public class KernelObjectManager {
	Logger logger = Logger.getLogger("sapphire.kernel.server.KernelObjectManager");
	private ConcurrentHashMap<KernelOID, KernelObject> objects;
	
	public KernelObjectManager() {
		objects = new ConcurrentHashMap<KernelOID, KernelObject>();
	}
	
	/** 
	 * Check whether we have this kernel object. If so, return the kernel object that you can use to invoke the RPC.
	 * 
	 * @param oid
	 * @return the kernel object that can be used to run the actual RPC.
	 * @throws KernelObjectNotFoundException if we do not have the object
	 */
	public KernelObject lookupObject(KernelOID oid) throws KernelObjectNotFoundException {
		KernelObject object = objects.get(oid);
		if (object == null) {
			throw new KernelObjectNotFoundException("Could not find kernel object with id: "+oid.toString()+" on local host.");
		}
		return object;
	}
	
	/**
	 * Add a new kernel object to this server. Used to move objects to the local server.
	 * @param oid
	 * @param object
	 */
	public void addObject(KernelOID oid, KernelObject object) {
		objects.put(oid, object);
	}
	
	public KernelObject removeObject(KernelOID oid) throws KernelObjectNotFoundException {
		KernelObject object = objects.remove(oid);
		if (object == null) {
			throw new KernelObjectNotFoundException("Could not find kernel object with id: "+oid.toString()+"on local host to remove.");
		}
		return object;
	}
	
	/**
	 * Create a new kernel object locally.
	 * @param stub
	 */
	public void newObject(KernelOID oid, Class<?> cl, Object ... args) throws KernelObjectNotCreatedException {
		// Grab the right constructor
		Class<?>[] params = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			params[i] = args[i].getClass();
		}
		
		// Construct the kernel object
		Constructor<?> cons = null;
		Object obj = null;
		try {
			cons = cl.getConstructor(params);
		} catch (NoSuchMethodException e) {
			logger.severe("Cannot find the appropriate constructor to instantiate class: " + cl.getName());
			throw new KernelObjectNotCreatedException("Cannot find the appropriate constructor: "+e);
			
		}
		
		try {
			obj = cons.newInstance(args);
		} catch (Exception e) {
			logger.severe("Could not instantiate kernel object of type: " + cl);
			throw new KernelObjectNotCreatedException("Could not instantiate kernel object of type: "+cl);
		}

		// create new kernelobject
		KernelObject kernelObject = new KernelObject(obj);
		addObject(oid, kernelObject);
	}
}

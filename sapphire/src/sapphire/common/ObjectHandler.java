package sapphire.common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;

/** 
 * An object handler contains the actual object and pointers to its methods. It basically invokes the method, identified
 * by a String, on the contained object.
 * @author iyzhang
 *
 */

public class ObjectHandler implements Serializable {
	/** Reference to the actual object instance */
	private Serializable object;
	static private Logger logger = Logger.getLogger(ObjectHandler.class.getName());

	/** Table of strings of method names and function pointers to the actual methods
	 * For invoking RPCs on the object. Constructed at construction time to reduce
	 * overhead from reflection.
	 */
	private Hashtable<String, Method> methods;

	protected Class<?> getClass(Object obj) {
		return obj.getClass();
	}

	private void fillMethodTable(Object obj) {
		Class<?> cl = getClass(obj);
		this.methods = new Hashtable<String, Method>();
		// Grab the methods of the class
		Method[] methods = cl.getMethods();
		for (int i = 0; i < methods.length; i++) {
			if (this.methods.get(methods[i].toGenericString()) == null)
				this.methods.put(methods[i].toGenericString(), methods[i]);
		}
	}
	/**
	 * At creation time, we create the actual object, which happens to be the superclass of the stub.
	 * We also inspect the methods of the object to set up a table we can use to look up the method on RPC.
	 * 
	 * @param stub
	 */	
	public ObjectHandler(Object obj) {
		// TODO: get all the methods from all superclasses - careful about duplicates
		object = (Serializable) obj;
		fillMethodTable(obj);
		logger.fine("Created object " + obj.toString());
	}

	/**
	 * Invoke method on the object using the params
	 * 
	 * @param method
	 * @param params
	 * @return the return value from the method
	 */
	public Object invoke(String method, ArrayList<Object> params) throws Exception {
		return methods.get(method).invoke(object, params.toArray());
	}

	public Serializable getObject() {
		return object;
	}

	public void setObject(Serializable object) {
		this.object = object;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(object);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		Object obj = in.readObject();
		fillMethodTable(obj);
		this.object = (Serializable) obj;
	}
}

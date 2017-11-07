package sapphire.runtime;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import sapphire.common.ObjectHandler;

public class EventHandler extends ObjectHandler {

	private class PolicyHandler implements Serializable {
		private Object policyObject;
		private Method handler;
		
		public PolicyHandler(Object policyObject, Method handler) {
			this.policyObject = policyObject;
			this.handler = handler;
		}

		public Object invokeHandler(ArrayList<Object> params) throws Exception {
			return handler.invoke(policyObject, params.toArray());
		}
	}
	
	private ArrayList<Object> objects;
	private Hashtable<String, PolicyHandler> handlers;
	
	private void fillMethodTable() {
		this.handlers = new Hashtable<String, PolicyHandler>();
		Iterator<Object> it = objects.iterator();
		Object obj = it.next();
		while(it.hasNext()) {
			Class<?> cl = obj.getClass();
			// Grab the methods of the class
			Method[] methods = cl.getDeclaredMethods();
			for (int i = 0; i < methods.length; i++) {
				PolicyHandler handler = new PolicyHandler(obj, methods[i]);
				this.handlers.put(methods[i].toGenericString(), handler);
			}
		}
	}

	public EventHandler(ArrayList<Object> policies) {
		super(null);
		fillMethodTable();		
	}

	/**
	 * Invoke method on the object using the params
	 * 
	 * @param method
	 * @param params
	 * @return the return value from the method
	 */
	public Object invoke(String method, ArrayList<Object> params) throws Exception {
		if (handlers.contains(method)) {
			return handlers.get(method).invokeHandler(params);
		} else {
			return null;
		}
	}
	
	public Boolean hasHandler(String method) {
		return handlers.contains(method);
	}
}

package sapphire.runtime;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * Event handler holds the group policy stub or server policy stub object of the sapphire object or
 * sapphire replica respectively created on the particular kernel server.
 */
public class EventHandler implements Serializable {

    private class PolicyHandler {
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

    private InetSocketAddress host;
    private ArrayList<Object> objects;
    private Hashtable<String, PolicyHandler> handlers;

    private void fillMethodTable() {
        handlers = new Hashtable<String, PolicyHandler>();
        Iterator<Object> it = objects.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            Class<?> cl = obj.getClass();
            // Grab the methods of the class
            Method[] methods = cl.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                PolicyHandler handler = new PolicyHandler(obj, methods[i]);
                handlers.put(methods[i].toGenericString(), handler);
            }
        }
    }

    public EventHandler(InetSocketAddress hostAddr, ArrayList<Object> policies) {
        host = hostAddr;
        objects = policies;
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
        if (handlers.containsKey(method)) {
            return handlers.get(method).invokeHandler(params);
        } else {
            return null;
        }
    }

    public InetSocketAddress getHost() {
        return host;
    }

    public ArrayList<Object> getObjects() {
        return objects;
    }

    public Boolean hasHandler(String method) {
        return handlers.containsKey(method);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(getHost());
        out.writeObject(getObjects());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        host = (InetSocketAddress) in.readObject();
        objects = (ArrayList<Object>) in.readObject();
        fillMethodTable();
    }
}

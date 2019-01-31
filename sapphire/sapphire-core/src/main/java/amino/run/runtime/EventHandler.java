package amino.run.runtime;

import amino.run.runtime.exception.EventNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Event handler holds the group policy stub or server policy stub object of the sapphire object or
 * sapphire replica respectively created on the particular kernel server.
 */
public class EventHandler implements Serializable {
    static Logger logger = Logger.getLogger(EventHandler.class.getCanonicalName());

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
    private HashMap<String, String> eventMapper;

    private void fillMethodTable() {
        handlers = new Hashtable<String, PolicyHandler>();
        eventMapper = new HashMap<String, String>();
        Iterator<Object> it = objects.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            Class<?> cl = obj.getClass();
            // Grab the methods of the class
            Method[] methods = cl.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                try {
                    if (methods[i].isAnnotationPresent(AddEvent.class)) {
                        PolicyHandler handler = new PolicyHandler(obj, methods[i]);
                        String methodGenName = methods[i].toGenericString();
                        handlers.put(methodGenName, handler);
                        eventMapper.put(
                                canonicalEventName(
                                        cl, methods[i].getAnnotation(AddEvent.class).event()),
                                methodGenName);
                    }
                } catch (Exception e) {
                    System.out.println(e.toString());
                }
            }
        }
    }

    private String canonicalEventName(Class c, String event) {
        return c.getName() + "_" + event;
    }

    public EventHandler(InetSocketAddress hostAddr, ArrayList<Object> policies) {
        host = hostAddr;
        objects = policies;
        fillMethodTable();
    }

    private Object invoke(String method, ArrayList<Object> params) throws Exception {
        return handlers.get(method).invokeHandler(params);
    }

    /**
     * Invoke method on the object using the params
     *
     * @param event
     * @param params
     * @return the return value from the method
     */
    public Object notifyEvent(Class c, String event, ArrayList<Object> params) throws Exception {
        String canonicalEventName = canonicalEventName(c, event);
        if (!eventMapper.containsKey(canonicalEventName)) {
            throw new EventNotFoundException(
                    String.format("Event %s not available in event handler", event));
        }
        return invoke(eventMapper.get(canonicalEventName), params);
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

package amino.run.common;

import amino.run.graal.io.*;
import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;
import org.graalvm.polyglot.*;

/**
 * An object handler contains the actual object and pointers to its methods. It basically invokes
 * the method, identified by a String, on the contained object.
 *
 * @author iyzhang
 */
public class ObjectHandler implements Serializable {
    /** Reference to the actual object instance */
    private Object object;

    public enum ObjectType {
        graal,
        java,
    }

    private static Logger logger = Logger.getLogger(ObjectHandler.class.getName());

    /**
     * Table of strings of method names and function pointers to the actual methods For invoking
     * RPCs on the object. Constructed at construction time to reduce overhead from reflection.
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

    public boolean isGraalObject() {
        return (object instanceof GraalObject);
    }

    public boolean isGraalObject(Object object) {
        return (object instanceof GraalObject);
    }
    /**
     * At creation time, we create the actual object, which happens to be the superclass of the
     * stub. We also inspect the methods of the object to set up a table we can use to look up the
     * method on RPC.
     *
     * @param obj
     */
    public ObjectHandler(Object obj) {
        // TODO: get all the methods from all superclasses - careful about duplicates
        object = obj;

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

        Object[] p = params.toArray();
        if (isGraalObject()) {
            ArrayList<Object> inParams = new ArrayList<Object>();
            for (Object o : params) {
                if (o instanceof Value) {
                    inParams.add(o);
                } else {
                    inParams.add(
                            ((GraalObject) object).deserializedSerializeValue((SerializeValue) o));
                }
            }

            // Note in graal microservice stub we use varargs (Object ...) as function
            // parameters, so
            // we need to wrap parameters with another object array.
            // Please refer to unit test
            // sapphire-core/src/test/java/amino/run/common/VarargsFunctionReflectionTest
            p = new Object[] {inParams.toArray()};
        }
        Method m = methods.get(method);

        if (m == null) {
            throw new Exception(
                    String.format("Could not find method %s, methods are %s", method, methods));
        }

        return m.invoke(object, p);
    }

    public Serializable getObject() {
        return (Serializable) object;
    }

    public void setObject(Serializable object) {
        this.object = object;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        if (isGraalObject()) {
            out.writeUTF(ObjectType.graal.toString());
            // TODO: make language configurable.
            out.writeUTF(Boolean.toString(((GraalAppObjectStub) object).$__directInvocation()));
            ((GraalObject) object).writeObject(out);
        } else {
            out.writeUTF(ObjectType.java.toString());
            out.writeObject(object);
        }
    }

    /**
     * Write the object to a stream.
     *
     * @param out
     * @throws IOException Note - it's not possible to simply make writeObject public, as java
     *     serialization requires it to be private.
     */
    public void write(ObjectOutputStream out) throws IOException {
        this.writeObject(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        if (ObjectType.valueOf(in.readUTF()) == ObjectType.graal) {
            boolean directInvocation = Boolean.valueOf(in.readUTF());
            GraalObject object = (GraalObject) GraalObject.readObject(in);
            Class<?> appObjectStubClass = Class.forName(object.getJavaClassName());
            // Construct the list of classes of the arguments as Class[]
            // TODO: Currently all polyglot application stub should have default
            // constructor. Fix it
            Object appStubObject;
            try {
                appStubObject = appObjectStubClass.newInstance();
            } catch (IllegalAccessException e) {
                throw new ClassNotFoundException("IllegalAccessException  " + e.getMessage());
            } catch (InstantiationException e) {
                throw new ClassNotFoundException("InstantiationException  " + e.getMessage());
            }

            ((GraalObject) appStubObject).$__initializeGraal(object);
            ((GraalAppObjectStub) appStubObject).$__initialize(directInvocation);
            fillMethodTable(appStubObject);
            this.object = appStubObject;
        } else {
            Object obj = in.readObject();
            fillMethodTable(obj);
            this.object = obj;
        }
    }

    public void read(ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.readObject(in);
    }
}

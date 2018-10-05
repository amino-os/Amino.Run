package sapphire.common;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;
//import org.graalvm.polyglot.*;
import sapphire.app.Language;
import sapphire.graal.io.*;

/**
 * An object handler contains the actual object and pointers to its methods. It basically invokes
 * the method, identified by a String, on the contained object.
 *
 * @author iyzhang
 */
public class ObjectHandler implements Serializable {
    /** Reference to the actual object instance */
    protected Object object;

    private Language lang;

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

    private boolean IsGraalObject() {
        return lang != Language.java;
    }

    /**
     * At creation time, we create the actual object, which happens to be the superclass of the
     * stub. We also optionally inspect the methods of the object to set up a table we can use to look up the
     * method on RPC.
     *
     * @param stub
     * @param fillMethodTable false to suppress filling the method table
     */
    public ObjectHandler(Object obj, boolean fillMethodTable) {
        // TODO: get all the methods from all superclasses - careful about duplicates
        object = obj;
        if (fillMethodTable) {
            this.fillMethodTable(obj);
        }
        logger.fine("Created object " + obj.toString());
    }

    /**
     * Constructor, with method table filling enabled
     * @param obj
     */
    public ObjectHandler(Object obj) {
        this(obj, true);
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
        return (Serializable) object;
    }

    public void setObject(Serializable object) {
        this.object = object;
    }

    protected void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(object);
    }

    /**
     * Write the object to a stream.
     * Note - it's not possible to simply make writeObject public, as java
     *     serialization requires it to be private.
     * @param out
     * @throws IOException
     */
    public void write(ObjectOutputStream out) throws IOException {
        this.writeObject(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        lang = Language.valueOf(in.readUTF());
        if (IsGraalObject()) {
            sapphire.graal.io.Deserializer deserializer =
                    new Deserializer(in, GraalContext.getContext());
            object = deserializer.deserialize();
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

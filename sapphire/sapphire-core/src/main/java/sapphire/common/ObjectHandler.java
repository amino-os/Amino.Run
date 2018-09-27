package sapphire.common;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;
import org.graalvm.polyglot.*;
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
    private Object object;

    private Language lang;
    // TODO: Context cannot be SerDe to another machine, we should recreate context.
    private Context c;

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

    public boolean IsGraalObject() {
        return lang != Language.java;
    }

    public void SetGraalContext(Context c) {
        this.c = c;
    }
    /**
     * At creation time, we create the actual object, which happens to be the superclass of the
     * stub. We also inspect the methods of the object to set up a table we can use to look up the
     * method on RPC.
     *
     * @param stub
     */
    public ObjectHandler(Object obj) {
        // TODO: get all the methods from all superclasses - careful about duplicates
        object = obj;

        // TODO: we should support other languages than js, when object is created we can track it's
        // language.
        if (org.graalvm.polyglot.Value.class.isAssignableFrom(obj.getClass())) {
            lang = Language.js;
        } else {
            lang = Language.java;
        }

        if (!IsGraalObject()) {
            fillMethodTable(obj);
        }
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
        if (IsGraalObject()) {
            Value v = (Value) object;
            ArrayList<Object> objs = new ArrayList<>();
            for (Object p : params) {
                ByteArrayInputStream in = new ByteArrayInputStream((byte[]) p);
                sapphire.graal.io.Deserializer deserializer = new Deserializer(in, c);

                // TODO: we should get language accordingly. We know language we create this graal
                // object, we can save it.
                deserializer.setLanguage(sapphire.app.Language.js);
                objs.add(deserializer.deserialize());
            }
            return v.getMember(method).execute(objs.toArray());
        } else {
            return methods.get(method).invoke(object, params.toArray());
        }
    }

    public Serializable getObject() {
        return (Serializable) object;
    }

    public Value getGraalObject() {
        return (Value) object;
    }

    public void setObject(Serializable object) {
        this.object = object;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(lang.toString());
        if (IsGraalObject()) {
            // TODO: make language configurable.
            sapphire.graal.io.Serializer serializer = new Serializer(out, lang);
            serializer.serialize((Value) this.object);
        } else {
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
        lang = Language.valueOf(in.readUTF());
        if (IsGraalObject()) {
            sapphire.graal.io.Deserializer deserializer = new Deserializer(in, c);
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

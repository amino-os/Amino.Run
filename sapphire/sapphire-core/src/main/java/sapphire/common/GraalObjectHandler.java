package sapphire.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;

import sapphire.app.Language;
import sapphire.graal.io.Deserializer;
import sapphire.graal.io.GraalContext;
import sapphire.graal.io.Serializer;
import org.graalvm.polyglot.*;

/**
 * An object handler for Graal objects.
 *
 * @author quinton
 */
public class GraalObjectHandler extends ObjectHandler implements Serializable {
    private Language lang; // What language is the object written in?
    private static Logger logger = Logger.getLogger(GraalObjectHandler.class.getName());

    /**
     * See superclass constructor for description.
     *
     * @param stub  TODO: quinton: Is this the stub or the actual object.  Reconcile javadoc with code
     */
    public GraalObjectHandler(Object obj) {
        super(obj, false); // Don't fill the method table
        // TODO: we should support other languages than js, when object is created we can track it's
        // language.
        // TODO: quinton: The logic here looks questionable.  Pretty sure that java is also assignable.
        if (org.graalvm.polyglot.Value.class.isAssignableFrom(obj.getClass())) {
            lang = Language.js;
        } else {
            lang = Language.java;
        }
        /*
            TODO: Why do we handle java objects differently?  I think this is a bug?
            Presumably we should do this for all Graal objects, even if they're java?
         */
        if (lang != Language.java) {
            fillMethodTable(obj);
        }
    }

    /**
     * Invoke method on the object using the params
     *
     * @param method
     * @param params
     * @return the return value from the method
     */
    public Object invoke(String method, ArrayList<Object> params) throws Exception {
        /*
            TODO: Why do we handle java objects differently?  I think this is a bug?
            Presumably we should do this for all Graal objects, even if they're java?
         */
        if (lang != Language.java) {
            Value v = (Value) object;
            ArrayList<Object> objs = new ArrayList<>();
            for (Object p : params) {
                ByteArrayInputStream in = new ByteArrayInputStream((byte[]) p);
                Deserializer deserializer =
                        new Deserializer(in, GraalContext.getContext());
                objs.add(deserializer.deserialize());
            }
            return v.getMember(method).execute(objs.toArray());
        } else { // TODO: See above - presumably we should never do this here for Graal
            return methods.get(method).invoke(object, params.toArray());
        }
    }

    /**
     * TODO: quinton: Should be able to just do this cast in the caller instead, and delete this method.
     * @return
     */
    public Value getGraalObject() {
        return (Value) object;
    }

    @Override
    /**
     * Use the graal serializer to write out the object.
     * TODO: quinton - we really can't afford to create a new serializer for every invocation like this.
     */
    protected void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(lang.toString());
        // TODO: make language configurable.
        Serializer serializer = new Serializer(out, lang);
        serializer.serialize((Value) this.object);
    }

    @Override
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        lang = Language.valueOf(in.readUTF());
        Deserializer deserializer = new Deserializer(in, GraalContext.getContext());
        object = deserializer.deserialize();
    }
}

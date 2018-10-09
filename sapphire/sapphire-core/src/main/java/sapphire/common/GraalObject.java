package sapphire.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import sapphire.app.Language;
import sapphire.app.SapphireObjectSpec;
import sapphire.graal.io.Deserializer;
import sapphire.graal.io.Serializer;

/** Created by AmitRoushan on 9/29/18. Wrapper of graal object for polyglot Sapphire object */
// TODO: Need to update how to serialize Graal Object
public class GraalObject implements Serializable {
    private Language lang;
    private transient Context context;
    private transient Value value;
    private String sourceLocation;
    private String constructor;

    private GraalObject() {}

    private void setSourceLocation(String sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    private void setConstructor(String constructor) {
        this.constructor = constructor;
    }

    private void setContext(Context context) {
        this.context = context;
    }

    private void setLang(String lang) {
        for (Language l : Language.values()) {
            if (l.toString().equals(lang)) {
                this.lang = l;
            }
        }
    }

    private void setValue(Value value) {
        this.value = value;
    }

    public GraalObject(SapphireObjectSpec spec, Object[] params) throws IOException {
        lang = spec.getLang();
        sourceLocation = spec.getSourceFileLocation();
        constructor = spec.getConstructorName();

        context = Context.newBuilder(lang.toString()).allowAllAccess(true).build();
        context.eval(
                org.graalvm.polyglot.Source.newBuilder(
                                lang.toString(), new java.io.File(sourceLocation))
                        .build());

        // create class instance with default constructor
        if (params == null || params.length == 0) {
            value = context.eval(lang.toString(), constructor).newInstance();
            return;
        }
        value = context.eval(lang.toString(), constructor).newInstance(params);
    }

    public synchronized Object invoke(String method, ArrayList<Object> params) throws Exception {
        ArrayList<Object> objs = new ArrayList<>();
        for (Object p : params) {
            ByteArrayInputStream in = new ByteArrayInputStream((byte[]) p);
            sapphire.graal.io.Deserializer deserializer = new Deserializer(in, context);
            objs.add(deserializer.deserialize());
        }
        return value.getMember(method).execute(objs.toArray());
    }

    public void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(sourceLocation);
        out.writeUTF(constructor);
        out.writeUTF(lang.toString());
        sapphire.graal.io.Serializer serializer = new Serializer(out, lang);
        serializer.serialize(value);
    }

    public static Object readObject(ObjectInputStream in) throws IOException {

        String sourceLocation = in.readUTF();
        String constructor = in.readUTF();
        String lang = in.readUTF();

        GraalObject graalObject = new GraalObject();
        Context context = Context.newBuilder(lang).allowAllAccess(true).build();
        context.eval(
                org.graalvm.polyglot.Source.newBuilder(lang, new java.io.File(sourceLocation))
                        .build());

        graalObject.setSourceLocation(sourceLocation);
        graalObject.setConstructor(constructor);
        graalObject.setLang(lang);
        graalObject.setContext(context);

        sapphire.graal.io.Deserializer deserializer = new Deserializer(in, context);
        Value value = deserializer.deserialize();
        graalObject.setValue(value);

        return graalObject;
    }

    @Override
    public String toString() {
        return sourceLocation + constructor + lang.toString();
    }
}

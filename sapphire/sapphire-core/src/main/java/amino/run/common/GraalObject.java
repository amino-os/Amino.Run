package amino.run.common;

import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.graal.io.Deserializer;
import amino.run.graal.io.SerializeValue;
import amino.run.graal.io.Serializer;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/** Created by AmitRoushan on 9/29/18. Wrapper of graal object for polyglot MicroService object */
// TODO: Need to update how to serialize Graal Object
public class GraalObject implements Serializable {
    private static final long serialVersionUID = 6529685098267757690L;
    private Language lang;
    private transient Context context;
    private transient Value value;
    private String sourceLocation;
    private String constructor;
    private String javaStubClassName;

    public GraalObject(Object... args) {}

    public GraalObject() {}

    private void setSourceLocation(String sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    private void setConstructor(String constructor) {
        this.constructor = constructor;
    }

    private void setContext(Context context) {
        this.context = context;
    }

    private void setLang(Language lang) {
        this.lang = lang;
    }

    private void setValue(Value value) {
        this.value = value;
    }

    public void setJavaClassName(String javaStubClassName) {
        this.javaStubClassName = javaStubClassName;
    }

    public Context getContext() {
        return context;
    }

    public Language getLanguage() {
        return lang;
    }

    public Value getValue() {
        return value;
    }

    public String getJavaClassName() {
        return javaStubClassName;
    }

    public String getConstructor() {
        return constructor;
    }

    public String getSourceFileLocation() {
        return sourceLocation;
    }

    public static GraalObject.Builder newBuilder() {
        return new GraalObject.Builder();
    }

    public void $__initializeGraal(MicroServiceSpec spec, Object[] params) throws IOException {
        lang = spec.getLang();
        sourceLocation = spec.getSourceFileLocation();
        constructor = spec.getConstructorName();
        javaStubClassName = spec.getJavaClassName();
        if (javaStubClassName.isEmpty() || !javaStubClassName.equals(this.getClass().getName())) {
            throw new RuntimeException("java stub class name not provided");
        }

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

    // only create context in graal initialize. Only used in client side
    public void $__initializeGraal(MicroServiceSpec spec) throws IOException {
        lang = spec.getLang();
        sourceLocation = spec.getSourceFileLocation();
        constructor = spec.getConstructorName();
        javaStubClassName = spec.getJavaClassName();

        context = Context.newBuilder(lang.toString()).allowAllAccess(true).build();
        context.eval(
                org.graalvm.polyglot.Source.newBuilder(
                                lang.toString(), new java.io.File(sourceLocation))
                        .build());
    }

    public void $__initializeGraal(GraalObject object) {
        lang = object.getLanguage();
        sourceLocation = object.getSourceFileLocation();
        constructor = object.getConstructor();
        javaStubClassName = object.getJavaClassName();
        context = object.getContext();
        value = object.getValue();
    }

    public Object invoke(String method, ArrayList<Object> params) throws Exception {
        Value object = value.getMember(method).execute(params.toArray());
        return SerializeValue.getSerializeValue(object, lang);
    }

    public Value deserializedSerializeValue(SerializeValue val) throws Exception {
        Value value = SerializeValue.getDeserializedValue(val, context);
        return value;
    }

    public void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(sourceLocation);
        out.writeUTF(constructor);
        out.writeUTF(lang.toString());
        out.writeUTF(javaStubClassName);

        try {
            amino.run.graal.io.Serializer serializer = new Serializer(out, lang);
            serializer.serialize(value);
        } catch (IOException e) {
            System.out.println(e.toString());
            throw e;
        }
    }

    public static Object readObject(ObjectInputStream in) throws IOException {
        String sourceLocation = in.readUTF();
        String constructor = in.readUTF();
        String lang = in.readUTF();
        String javaStubClassName = in.readUTF();

        GraalObject graalObject =
                GraalObject.newBuilder()
                        .setLang(Language.valueOf(lang))
                        .setConstructor(constructor)
                        .setJavaClassName(javaStubClassName)
                        .setSourceLocation(sourceLocation)
                        .create();

        amino.run.graal.io.Deserializer deserializer =
                new Deserializer(in, graalObject.getContext());
        Value value = deserializer.deserialize();
        graalObject.setValue(value);

        return graalObject;
    }

    @Override
    public String toString() {
        return sourceLocation + constructor + lang.toString() + javaStubClassName;
    }

    public static class Builder {
        private Language lang;
        private String sourceLocation;
        private String constructor;
        private String javaClassName;

        public Builder setLang(Language lang) {
            this.lang = lang;
            return this;
        }

        public Builder setSourceLocation(String sourceLocation) {
            this.sourceLocation = sourceLocation;
            return this;
        }

        public Builder setConstructor(String constructor) {
            this.constructor = constructor;
            return this;
        }

        public Builder setJavaClassName(String javaClassName) {
            this.javaClassName = javaClassName;
            return this;
        }

        public GraalObject create() throws IOException {
            GraalObject graalObject = new GraalObject();

            Context context = Context.newBuilder(lang.toString()).allowAllAccess(true).build();
            context.eval(
                    org.graalvm.polyglot.Source.newBuilder(
                                    lang.toString(), new java.io.File(sourceLocation))
                            .build());

            graalObject.setSourceLocation(sourceLocation);
            graalObject.setConstructor(constructor);
            graalObject.setJavaClassName(javaClassName);
            graalObject.setLang(lang);
            graalObject.setContext(context);
            return graalObject;
        }
    }
}

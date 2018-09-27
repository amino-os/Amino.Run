package sapphire.graal.io;

import java.io.*;
import java.util.*;
import org.graalvm.polyglot.*;
import sapphire.app.Language;

// DUPLICATE represents duplicate objects that has been serialized once.
enum GraalType {
    BOOLEAN,
    NULL,
    NUMBER,
    STRING,
    ARRAY,
    OBJECT,
    DUPLICATE;
}

public class Serializer implements AutoCloseable {

    // TODO: should do identity better than toString
    private Map<String, Integer> seenCache;
    private int seenInd;
    private DataOutputStream out;
    private Language lang;

    public Serializer(OutputStream os, Language language) {
        out = new DataOutputStream(os);
        lang = language;
    }

    public void serialize(Value v) throws IOException {
        System.out.println("******************start serialize: " + v);
        seenInd = 0;
        seenCache = new HashMap<String, Integer>();
        out.writeUTF(lang.toString());
        serializeHelper(v);
    }

    // TODO narrow exception
    private void serializeHelper(Value v) throws IOException {
        // if (v.canExecute()) return;

        // check if value cached
        if (seenCache.keySet().contains(v.toString())) {
            out.writeInt(GraalType.DUPLICATE.ordinal());
            out.writeInt(seenCache.get(v.toString()));
            System.out.println("found in cache: " + v.toString());
            return;
        } else if (!v.isBoolean() && !v.isNull() && !v.isNumber()) {
            System.out.println("new value to cache: " + v.toString());
            seenCache.put(v.toString(), seenInd);
            // out.writeInt(seenInd);
            seenInd++;
        }

        if (v.isBoolean()) {
            System.out.println("found boolean: " + v);
            out.writeInt(GraalType.BOOLEAN.ordinal());
            out.writeBoolean(v.asBoolean());
        } else if (v.isNativePointer()) {
            throw new IllegalArgumentException("native pointer not supported");
        } else if (v.isNull()) {
            System.out.println("found null");
            out.writeInt(GraalType.NULL.ordinal());
        } else if (v.isNumber()) {
            System.out.println("found number (assuming int): " + v);
            out.writeInt(GraalType.NUMBER.ordinal());
            out.writeInt(v.asInt());
        } else if (v.isString()) {
            System.out.println("found string: " + v.asString());
            out.writeInt(GraalType.STRING.ordinal());
            out.writeUTF(v.asString());
        } else if (v.hasArrayElements()) {
            System.out.println("has array of length " + v.getArraySize());
            out.writeInt(GraalType.ARRAY.ordinal());
            out.writeLong(v.getArraySize());
            for (long i = 0; i < v.getArraySize(); i++) {
                System.out.println("writing array element " + i);
                serializeHelper(v.getArrayElement(i));
            }
        } else { // isComplex
            System.out.println("found non-primitive, " + " canExecute " + v.canExecute() + " " + v);
            out.writeInt(GraalType.OBJECT.ordinal());
            // String className = v.getMetaObject().getMember("className").asString();
            String className = getClassName(v);
            System.out.println("^^^^class name is " + className);
            out.writeUTF(className);

            // check named members
            if (v.hasMembers()) {
                // List<String> keys = new ArrayList<>(v.getMemberKeys());
                List<String> keys = getMemberVariables(v);
                System.out.println("found members: " + keys);
                // System.out.println("writing " + keys.size() + " members");
                for (String k : keys) {
                    if (k.equals("__proto__")) { // TODO hard coded for js
                        continue;
                    }
                    System.out.println("key: " + k + " value: " + v.getMember(k));
                    serializeHelper(getInstanceVariable(v, k));
                }
            }
        }
    }

    private Value getInstanceVariable(Value v, String s) {
        switch (lang) {
            case ruby:
                s = s.replaceAll(":", "");
                return v.getMember("instance_variable_get").execute(s);
            case js:
                return v.getMember(s);
        }
        return null;
    }

    private String getClassName(Value v) {
        switch (lang) {
            case ruby:
                return v.getMetaObject().getMember("name").execute().asString();
            case js:
                return v.getMetaObject().getMember("className").asString();
        }
        return "INVALID_LANG";
    }

    private List<String> getMemberVariables(Value v) {
        switch (lang) {
            case ruby:
                Value instVars = v.getMember("instance_variables").execute();
                List<String> varSte = new ArrayList<String>();
                for (int i = 0; i < instVars.getArraySize(); i++) {
                    varSte.add(instVars.getArrayElement(i).toString());
                }
                return varSte;
            case js:
                return new ArrayList<>(v.getMemberKeys());
        }
        return new ArrayList<>();
    }

    public void close() throws IOException {
        if (out != null) {
            out.close();
            out = null;
        }
    }
}

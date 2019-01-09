package amino.run.graal.io;

import amino.run.app.Language;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import org.graalvm.polyglot.*;

// DUPLICATE represents duplicate objects that has been serialized once.
enum GraalType {
    BOOLEAN,
    NULL,
    NUMBER,
    STRING,
    ARRAY,
    OBJECT,
    DUPLICATE
}

public class Serializer implements AutoCloseable {
    Logger logger = Logger.getLogger(Serializer.class.getName());

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
        seenInd = 0;
        seenCache = new HashMap<String, Integer>();
        out.writeUTF(lang.toString());
        serializeHelper(v);
    }

    // TODO narrow exception
    private void serializeHelper(Value v) throws IOException {
        // if (v.canExecute()) return;

        // check if value cached
        /*if (seenCache.keySet().contains(v.toString())) {
            out.writeInt(GraalType.DUPLICATE.ordinal());
            out.writeInt(seenCache.get(v.toString()));
            logger.fine("found in cache: " + v.toString());
            return;
        } else if (!v.isBoolean() && !v.isNull() && !v.isNumber()) {
            logger.fine("new value to cache: " + v.toString());
            seenCache.put(v.toString(), seenInd);
            // out.writeInt(seenInd);
            seenInd++;
        }*/

        if (v.isBoolean()) {
            logger.fine("found boolean: " + v);
            out.writeInt(GraalType.BOOLEAN.ordinal());
            out.writeBoolean(v.asBoolean());
        } else if (v.isNativePointer()) {
            throw new IllegalArgumentException("native pointer not supported");
        } else if (v.isNull()) {
            logger.fine("found null");
            out.writeInt(GraalType.NULL.ordinal());
        } else if (v.isNumber()) {
            logger.fine("found number (assuming int): " + v);
            out.writeInt(GraalType.NUMBER.ordinal());
            out.writeInt(v.asInt());
        } else if (v.isString()) {
            logger.fine("found string: " + v);
            out.writeInt(GraalType.STRING.ordinal());
            out.writeUTF(v.asString());
        } else if (v.hasArrayElements()) {
            logger.fine("has array of length " + v.getArraySize());
            out.writeInt(GraalType.ARRAY.ordinal());
            out.writeLong(v.getArraySize());
            for (long i = 0; i < v.getArraySize(); i++) {
                logger.fine("writing array element " + i);
                serializeHelper(v.getArrayElement(i));
            }
        } else { // isComplex
            logger.fine("found non-primitive, " + " canExecute " + v.canExecute());
            out.writeInt(GraalType.OBJECT.ordinal());
            // String className = v.getMetaObject().getMember("className").asString();
            String className = getClassName(v);
            out.writeUTF(className);
            // check named members
            if (v.hasMembers()) {
                // List<String> keys = new ArrayList<>(v.getMemberKeys());
                List<String> keys = getMemberVariables(v);
                logger.fine("found members: " + keys);
                // logger.fine("writing " + keys.size() + " members");
                for (String k : keys) {
                    if (k.equals("__proto__") || v.getMember(k).canExecute()) {
                        continue;
                    }
                    logger.fine("key: " + k + " value: " + v.getMember(k));
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
                return v.getMetaObject().toString();
            case js:
                try {
                    return v.getMetaObject().getMember("className").asString();
                } catch (Exception e) {
                    // Although sapphire object is in js, the parameters could be in any language,
                    // for example the key value store, client could pass anything as value.
                    // In this case, we assume the class name is this. (currently looks like java
                    // and ruby
                    // client would work.)
                    return v.getMetaObject().toString();
                }
        }
        return "INVALID_LANG";
    }

    private List<String> getMemberVariables(Value v) {
        switch (lang) {
            case ruby:
                Value instVars = v.getMember("instance_variables").execute();
                List<String> varSte = new ArrayList<String>();
                for (int i = 0; i < instVars.getArraySize(); i++) {
                    varSte.add(instVars.getArrayElement(i).toString().replaceAll(":", ""));
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

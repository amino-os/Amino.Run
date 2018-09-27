package sapphire.graal.io;

import java.io.*;
import java.util.*;
import org.graalvm.polyglot.*;
import sapphire.app.Language;

public class Deserializer implements AutoCloseable {

    private DataInputStream in;
    private Context c;
    public Map<Integer, Value> seenCache = new HashMap<Integer, Value>();
    private Language lang;

    public Deserializer(InputStream in, Context c) throws IOException {
        this.in = new DataInputStream(in);

        if (c == null) this.c = Context.create();
        else this.c = c;
    }

    // If client does not specify language during serialization, we should set the language here.
    public void setLanguage(Language language) {
        lang = language;
    }

    public Value deserialize() throws IOException {
        seenCache = new HashMap<Integer, Value>();
        Language language = Language.valueOf(in.readUTF());
        if (language != Language.unspecified) lang = language;
        System.out.println(String.format("lang is %s", lang));
        return deserializeHelper();
    }

    public Value deserializeHelper() throws IOException {
        Value out = null;

        GraalType type = GraalType.values()[in.readInt()];
        System.out.println(String.format("type is %s", type));

        switch (type) {
            case BOOLEAN:
                // System.out.println("found boolean");
                boolean b = in.readBoolean();
                out = c.asValue(b);
                System.out.println("boolean " + b);
                break;
            case NULL:
                // System.out.println("found null");
                out = c.asValue(null);
                System.out.println("null");
                break;
            case NUMBER:
                // System.out.println("found number");
                int i = in.readInt();
                out = c.asValue(i);
                System.out.println("int " + i);
                break;
            case STRING:
                // System.out.println("found string");
                String s = in.readUTF();
                out = c.asValue(s);
                System.out.println("String " + s);
                break;
            case DUPLICATE:
                // System.out.println("found cached value");
                return seenCache.get(in.readInt());
            case ARRAY:
                long arraylength = in.readLong();
                if (arraylength != 0) {
                    // System.out.println("array of length " + arraylength);
                    out = c.eval(lang.toString(), String.format("[]"));
                }
                for (int j = 0; j < arraylength; j++) {
                    out.setArrayElement(j, deserializeHelper());
                    System.out.println("Array element " + j);
                }
                break;
            case OBJECT:
                String className = in.readUTF();
                System.out.println("Got object, class name is " + className);
                // out = c.eval(lang, String.format("new %s()", className));
                out = c.eval(lang.toString(), className).newInstance();

                // for(String key : out.getMemberKeys()) {
                for (String key : getMemberVariables(out)) {
                    // System.out.println("now reading in " + s);
                    if (key.equals(
                            "__proto__")) { // TODO: for some reason can't serialize js inheritance
                        // chain
                        continue;
                    }
                    System.out.println("Reading member, id is " + key);
                    Value member = deserializeHelper();
                    if (member != null) {
                        setInstanceVariable(out, member, key);
                        // out.putMember(key, member);
                    }
                }
                break;
            default:
                throw new IOException("we should never get here, unknown type " + type);
        }
        seenCache.put(seenCache.size(), out);
        return out;
    }

    private List<String> getMemberVariables(Value v) {
        switch (lang) {
            case ruby:
                Value instVars = v.getMember("instance_variables").execute();
                List<String> varStr = new ArrayList<String>();
                for (int i = 0; i < instVars.getArraySize(); i++) {
                    varStr.add(instVars.getArrayElement(i).toString());
                }
                return varStr;
            case js:
                return new ArrayList<>(v.getMemberKeys());
        }
        return new ArrayList<>();
    }

    private void setInstanceVariable(Value out, Value member, String key) {
        if (member == null || out == null) {
            return;
        }
        switch (lang) {
            case ruby:
                key = key.replaceAll(":", "");
                out.getMember("instance_variable_set").execute(key, member);
                return;
            case js:
                out.putMember(key, member);
                return;
        }

        return;
    }

    public void close() throws Exception {
        in.close();
        in = null;
    }
}

package sapphire.graal.io;

import java.io.*;
import java.util.*;
import org.graalvm.polyglot.*;
import sapphire.app.Language;

public class Deserializer implements AutoCloseable {

    private DataInputStream in;
    public Map<Integer, Value> seenCache = new HashMap<Integer, Value>();
    private Language lang;
    private Context context;

    public Deserializer(InputStream in, Context c) throws IOException {
        this.in = new DataInputStream(in);
        this.context = c;
        if (this.context == null) {
            this.context = context;
        }
    }

    public Value deserialize() throws IOException {
        seenCache = new HashMap<Integer, Value>();
        lang = Language.valueOf(in.readUTF());
        return deserializeHelper();
    }

    public Value deserializeHelper() throws IOException {
        Value out = null;

        GraalType type = GraalType.values()[in.readInt()];
        switch (type) {
            case BOOLEAN:
                boolean b = in.readBoolean();
                out = context.asValue(b);
                break;
            case NULL:
                out = context.asValue(null);
                break;
            case NUMBER:
                int i = in.readInt();
                out = context.asValue(i);
                break;
            case STRING:
                String s = in.readUTF();
                out = context.asValue(s);
                break;
            case DUPLICATE:
                return seenCache.get(in.readInt());
            case ARRAY:
                long arraylength = in.readLong();
                if (arraylength != 0) {
                    out = context.eval(lang.toString(), String.format("[]"));
                }
                for (int j = 0; j < arraylength; j++) {
                    out.setArrayElement(j, deserializeHelper());
                }
                break;
            case OBJECT:
                String className = in.readUTF();
                out = context.eval(lang.toString(), className).newInstance();

                // for(String key : out.getMemberKeys()) {
                for (String key : getMemberVariables(out)) {
                    if (key.equals(
                            "__proto__")) { // TODO: for some reason can't serialize js inheritance
                        continue;
                    }
                    Value member = deserializeHelper();
                    if (member != null) {
                        setInstanceVariable(out, member, key);
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
        if (in != null) {
            in.close();
            in = null;
        }
    }
}

package sapphire.graalvm.serde;

import org.graalvm.polyglot.*;
import java.io.*;
import java.util.*;


public class Deserializer implements AutoCloseable {

	private DataInputStream in;
	private Context c;
	public Map<Integer, Value> seenCache = new HashMap<Integer, Value>();
	private String lang;
		
	public Deserializer(InputStream in, Context c) {
		this.in = new DataInputStream(in);
		this.c = c;
	}

	public Value deserialize() throws Exception {
		seenCache = new HashMap<Integer, Value>();
		lang = in.readUTF();
		return deserializeHelper();
	}

	public Value deserializeHelper() throws Exception {
		Value out = null;

		GraalType type = GraalType.values()[in.readInt()];
		switch(type) {
			case BOOLEAN:
				//System.out.println("found boolean");
				boolean b = in.readBoolean();
				out = c.asValue(b);
				break;
			case NULL:
				out = c.asValue(null);
				break;
			case NUMBER:
				int i = in.readInt();
				out = c.asValue(i);
				break;
			case STRING:
				String s = in.readUTF();
				out = c.asValue(s);
				break;
			case DUPLICATE:
				return seenCache.get(in.readInt());
			case ARRAY:
				long arraylength = in.readLong();
				if(arraylength != 0) {
					out = c.eval(lang, String.format("[]"));
				}
				for(int j = 0; j < arraylength; j++) {
					out.setArrayElement(j, deserializeHelper());
				}
				break;
			case OBJECT:
				String className = in.readUTF();
				out = c.eval(lang, className).newInstance();

				for(String key : getMemberVariables(out)) {
					if(key.equals("__proto__")) { //TODO: for some reason can't serialize js inheritance chain
						continue;
					}
					Value member = deserializeHelper();
					if(member != null) {
                        setInstanceVariable(out,member,key);
					}
				}
				break;
			default:
				throw new IllegalArgumentException("we should never get here, unknown type " + type);
		}
		seenCache.put(seenCache.size(), out);
		return out;
	}

	private List<String> getMemberVariables(Value v){
		switch ( lang ){
			case "ruby":
				Value instVars = v.getMember("instance_variables").execute();
				List<String> varStr = new ArrayList<String>();
				for ( int i = 0; i < instVars.getArraySize(); i++){
                    varStr.add(instVars.getArrayElement(i).toString());
				}
				return varStr;
			case "js":
				return new ArrayList<>(v.getMemberKeys());
		}
		return new ArrayList<>();
	}

    private void setInstanceVariable(Value out, Value member, String key){
        if(member == null || out == null) {
           return;
        }
        switch ( lang ){
            case "ruby":
                key = key.replaceAll(":","");
                out.getMember("instance_variable_set").execute(key, member);
                return;
            case "js":
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

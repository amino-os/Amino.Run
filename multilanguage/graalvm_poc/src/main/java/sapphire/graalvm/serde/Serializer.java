package sapphire.graalvm.serde;

import org.graalvm.polyglot.*;
import java.io.*;
import java.util.*;

// DUPLICATE represents duplicate objects that has been serialized once.
enum GraalType
{
	BOOLEAN, NULL, NUMBER, STRING, ARRAY, OBJECT, DUPLICATE;
}

public class Serializer implements AutoCloseable {

	//TODO: should do identity better than toString
	private Map<String, Integer> seenCache;
	private int seenInd;
	private DataOutputStream out;
	private String lang;

	public Serializer(OutputStream os, String l) {
		out = new DataOutputStream(os);
		lang = l;
	}

	public void serialize(Value v) throws Exception {
		seenInd = 0;
		seenCache = new HashMap<String, Integer>();
		out.writeUTF(lang);
		serializeHelper(v);
	}

	//TODO narrow exception
	private void serializeHelper(Value v) throws Exception {
		//if (v.canExecute()) return;

		//check if value cached
		if(seenCache.keySet().contains(v.toString())) {
			out.writeInt(GraalType.DUPLICATE.ordinal());
			out.writeInt(seenCache.get(v.toString()));
			return;
		} else if (!v.isBoolean() && !v.isNull() && !v.isNumber()) {
			seenCache.put(v.toString(),seenInd);
			//out.writeInt(seenInd);
			seenInd++;
		}

		if(v.isBoolean()) {
			out.writeInt(GraalType.BOOLEAN.ordinal());
			out.writeBoolean(v.asBoolean());
		} else if(v.isNativePointer()) {
			throw new IllegalArgumentException("native pointer not supported");
		} else if(v.isNull()) {
			out.writeInt(GraalType.NULL.ordinal());
		} else if(v.isNumber()) {
			out.writeInt(GraalType.NUMBER.ordinal());
			out.writeInt(v.asInt());
		} else if(v.isString()) {
			out.writeInt(GraalType.STRING.ordinal());
			out.writeUTF(v.asString());
		} else if(v.hasArrayElements()) {
			out.writeInt(GraalType.ARRAY.ordinal());
			out.writeLong(v.getArraySize());
			for(long i = 0; i < v.getArraySize(); i++) {
				serializeHelper(v.getArrayElement(i));
			}
		} else { //isComplex
			out.writeInt(GraalType.OBJECT.ordinal());
			//String className = v.getMetaObject().getMember("className").asString();
			String className = getClassName(v);
			out.writeUTF(className);

			//check named members
			if (v.hasMembers()) {
				//List<String> keys = new ArrayList<>(v.getMemberKeys());
				List<String> keys = getMemberVariables(v);
				for (String k : keys) {
					if (k.equals("__proto__")) { //TODO hard coded for js
						continue;
					}
					serializeHelper(getInstanceVariable(v, k));
				}
			}
		}
	}

	private Value getInstanceVariable(Value v, String s){
        switch ( lang ){
            case "ruby":
                s = s.replaceAll(":","");
                return v.getMember("instance_variable_get").execute(s);
            case "js":
                return v.getMember(s);
        }
        return null;
    }

	private String getClassName(Value v){
	    switch ( lang ){
	        case "ruby":
	            return v.getMetaObject().getMember("name").execute().asString();
            case "js":
                return v.getMetaObject().getMember("className").asString();
        }
        return "INVALID_LANG";
    }

    private List<String> getMemberVariables(Value v){
        switch ( lang ){
            case "ruby":
                Value instVars = v.getMember("instance_variables").execute();
                List<String> varSte = new ArrayList<String>();
                for ( int i = 0; i < instVars.getArraySize(); i++){
                   varSte.add(instVars.getArrayElement(i).toString());
                }
                return varSte;
            case "js":
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

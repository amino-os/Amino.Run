import org.graalvm.polyglot.*;
import java.io.*;
import java.util.*;

public class Writer {

	//TODO: should do identity better than toString
	private Map<String, Integer> seenCache;
	private int seenInd;
	private DataOutputStream out;

	public Writer(OutputStream os) {
		out = new DataOutputStream(os);
	}

	public void write(Value v) throws Exception {
		seenInd = 0;
		seenCache = new HashMap<String, Integer>();
		writeHelper(v);
	}

	//TODO narrow exception
	private void writeHelper(Value v) throws Exception {
		//check if value cached
		if(seenCache.keySet().contains(v.toString())) {
			out.writeInt(seenCache.get(v.toString()));
			out.writeShort(TypesDB.Types.SEEN.ordinal());
			return;
		} else {
			//System.out.println("new value to cache: " + v.toString());
			seenCache.put(v.toString(),seenInd);
			out.writeInt(seenInd);
			seenInd++;
		}

		//value type switch
		short typeindex = 0;
		if(v.isBoolean()) {
			//System.out.println("found boolean: " + v);
			out.writeShort(TypesDB.Types.BOOLEAN.ordinal());
			out.writeBoolean(v.asBoolean());
		} else if(v.isNativePointer()) {
			throw new IllegalArgumentException("native pointer not supported");
		} else if(v.isNull()) {
			//System.out.println("found null");
			out.writeShort(TypesDB.Types.NULL.ordinal());
		} else if(v.isNumber()) {
			//System.out.println("found number (assuming int): " + v);
			out.writeShort(TypesDB.Types.NUMBER.ordinal());
			out.writeInt(v.asInt());
		} else if(v.isString()) {
			//System.out.println("found string: " + v);
			out.writeShort(TypesDB.Types.STRING.ordinal());
			out.writeUTF(v.asString());
		} else { //isComplex
			//System.out.println("found non-primitive");
			typeindex = TypesDB.getTypeID(v);
			out.writeShort(typeindex);
		}

		//check array members
		if(v.hasArrayElements()) {
			//System.out.println("has array of length " + v.getArraySize());
			out.writeLong(v.getArraySize());
			for(long i = 0; i < v.getArraySize(); i++) {
				writeHelper(v.getArrayElement(i));
			}
		} else {
			out.writeLong(0);
		}

		//check named members
		if(v.hasMembers()) {
			List<String> keys = TypesDB.getMembers(typeindex);
			//System.out.println("writing " + keys.size() + " members");
			for(String k : keys) {
				if(k.equals("__proto__")) { //TODO hard coded for js
					continue;
				}
				//System.out.println("key: " + k + " value: " + v.getMember(k));
				writeHelper(v.getMember(k));
			}
		}
	}

	public void close() throws Exception {
		out.close();
		out = null;
	}
}

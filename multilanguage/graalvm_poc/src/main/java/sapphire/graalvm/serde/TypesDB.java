package sapphire.graalvm.serde;

import org.graalvm.polyglot.*;
import java.util.*;

public class TypesDB {
	
	public enum Types {
		BOOLEAN, NATIVE_POINTER, NULL, NUMBER, STRING, SEEN, DYNAMIC_START
	}

	public static final Types[] fromInt = Types.values();
	private static List<Type> typesById = new ArrayList<Type>();
  	private static Map<String, Short> typesByName = new HashMap<String, Short>();
	
	public static void register(Value prototype) {
		registerHelper(prototype);

    	Set<String> keys = prototype.getMemberKeys();
    	for (String k : keys) {
    		Value v = prototype.getMember(k);
    		if (v.hasArrayElements() || v.hasMembers()) {
    			register(v);
			}
 		}
	}

	private static void registerHelper(Value prototype) {
		Type t = new Type(prototype);
		String typeName = prototype.getMetaObject().toString();
		typesByName.put(typeName, (short)(typesById.size() + fromInt.length));
		typesById.add(t);
	}

	public static short getTypeID(Value v) {
    return typesByName.getOrDefault(v.getMetaObject().toString(), (short)-1);
	}

	public static Value newInstance(short id, Context c) {
		if(id < fromInt.length) 
	        throw new IllegalArgumentException("cannot instantiate primitive. got code: " + id);
		return typesById.get(id - fromInt.length).newInstance(c);
	}

	public static List<String> getMembers(short id) {
		if(id < fromInt.length)
			return new ArrayList<>();

		return typesById.get(id - fromInt.length).members;
	}

	public static class Type {
		public List<String> members;
		private Value prototypeConstructor;

		public Type(Value prototype) {
			// prototype is a function which can be
			// used to create new instances.
			this.prototypeConstructor = prototype.getMember("constructor");
			if (prototypeConstructor == null) {
				throw new Error("Constructor not declared in value " + prototype);
			}

			this.members = new ArrayList<>();
			for(String m : new TreeSet<>(prototype.getMemberKeys())) {
				// verified that getMemberKeys will not return
				// executables and functions.
				Value v = prototype.getMember(m);
				if(!v.canExecute()) {
					this.members.add(m);
				} 
			}
		}

		public Value newInstance(Context c) {
			return prototypeConstructor.newInstance();
		}
	}
}

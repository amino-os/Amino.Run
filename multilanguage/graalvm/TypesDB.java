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
		Type t = new Type(prototype);
    typesByName.put(prototype.getMetaObject().toString(), (short)(typesById.size() + fromInt.length));
    typesById.add(t);
	}

	public static short getTypeID(Value v) {
    return typesByName.get(v.getMetaObject().toString());
	}

	public static Value newInstance(short id, Context c) {
		if(id < fromInt.length) 
        throw new IllegalArgumentException("cannot instantiate primitive. got code: " + id);
		return typesById.get(id - fromInt.length).newInstance(c);
	}

	public static List<String> getMembers(short id) {
		if(id < fromInt.length)
			return new ArrayList<String>();

		return typesById.get(id - fromInt.length).members;
	}


	public static class Type {
		public List<String> members;
		private Value prototype;

		public Type(Value prototype) {
			this.prototype = prototype;
			this.members = new ArrayList<String>();

			for(String m : new TreeSet<String>(prototype.getMemberKeys())) {
				Value v = prototype.getMember(m);
				if(!v.canExecute()) { //TODO techincally can be executable and data
					this.members.add(m);
				} 
			}
		}

		public Value newInstance(Context c) {
      return prototype.getMember("construct").execute();
		}
	}

}

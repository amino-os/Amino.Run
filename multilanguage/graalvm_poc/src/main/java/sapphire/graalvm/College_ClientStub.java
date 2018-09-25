import org.graalvm.polyglot.*;
import java.io.ByteArrayOutputStream;
import java.util.*;
public final class College_ClientStub {
	enum GraalLanguage { JS, PYTHON, R, RUBY, LLVM;}
	GraalLanguage lang;
	sapphire.policy.SapphirePolicy.SapphireClientPolicy client = null;

	public void initialize(sapphire.policy.SapphirePolicy.SapphireClientPolicy client, GraalLanguage lang) {
        this.client = client;
        this.lang = lang;
    }

	public Object setName(Object... args) {
		ArrayList<Object> params = new ArrayList<>();
		params.add(lang);
		params.addAll(Arrays.asList(args));
		return client.onRPC("setName", params);
	}

	public Object getName(Object... args) {
		ArrayList<Object> params = new ArrayList<>();
		params.add(lang);
		params.addAll(Arrays.asList(args));
		return client.onRPC("getName", params);
	}

	public Object addStudent(Object... args) {
		ArrayList<Object> params = new ArrayList<>();
		params.add(lang);
		params.addAll(Arrays.asList(args));
		return client.onRPC("addStudent", params);
	}

	public Object getStudents(Object... args) {
		ArrayList<Object> params = new ArrayList<>();
		params.add(lang);
		params.addAll(Arrays.asList(args));
		return client.onRPC("getStudents", params);
	}

	public Object toString(Object... args) {
		ArrayList<Object> params = new ArrayList<>();
		params.add(lang);
		params.addAll(Arrays.asList(args));
		return client.onRPC("toString", params);
	}

}

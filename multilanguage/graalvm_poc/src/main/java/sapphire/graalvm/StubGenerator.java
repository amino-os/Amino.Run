package sapphire.graalvm;

import java.util.*;
import java.io.*;
import org.graalvm.polyglot.*;
import java.util.logging.Logger;

public class StubGenerator {

    // Enumerate all languages that are processed on server side by polyglot,
    // the string matches can be directly used as parameter to polyglot.eval function call.
    public enum GraalLanguage {
        JS,
        PYTHON,
        R,
        RUBY,
        LLVM;
    }
  
	public static void main(String[] args) throws Exception {
        //src folder, package name, dest folder
        String[] supportedLangs = { "js", "python", "ruby" };
        Context polyglot = Context.newBuilder(supportedLangs).allowAllAccess(true).build();
        String jsHome = System.getProperty("JS_HOME");
        jsHome = "/Users/haibinxie/Code/DCAP_Sapphire_Fork/DCAP-Sapphire/multilanguage/graalvm_poc/src/main/js";

        Value v = polyglot.eval(Source.newBuilder("js", new File(jsHome + "/college.js")).build());
        Value v1 = polyglot.eval("js", "new College()");
        StubGenerator stubGenerator = new StubGenerator(GraalLanguage.JS, v1);
        System.out.println("is there setName member? " + v1.hasMember("setName"));
        System.out.println("all member keys: " + v1.getMemberKeys());
        stubGenerator.generateClientStub();
	}

	public StubGenerator(GraalLanguage lang, Value prototype) {
		this.fileName = prototype.getMetaObject().getMember("className") + "_ClientStub.java";
	    this.prototype = prototype;
	    this.lang = lang;
	}

	private PrintStream out;
	private Value prototype;
	private int indentLevel;
	private String fileName;
	private GraalLanguage lang;
    private static Logger logger = Logger.getLogger(StubGenerator.class.getName());

	public void generateClientStub() throws FileNotFoundException {
		//TODO directory for package name
		this.out = new PrintStream(fileName);
		if(prototype.isHostObject()) {
			generateJava();
		} else {
			generateGuest();
		}
	}

	public void generateGuest() {
        //TODO package
        println("import org.graalvm.polyglot.*;");
        println("import java.io.ByteArrayOutputStream;");
        println("import java.util.*;");

        println("public final class " + prototype.getMetaObject().getMember("className") + "_ClientStub {"); //TODO implements interface?
        indentLevel++;
        println("enum GraalLanguage { JS, PYTHON, R, RUBY, LLVM;}");
        println("GraalLanguage lang;");
        println("sapphire.policy.SapphirePolicy.SapphireClientPolicy client = null;\n");

        println("public void initialize(sapphire.policy.SapphirePolicy.SapphireClientPolicy client, GraalLanguage lang) {\n" +
                "        this.client = client;\n" +
                "        this.lang = lang;\n" +
                "    }\n");

        System.out.println(prototype.getMemberKeys());
		for(String m : prototype.getMemberKeys()) {
            if (m.startsWith("__")) break;

            System.out.println("got key " + m);
			if(prototype.getMember(m).canExecute() && !m.equals("constructor")) {
				println("public Object " + m + "(Object... args) {");
                indentLevel++;
                println("ArrayList<Object> params = new ArrayList<>();");
                println("params.add(lang);");
                println("params.addAll(Arrays.asList(args));");
                println(String.format("return client.onRPC(\"%s\", params);", m));
                indentLevel--;
				println("}\n");
			}	
		}
        indentLevel--;
		println("}");
	}

	//we can get arg types for java
	public void generateJava() {
      //TODO: for now we just use weak typing
      generateGuest();
	}

    private void println(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indentLevel; ++i) sb.append('\t');
        String tabs = sb.toString();

        out.println(tabs + s);
    }
}

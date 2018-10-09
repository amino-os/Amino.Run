package sapphire.compiler;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import org.graalvm.polyglot.*;
import sapphire.app.Language;

public class GraalStubGenerator {

    private static String workingDir =
            "./examples/hanksTodo/src/main/java/sapphire/appexamples/college";

    public static void main(String[] args) throws Exception {
        // src folder, package name, dest folder
        String[] supportedLangs = {"js", "python", "ruby"};
        Context polyglot = Context.newBuilder(supportedLangs).allowAllAccess(true).build();
        String jsHome = System.getProperty("JS_HOME");
        if (jsHome != null && !jsHome.isEmpty()) workingDir = jsHome;

        polyglot.eval(Source.newBuilder("js", new File(workingDir + "/college.js")).build());
        Value v1 = polyglot.eval("js", "new College()");
        GraalStubGenerator stubGenerator = new GraalStubGenerator(Language.js, v1);
        stubGenerator.generateClientStub();

        Value v2 = polyglot.eval("js", "new Student()");
        GraalStubGenerator stubGenerator2 = new GraalStubGenerator(Language.js, v2);
        stubGenerator2.generateClientStub();
    }

    public GraalStubGenerator(Language lang, Value prototype) {
        this.fileName =
                workingDir
                        + "/stubs/"
                        + prototype.getMetaObject().getMember("className")
                        + "_ClientStub.java";
        this.prototype = prototype;
        this.lang = lang;
    }

    private PrintStream out;
    private Value prototype;
    private int indentLevel;
    private String fileName;
    private Language lang;
    private static Logger logger = Logger.getLogger(StubGenerator.class.getName());

    public void generateClientStub() throws FileNotFoundException {
        // TODO directory for package name
        this.out = new PrintStream(fileName);
        if (prototype.isHostObject()) {
            generateJava();
        } else {
            generateGuest();
        }
    }

    public void generateGuest() {
        // TODO package
        println("import org.graalvm.polyglot.*;");
        println("import java.io.ByteArrayOutputStream;");
        println("import java.util.*;");
        println("import sapphire.app.Language;\n");

        println(
                "public final class "
                        + prototype.getMetaObject().getMember("className")
                        + "_ClientStub {"); // TODO implements interface?
        indentLevel++;
        println(String.format("Language lang = Language.%s;", lang.toString()));
        println("sapphire.policy.SapphirePolicy.SapphireClientPolicy client = null;\n");

        println(
                "public void initialize(sapphire.policy.SapphirePolicy.SapphireClientPolicy client) {\n"
                        + "        this.client = client;\n"
                        + "    }\n");

        System.out.println(prototype.getMemberKeys());
        for (String m : prototype.getMemberKeys()) {
            if (m.startsWith("__")) break;

            System.out.println("got key " + m);
            if (prototype.getMember(m).canExecute() && !m.equals("constructor")) {
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

    // we can get arg types for java
    public void generateJava() {
        // TODO: for now we just use weak typing
        generateGuest();
    }

    private void println(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indentLevel; ++i) sb.append('\t');
        String tabs = sb.toString();

        out.println(tabs + s);
    }
}

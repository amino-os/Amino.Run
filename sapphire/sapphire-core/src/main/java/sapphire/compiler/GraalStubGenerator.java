package sapphire.compiler;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import org.graalvm.polyglot.*;
import sapphire.app.Language;

public class GraalStubGenerator {

    private static String workingDir =
            "./examples/hanksTodoRuby/src/main/js/sapphire/appexamples/hankstodo";

    public static void main(String[] args) throws Exception {
        try {
            // src folder, package name, dest folder
            String[] supportedLangs = {"js", "python", "ruby"};
            Context polyglot = Context.newBuilder(supportedLangs).allowAllAccess(true).build();
            String jsHome = System.getProperty("JS_HOME");
            if (jsHome != null && !jsHome.isEmpty()) workingDir = jsHome;

            polyglot.eval(Source.newBuilder("js", new File(workingDir + "/todo_list_manager.js")).build());
            Value v1 = polyglot.eval("js", "TodoListManager").newInstance();
            GraalStubGenerator stubGenerator = new GraalStubGenerator(Language.js, v1);
            stubGenerator.generateStub();
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    public GraalStubGenerator(Language lang, Value prototype) {
        this.prototype = prototype;
        this.lang = lang;
        this.fileName =
                workingDir
                        + "/stubs/"
                        + prototype.getMetaObject().getMember("className")
                        + stubSuffix
                        + ".java";
    }

    private PrintStream out;
    private Value prototype;
    private int indentLevel;
    private String fileName;
    private Language lang;
    private static Logger logger = Logger.getLogger(StubGenerator.class.getName());
    private static String stubSuffix = "_GraalStub";
    private String packageName = "sapphire.appexamples.hankstodo.stubs";

    // string format following these inputs:
    // packageName
    // className
    // stubSuffix
    // className
    // stubSuffix
    // className
    // functions
    String codeStringFormat =
            "package %s;\n"+
            "import org.graalvm.polyglot.Value;\n" +
            "import jdk.nashorn.internal.runtime.regexp.joni.exception.SyntaxException;\n" +
            "import sapphire.graal.io.SerializeValue;\n" +
            "\n" +
            "public final class %s%s extends sapphire.common.GraalObject implements sapphire.common.AppObjectStub {\n" +
            "\n" +
            "    sapphire.policy.SapphirePolicy.SapphireClientPolicy $__client = null;\n" +
            "    boolean $__directInvocation = false;\n" +
            "\n" +
            "    public %s%s () {super();}\n" +
            "\n" +
            "    public void $__initialize(sapphire.policy.SapphirePolicy.SapphireClientPolicy client) {\n" +
            "        $__client = client;\n" +
            "    }\n" +
            "\n" +
            "    public void $__initialize(boolean directInvocation) {\n" +
            "        $__directInvocation = directInvocation;\n" +
            "    }\n" +
            "\n" +
            "    public Object $__clone() throws CloneNotSupportedException {\n" +
            "        return super.clone();\n" +
            "    }\n" +
            "\n" +
            "    public void $__initializeGraal(sapphire.app.SapphireObjectSpec spec, java.lang.Object[] params){\n" +
            "        try {\n" +
            "            super.$__initializeGraal(spec, params);\n" +
            "        } catch (java.lang.Exception e) {\n" +
            "            throw new java.lang.RuntimeException(e);\n" +
            "        }\n" +
            "\n" +
            "    }\n\n" +
            "%s" +
            " }\n";

    // String format following these inputs:
    // functionName
    // functionName
    // packageName
    // className
    // functionName
    String functionStringFormat =
            "    public java.lang.Object %s(Object... args) {\n" +
            "        java.lang.Object $__result = null;\n" +
            "        if ($__directInvocation) {\n" +
            "            try {\n" +
            "                java.util.ArrayList<Object> objs = new java.util.ArrayList<>();\n" +
            "                objs.addAll(Arrays.asList(args));\n" +
            "                $__result = super.invoke(\"%s\", objs);\n" +
            "            } catch (java.lang.Exception e) {\n" +
            "                throw new sapphire.common.AppExceptionWrapper(e);\n" +
            "            }\n" +
            "        } else {\n" +
            "            java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();\n" +
            "            String $__method = \"public java.lang.Object %s.%s_Stub.%s(Object... args)\";\n" +
            "            $__params.addAll(Arrays.asList(args));\n" +
            "            try {\n" +
            "                $__result = $__client.onRPC($__method, $__params);\n" +
            "                if ($__result instanceof SerializeValue) {\n" +
            "                    $__result = deserializedSerializeValue((SerializeValue)$__result);\n" +
            "                    $__result = ((org.graalvm.polyglot.Value)$__result).as(java.lang.Object.class);\n" +
            "                    System.out.println($__result);\n" +
            "                }\n" +
            "            } catch (sapphire.common.AppExceptionWrapper e) {\n" +
            "                Exception ex = e.getException();\n" +
            "                if (ex instanceof java.lang.RuntimeException) {\n" +
            "                    throw (java.lang.RuntimeException) ex;\n" +
            "                } else {\n" +
            "                    throw new java.lang.RuntimeException(ex);\n" +
            "                }\n" +
            "            } catch (java.lang.Exception e) {\n" +
            "                throw new java.lang.RuntimeException(e);\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        System.out.println($__result);\n" +
            "        return ($__result);\n" +
            "    }\n\n";

    public String getClassName() {
        return prototype.getMetaObject().getMember("className").asString();
    }
    public void generateStub() throws FileNotFoundException {
        String className = getClassName();
        String functions = generateFunctions();
        String code = String.format(codeStringFormat,
                packageName,
                className,
                stubSuffix,
                className,
                stubSuffix,
                functions);

        this.out = new PrintStream(fileName);
        this.out.print(code);
    }

    public String generateFunctions() {
        StringBuilder res = new StringBuilder();
        String className = getClassName();
        for (String m : prototype.getMemberKeys()) {
            // Graal Value has lots of self defined function, let's skip them.
            if (m.startsWith("__")) break;

            System.out.println("got key " + m);
            if (prototype.getMember(m).canExecute() && !m.equals("constructor")) {

                String function = String.format(functionStringFormat, m, m, packageName, className, m);
                res.append(function);
            }
        }
        return res.toString();
    }
}

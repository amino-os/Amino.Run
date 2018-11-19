package sapphire.compiler;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.polyglot.*;

public class GraalStubGenerator {
    private Value prototype;
    private String fileName;
    private String lang;
    private static Logger logger = Logger.getLogger(GraalStubGenerator.class.getName());
    private static String stubSuffix = "_Stub";
    private static String packageName;
    private static String[] supportedLangs = {"js", "python", "ruby"};

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println(
                    "Invalid arguments, expected arguments:  [SapphireObjectFileOrFolder] [OutputFolder] [StubPackageName] [ClassNamesSeparatedByComma]");
            return;
        }

        String inputFile = args[0], outputDirectory = args[1];
        packageName = args[2];
        String[] classNames = args[3].split(",");

        Context polyglot = Context.newBuilder(supportedLangs).allowAllAccess(true).build();

        // Evaluate all polyglot files recursively.
        File f = new File(inputFile);
        List<File> files = new ArrayList<>();
        getFilesRecursively(files, f);
        for (File sub : files) {
            loadFile(sub, polyglot);
        }

        for (String className : classNames) {
            for (String lang : supportedLangs) {
                generateStub(polyglot, lang, className, outputDirectory);
            }
        }
    }

    private static void generateStub(
            Context polyglot, String lang, String className, String outputDirectory)
            throws Exception {
        Value v;
        try {
            v = polyglot.eval(lang, className).newInstance();
        } catch (Exception e) {
            logger.log(
                    Level.INFO,
                    String.format("Could not find class %s in language %s", className, lang));
            return;
        }

        logger.log(
                Level.INFO,
                String.format("Generating stub for class %s in language %s", className, lang));
        GraalStubGenerator gen =
                new GraalStubGenerator(
                        v,
                        lang,
                        outputDirectory + "/" + packageName.replaceAll("\\.", "/"),
                        packageName);
        gen.generateStub();
    }

    private static void loadFile(File file, Context polyglot) {
        String l = getLanguageFromFileName(file.getName());
        if (!l.isEmpty()) {
            try {
                polyglot.eval(Source.newBuilder(l, file).build());
            } catch (Exception e) {
                // Note, we don't throw exception, skip the file and continue evaluating
                // remaining files.
                logger.log(
                        Level.WARNING,
                        String.format(
                                "Failed to evaluate file %s via lang %s, exception is %s",
                                file.getAbsolutePath(), l, e.getMessage()));
            }
        }
    }

    private static void getFilesRecursively(List<File> files, File f) {
        if (f.isFile()) {
            files.add(f);
            return;
        }

        if (f.isDirectory()) {
            for (File sub : f.listFiles()) {
                getFilesRecursively(files, sub);
            }
        }
    }

    private static String getLanguageFromFileName(String fileName) {
        if (fileName.endsWith(".js")) return "js";
        if (fileName.endsWith(".rb")) return "ruby";
        if (fileName.endsWith(".py")) return "python";

        // TODO: test and add support for other languages.
        return "";
    }

    public GraalStubGenerator(Value prototype, String lang, String outputDir, String packageName) {
        this.lang = lang;
        this.prototype = prototype;
        this.fileName = outputDir + "/" + getClassName() + stubSuffix + ".java";
        this.packageName = packageName;
        new File(outputDir).mkdirs();
    }

    // string format following these inputs:
    // packageName
    // className
    // stubSuffix
    // className
    // stubSuffix
    // className
    // functions
    private static String codeStringFormat =
            "package %s;\n\n"
                    + "import java.io.File;\n"
                    + "import java.net.InetSocketAddress;\n"
                    + "import java.nio.file.Files;\n"
                    + "import java.rmi.registry.LocateRegistry;\n"
                    + "import java.rmi.registry.Registry;\n"
                    + "import java.util.Arrays;\n"
                    + "import java.util.ArrayList;\n"
                    + "import java.util.List;\n"
                    + "import org.graalvm.polyglot.Value;\n"
                    + "import sapphire.app.SapphireObjectServer;\n"
                    + "import sapphire.app.SapphireObjectSpec;\n"
                    + "import sapphire.common.SapphireObjectID;\n"
                    + "import sapphire.graal.io.SerializeValue;\n"
                    + "import sapphire.kernel.server.KernelServerImpl;"
                    + "\n\n"
                    + "public final class %s%s extends sapphire.common.GraalObject implements sapphire.common.GraalAppObjectStub {\n"
                    + "\n"
                    + "    sapphire.policy.SapphirePolicy.SapphireClientPolicy $__client = null;\n"
                    + "    boolean $__directInvocation = false;\n"
                    + "\n"
                    + "    public static %s%s getStub(String specYamlFile, String omsIP, String omsPort, String hostIP, String hostPort) throws Exception {\n"
                    + "        String spec = getSpec(specYamlFile);\n"
                    + "        Registry registry = LocateRegistry.getRegistry(omsIP, Integer.parseInt(omsPort));\n"
                    + "        new KernelServerImpl(new InetSocketAddress(hostIP, Integer.parseInt(hostPort)), new InetSocketAddress(omsIP, Integer.parseInt(omsPort)));\n"
                    + "        SapphireObjectServer server = (SapphireObjectServer) registry.lookup(\"SapphireOMS\");\n"
                    + "\n"
                    + "        SapphireObjectID oid = server.createSapphireObject(spec);\n"
                    + "        %s%s stub = (%s%s)server.acquireSapphireObjectStub(oid);\n"
                    + "        stub.$__initializeGraal(SapphireObjectSpec.fromYaml(spec));\n"
                    + "\n"
                    + "        return stub;\n"
                    + "    }\n"
                    + "\n"
                    + "    private static String getSpec(String specYamlFile) throws Exception {\n"
                    + "        ClassLoader classLoader = new %s%s().getClass().getClassLoader();\n"
                    + "        File file = new File(classLoader.getResource(specYamlFile).getFile());\n"
                    + "        List<String> lines = Files.readAllLines(file.toPath());\n"
                    + "        return String.join(\"\\n\", lines);\n"
                    + "    }\n\n"
                    + "    public %s%s () {super();}\n"
                    + "\n"
                    + "    public void $__initialize(sapphire.policy.SapphirePolicy.SapphireClientPolicy client) {\n"
                    + "        $__client = client;\n"
                    + "    }\n"
                    + "\n"
                    + "    public void $__initialize(boolean directInvocation) {\n"
                    + "        $__directInvocation = directInvocation;\n"
                    + "    }\n"
                    + "\n"
                    + "    public Object $__clone() throws CloneNotSupportedException {\n"
                    + "        return clone();\n"
                    + "    }\n"
                    + "\n"
                    + "    public boolean $__directInvocation(){\n"
                    + "        return $__directInvocation;\n"
                    + "    }\n\n"
                    + "    public java.util.List<SerializeValue> serializeParams(Object... args) throws Exception {\n"
                    + "        java.util.List<SerializeValue> res = new ArrayList<>();\n"
                    + "        getContext().enter();\n"
                    + "        for (Object o : args) {\n"
                    + "            res.add(SerializeValue.getSerializeValue(Value.asValue(o), getLanguage()));\n"
                    + "        }\n"
                    + "        return res;\n"
                    + "    }\n"
                    + "\n"
                    + "    public void $__initializeGraal(sapphire.app.SapphireObjectSpec spec, java.lang.Object[] params){\n"
                    + "        try {\n"
                    + "            super.$__initializeGraal(spec, params);\n"
                    + "        } catch (java.lang.Exception e) {\n"
                    + "            throw new java.lang.RuntimeException(e);\n"
                    + "        }\n"
                    + "\n"
                    + "    }\n\n"
                    + "%s"
                    + " }\n";

    // String format following these inputs:
    // functionName
    // functionName
    // packageName
    // className
    // functionName
    private static String functionStringFormat =
            "    public java.lang.Object %s(Object... args) throws java.lang.Exception{\n"
                    + "        java.lang.Object $__result = null;\n"
                    + "        if ($__directInvocation) {\n"
                    + "            try {\n"
                    + "                java.util.ArrayList<Object> objs = new java.util.ArrayList<>();\n"
                    + "                objs.addAll(Arrays.asList(args));\n"
                    + "                $__result = super.invoke(\"%s\", objs);\n"
                    + "            } catch (java.lang.Exception e) {\n"
                    + "                throw new sapphire.common.AppExceptionWrapper(e);\n"
                    + "            }\n"
                    + "        } else {\n"
                    + "            java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();\n"
                    + "            String $__method = \"public java.lang.Object %s.%s_Stub.%s(java.lang.Object...) throws java.lang.Exception\";\n"
                    + "            $__params.addAll(serializeParams(args));\n"
                    + "            try {\n"
                    + "                $__result = $__client.onRPC($__method, $__params);\n"
                    + "                if ($__result instanceof SerializeValue) {\n"
                    + "                    $__result = deserializedSerializeValue((SerializeValue)$__result);\n"
                    + "                    $__result = ((org.graalvm.polyglot.Value)$__result).as(java.lang.Object.class);\n"
                    + "                }\n"
                    + "            } catch (sapphire.common.AppExceptionWrapper e) {\n"
                    + "                Exception ex = e.getException();\n"
                    + "                if (ex instanceof java.lang.RuntimeException) {\n"
                    + "                    throw (java.lang.RuntimeException) ex;\n"
                    + "                } else {\n"
                    + "                    throw new java.lang.RuntimeException(ex);\n"
                    + "                }\n"
                    + "            } catch (java.lang.Exception e) {\n"
                    + "                throw new java.lang.RuntimeException(e);\n"
                    + "            }\n"
                    + "        }\n"
                    + "\n"
                    + "        return ($__result);\n"
                    + "    }\n\n";

    private String getClassName() {
        return prototype.getMetaObject().toString();
    }

    public void generateStub() throws FileNotFoundException {
        String className = getClassName();
        String functions = generateFunctions();
        String code =
                String.format(
                        codeStringFormat,
                        packageName,
                        className,
                        stubSuffix,
                        className,
                        stubSuffix,
                        className,
                        stubSuffix,
                        className,
                        stubSuffix,
                        className,
                        stubSuffix,
                        className,
                        stubSuffix,
                        functions);

        if (new File(fileName).exists()) {
            logger.log(
                    Level.WARNING,
                    String.format("Stub already exists, will be overwritten. %s", fileName));
        }

        PrintStream out = new PrintStream(fileName);
        out.print(code);
    }

    private String generateFunctions() {
        StringBuilder res = new StringBuilder();
        String className = getClassName();
        for (String m : prototype.getMemberKeys()) {
            // Graal Value has lots of self defined function, let's skip them.
            // TODO: need to find a good way to skip graal self-defined functions.
            if (m.startsWith("__")) {
                // Don't ask why:) this is the pattern with js graal object, all functions after
                // this are
                // Graal self-defined functions so we skip them all.
                if (lang.equals("js")) break;
                else continue;
            }

            System.out.println("got key " + m);
            if (prototype.getMember(m).canExecute() && !m.equals("constructor")) {

                String function =
                        String.format(functionStringFormat, m, m, packageName, className, m);
                res.append(function);
            }
        }
        return res.toString();
    }
}

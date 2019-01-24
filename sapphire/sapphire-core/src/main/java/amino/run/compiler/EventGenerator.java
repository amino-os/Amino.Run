package amino.run.compiler;

import static org.apache.harmony.rmi.compiler.RmicConstants.EOLN;

import amino.run.runtime.AddEvent;

import java.io.*;
import java.lang.reflect.Method;

import org.apache.harmony.rmi.compiler.Indenter;

public class EventGenerator {
    private static String dst;
    private static Indenter indenter;
    private String shortClassName;
    private static String pkg;
    private File file;

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println(
                    "Invalid arguments, expected arguments:  [SapphireObjectFileOrFolder] [OutputFolder] [StubPackageName] [ClassNamesSeparatedByComma]");
            return;
        }
        String src = args[0];
        pkg = args[1];
        dst = args[2];

        File folder = new File(src);
        new EventGenerator().listFilesForFolder(folder);
    }

    public void listFilesForFolder(File folder) {
        createEventConstClass();
        for (File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                try {
                    Class<?> fileClass =
                            Class.forName(
                                    StubGenerator.removeExtension(pkg + "." + fileEntry.getName()));
                    for (Method m : fileClass.getDeclaredMethods()) {
                        if (m.isAnnotationPresent(AddEvent.class)) {
                            String event = m.getAnnotation(AddEvent.class).event();
                            String methodSig = m.toString();
                            int extensionIndex = fileClass.getSimpleName().lastIndexOf("_");
                            String className = fileClass.getSimpleName().substring(0, extensionIndex);
                            addFields(className, event, methodSig);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        addBrace(file);
    }

    public void createEventConstClass() {
        shortClassName = "EventConst";
        String eventConstFile = dst + File.separator + shortClassName + ".java";

        File dest = new File(eventConstFile);
        try {
            dest.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(dest));
            out.write(getSource());
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getSource() {
        indenter = new Indenter();

        return (getHeader()
                + getPackageStatement()
                + EOLN //$NON-NLS-1$
                + getImportStatement()
                + EOLN
                + getClassDeclaration());
    }

    public String getHeader() {
        return "";
    }

    public String getPackageStatement() {
        return "package "
                + GlobalStubConstants.EVENT_GENERATOR_PACKAGE_NAME
                + ';'
                + EOLN; //$NON-NLS-1$
    }

    public String getImportStatement() {
        return "import " + GlobalStubConstants.EVENT_CLASS + ";" + EOLN + EOLN;
    }

    public String getClassDeclaration() {
        StringBuilder buffer = new StringBuilder("");
        buffer.append("public final class " + shortClassName);
        buffer.append(" {" + EOLN); // $NON-NLS-1$
        return buffer.toString();
    }

    public void addFields(String className, String eventName, String methodSig) {
        try {
            String cwd = System.getProperty("user.dir");
            String filePath = cwd + "/src/main/java/amino/run/runtime/EventConst.java";
            file = new File(filePath);
            StringBuilder builder = new StringBuilder();
            if (file.exists()) {
                BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
                    builder.append(EOLN+indenter.tIncrease())
                            .append("public static Event ")
                            .append(className + "_" + eventName)
                            .append(" = new Event(").append("\"")
                            .append(methodSig).append("\"")
                            .append(");").append(EOLN);
                out.write(builder.toString());
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addBrace(File file){
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
            out.write("}");
            out.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}

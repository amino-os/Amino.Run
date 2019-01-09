package amino.run.graal.io;

import amino.run.app.Language;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.polyglot.*;

public class GraalContext {
    private static Context context;
    private static String sapphireObjectPath;
    private static Logger logger = Logger.getLogger(GraalContext.class.getName());

    public static void main(String[] args) throws Exception {
        getContext();
    }

    public static Context getContext() throws IOException {
        if (context == null) {
            context = Context.create();
            setSapphireObjectPath("/Users/haibinxie/SapphireObjects");
        }

        return context;
    }

    // we assume sapphire objects are placed in separate folder.
    private static void setSapphireObjectPath(String dir) throws IOException {
        sapphireObjectPath = dir;
        // Re-evaluate all files in this path.
        for (Language lang : Language.values()) {
            evaluateAllFiles(lang, sapphireObjectPath + "/" + lang.toString());
        }
    }

    private static void evaluateAllFiles(Language language, String dir) throws IOException {
        File[] files = (new File(dir)).listFiles();
        if (files == null) return;
        for (File f : files) {
            try {
                logger.log(Level.INFO, String.format("Found file %s %s", f.getPath(), f.getName()));
                context.eval(Source.newBuilder(language.toString(), f).build());
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.toString());
            }
        }
    }
}

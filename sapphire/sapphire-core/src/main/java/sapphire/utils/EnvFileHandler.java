package sapphire.utils;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;

/**
 * A file handler which is able to replace <code>${LOG_DIR}</code> in log property file with the
 * value of <code>LOG_DIR</code> system property.
 */
public class EnvFileHandler extends FileHandler {
    private static String LOG_DIR_ENV_KEY = "LOG_DIR";
    private static String DEFAULT_LOG_DIR = "/var/log";
    private static String TARGET_PATTERN = "${" + LOG_DIR_ENV_KEY + "}";

    private static String pattern() throws IOException {
        String prefix = EnvFileHandler.class.getName();
        String v = LogManager.getLogManager().getProperty(prefix + ".pattern");
        return v.replace(TARGET_PATTERN, System.getProperty(LOG_DIR_ENV_KEY, DEFAULT_LOG_DIR));
    }

    public EnvFileHandler() throws IOException {
        super(pattern());
    }
}

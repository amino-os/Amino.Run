package sapphire.policy.scalability;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import sapphire.runtime.MethodInvocationRequest;

/**
 * @author terryz
 */
public class FileLogger implements ILogger {
    private long term=0, index=0;
    private static Logger logger = Logger.getLogger(FileLogger.class.getName());
    private ObjectOutputStream oos;

    @Override
    public void log(MethodInvocationRequest request) throws Exception {
        if (oos == null) {
            oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(""))));
        }

        try {
            LogEntry entry = new LogEntry.Builder(term).index(index++).request(request).build();
            oos.writeObject(entry);
        } catch (IOException e) {
            logger.log(Level.WARNING, "failed to write entry in file: {0}", e);
        } finally {
            if (oos != null) {
                try {
                    oos.flush();
                } catch (Exception e) {
                    logger.log(Level.FINE, "failed to flush file: {0}", e);
                }
            }
        }
    }

    public void finalize() {
        if (oos != null) {
            try {
                oos.close();
            } catch (Exception e) {
                logger.log(Level.FINE, "failed to close file: {0}", e);
            }
        }
    }
}

package sapphire.policy.scalability;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author terryz
 */
public class Util {
    private static final Logger logger = Logger.getLogger(Util.class.getName());

    /**
     * Wraps the given {@code Runnable} with a try-catch block.
     * We suggest wrap {@code Runnable}s with this wrapper before passing it into
     * {@link java.util.concurrent.ExecutorService ExecutorService}.
     *
     * @param runnable
     * @return a runnable with try catch block
     */
    public static final Runnable RunnerWrapper(final Runnable runnable) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Throwable e) {
                    logger.log(Level.WARNING, "got exception when execute runnable {0}: {1}", new Object[]{runnable, e});
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * Serialize object into bytes.
     *
     * @param object the object to be serialized. The object must implement {@link java.io.Serializable}.
     * @return a byte array
     * @throws Exception
     */
    public static final byte[] toBytes(Object object) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(object);
            out.flush();
            return bos.toByteArray();
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                // ignore close exception
            }

            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                // ignore close exception
            }
        }
    }
}

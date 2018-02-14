package sapphire.policy.scalability;

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
}

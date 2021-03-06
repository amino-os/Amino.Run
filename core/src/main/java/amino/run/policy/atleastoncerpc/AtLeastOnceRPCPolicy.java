package amino.run.policy.atleastoncerpc;

import amino.run.common.AppExceptionWrapper;
import amino.run.policy.DefaultPolicy;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Deployment Manager policy to automatically retry RPCs for bounded amount of time, with
 * exponential backoff.
 */
public class AtLeastOnceRPCPolicy extends DefaultPolicy {

    public static class ClientPolicy extends DefaultClientPolicy {
        private static final Logger logger = Logger.getLogger(AtLeastOnceRPCPolicy.class.getName());
        // a reasonable default timeout for production
        private long timeoutMilliSeconds = 20000L;
        private long initialExponentialDelayMilliSeconds =
                20L; // Wait this long before the first retry.
        private long exponentialMultiplier = 2L; // Double the wait before every subsequent retry.

        public ClientPolicy() {}

        // for unit test use
        public void setTimeout(long timeoutMilliSeconds) {
            this.timeoutMilliSeconds = timeoutMilliSeconds;
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            long startTime = System.currentTimeMillis();
            long delay = initialExponentialDelayMilliSeconds;
            Exception lastException = null;
            do { // Retry until timeout expires
                try {
                    return super.onRPC(method, params);
                } catch (AppExceptionWrapper e) {
                    logger.info("Not retrying because exception is from application: " + e);
                    throw e; // Don't retry on application exceptions
                } catch (Exception e) {
                    logger.info("Retrying method " + method + " after " + delay + "ms due to " + e);
                    lastException = e; // So we can throw this after the timeout.
                    Thread.sleep(delay);
                    delay *= exponentialMultiplier;
                    continue; // Retry all non-application exceptions, e.g. network failures.
                }
            } while (System.currentTimeMillis() - startTime < timeoutMilliSeconds);
            TimeoutException e =
                    new TimeoutException(
                            "Retry timeout of "
                                    + timeoutMilliSeconds
                                    + "ms exceeded in AtLeastOnceRPCPolicy");
            e.initCause(lastException); // Legacy constructor lacks cause argument;
            throw e;
        }
    }

    public static class ServerPolicy extends DefaultServerPolicy {}

    public static class GroupPolicy extends DefaultGroupPolicy {}
}

package sapphire.policy.atleastoncerpc;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import sapphire.common.AppExceptionWrapper;
import sapphire.policy.DefaultSapphirePolicy;

// AtLeastOnceRPC: automatically retry RPCs for bounded amount of time
public class AtLeastOnceRPCPolicy extends DefaultSapphirePolicy {

    public static class AtLeastOnceRPCClientPolicy extends DefaultClientPolicy {
        // 5s looks like a reasonable default timeout for production
        private long timeoutMilliSeconds = 5000L;

        public AtLeastOnceRPCClientPolicy() {}

        // for unit test use
        public void setTimeout(long timeoutMilliSeconds) {
            this.timeoutMilliSeconds = timeoutMilliSeconds;
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            long startTime = System.currentTimeMillis();
            Exception lastException = null;
            do { // Retry until timeout expires
                try {
                    return super.onRPC(method, params);
                } catch (AppExceptionWrapper e) {
                    throw e; // Don't retry on application exceptions
                } catch (Exception e) {
                    lastException = e; // So we can throw this after the timeout.
                    continue; // Retry all non-application exceptions, e.g. network failures.
                }
            } while (System.currentTimeMillis() - startTime < timeoutMilliSeconds);
            TimeoutException e =
                    new TimeoutException("Retry timeout exceeded in AtLeastOnceRPCPolicy");
            e.initCause(lastException); // Legacy constructor lacks cause argument;
            throw e;
        }
    }

    public static class AtLeastOnceRPCServerPolicy extends DefaultServerPolicy {}

    public static class AtLeastOnceRPCGroupPolicy extends DefaultGroupPolicy {}
}

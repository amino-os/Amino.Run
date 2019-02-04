package amino.run.policy.atleastoncerpc;

import amino.run.common.AppExceptionWrapper;
import amino.run.policy.DefaultPolicy;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

// AtLeastOnceRPC: automatically retry RPCs for bounded amount of time
public class AtLeastOnceRPCPolicy extends DefaultPolicy {

    public static class AtLeastOnceRPCClientPolicy extends DefaultClientPolicy {
        // 5s looks like a reasonable default timeout for production
        private long timeoutMilliSeconds = 5000L;

        public AtLeastOnceRPCClientPolicy() {}

        // for unit test use
        public void setTimeout(long timeoutMilliSeconds) {
            this.timeoutMilliSeconds = timeoutMilliSeconds;
        }

        @Override
        public Object onRPC(
                String method,
                ArrayList<Object> params,
                String prevDMMethod,
                ArrayList<Object> paramStack)
                throws Exception {
            long startTime = System.currentTimeMillis();
            Exception lastException = null;
            do { // Retry until timeout expires
                try {
                    return super.onRPC(method, params, prevDMMethod, paramStack);
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

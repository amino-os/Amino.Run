package sapphire.policy.dmchain;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import sapphire.policy.DefaultSapphirePolicy;

// AtLeastOnceRPC: automatically retry RPCs for bounded amount of time
public class DMChainRetryPolicy extends DefaultSapphirePolicy {

    public static class DMChainRetryPolicyClientPolicy extends DefaultClientPolicy {
        // 5s looks like a reasonable default timeout for production
        private long timeoutMilliSeconds = 5000L;

        public DMChainRetryPolicyClientPolicy() {}

        // for unit test use
        public void setTimeout(long timeoutMilliSeconds) {
            this.timeoutMilliSeconds = timeoutMilliSeconds;
        }

        private Object doOnRPC(String method, ArrayList<Object> params) throws Exception {
            try{
                return super.onRPC(method, params);
            }catch(Exception e) {
                return this.onRPC(method, params);
            }
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            FutureTask<?> timeoutTask = null;
            final DMChainRetryPolicyClientPolicy clientPolicy = this;
            final String method_ = method;
            final ArrayList<Object> params_ = params;

            timeoutTask = new FutureTask<Object>(new Callable<Object>() {
                @Override
                public Object call() throws Exception{
                    return clientPolicy.doOnRPC(method_, params_);
                }
            });

            new Thread(timeoutTask).start();
            Object result = timeoutTask.get(this.timeoutMilliSeconds, TimeUnit.MILLISECONDS);
            return result;
        }
    }

    public static class DMChainRetryPolicyServerPolicy extends DefaultServerPolicy {}

    public static class DMChainRetryPolicyGroupPolicy extends DefaultGroupPolicy {}
}
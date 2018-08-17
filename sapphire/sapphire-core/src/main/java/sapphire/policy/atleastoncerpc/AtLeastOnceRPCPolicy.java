package sapphire.policy.atleastoncerpc;

import sapphire.policy.DefaultSapphirePolicy;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

// AtLeastOnceRPC: automatically retry RPCs for bounded amount of time
public class AtLeastOnceRPCPolicy extends DefaultSapphirePolicy {

    public static class AtLeastOnceRPCClientPolicy extends DefaultClientPolicy {
        // 5s looks like a reasonable default timeout for production
        private long timeoutMilliSeconds = 500000L;
        private int count = 0;
        private int id = -1;

        public AtLeastOnceRPCClientPolicy() {
            Random rand = new Random();
            this.id = rand.nextInt();
        }

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
            final AtLeastOnceRPCClientPolicy clientPolicy = this;
            final String method_ = method;
            final ArrayList<Object> params_ = params;
            count++;
            System.out.println("id: " + this.id + " atLeastOneRpc: " + this.count);
            return clientPolicy.doOnRPC(method_, params_);
//            timeoutTask = new FutureTask<Object>(new Callable<Object>() {
//                @Override
//                public Object call() throws Exception{
//                    return clientPolicy.doOnRPC(method_, params_);
//                }
//            });
//
//            new Thread(timeoutTask).start();
//            Object result = timeoutTask.get(this.timeoutMilliSeconds, TimeUnit.MILLISECONDS);
//            return result;
        }
    }

    public static class AtLeastOnceRPCServerPolicy extends DefaultServerPolicy {
        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            // This is dummy method to verify DM chain correctly visits here.
            return super.onRPC(method, params);
        }
    }

    public static class AtLeastOnceRPCGroupPolicy extends DefaultGroupPolicy {}
}
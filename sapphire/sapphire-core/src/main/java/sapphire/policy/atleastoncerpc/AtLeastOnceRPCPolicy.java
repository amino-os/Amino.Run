package sapphire.policy.atleastoncerpc;

import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.server.KernelObject;
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
            System.out.println("Successfully set the timeout to : " + this.timeoutMilliSeconds);
        }

        private Object doOnRPC(String method, ArrayList<Object> params) throws Exception {
            try{
                return super.onRPC(method, params);
            }catch(Exception e) {
                return this.onRPC(method, params);
            }
        }

        // TODO (8/27/2018) Remove debugging statements after verification: count, println
        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            FutureTask<?> timeoutTask = null;
            final AtLeastOnceRPCClientPolicy clientPolicy = this;
            final String method_ = method;
            final ArrayList<Object> params_ = params;
            count++;
            timeoutTask = new FutureTask<Object>(new Callable<Object>() {
                @Override
                public Object call() throws Exception{
                    System.out.println("Client) onRPC AtLeastOnceRPC");
                    return clientPolicy.doOnRPC(method_, params_);
                }
            });

            new Thread(timeoutTask).start();
            Object result = timeoutTask.get(this.timeoutMilliSeconds, TimeUnit.MILLISECONDS);
            return result;
        }
    }

    public static class AtLeastOnceRPCServerPolicy extends DefaultServerPolicy {
        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            // TODO (8/27/2018): Remove this code block except onRPC after verification of reference to the same group policy.
            //int koid = this.getGroup().$__getKernelOID().getID();
            //System.out.println("OID of Group Policy this policy (AtLeastOnceRPCServerPolicy) refers to " + koid);
            System.out.println("Server) onRPC at AtLeastOnceRPC called");

            // This is dummy method to verify DM chain correctly visits here.
            return super.onRPC(method, params);
        }

        public void PrintDummyStr(String printThis) {
            System.out.println("PrintDummyStr: " + printThis);
        }
    }

    // TODO (8/27/2018): Remove all methods and variables in group policy after verification.
    public static class AtLeastOnceRPCGroupPolicy extends DefaultGroupPolicy {

        @Override
        public void onCreate(SapphireServerPolicy server) {
            AtLeastOnceRPCServerPolicy serverPolicy = (AtLeastOnceRPCServerPolicy)server;
            serverPolicy.PrintDummyStr("PrintDummyStr: OnCreate at Group policy for AtLeastOneRPC");
            KernelObjectStub stub = (KernelObjectStub) server;
//            System.out.println("At GroupPolicy. Hostname: " + stub.$__getHostname());
        }
    }
}
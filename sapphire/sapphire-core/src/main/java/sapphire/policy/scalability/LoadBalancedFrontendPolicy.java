package sapphire.policy.scalability;


import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.apache.harmony.rmi.common.RMIUtil;

import sapphire.compiler.GlobalStubConstants;
import sapphire.common.AppObjectStub;
import sapphire.kernel.server.KernelServer;
import sapphire.policy.DefaultSapphirePolicy;


/**
 * Created by SrinivasChilveri on 19/2/18.
 * Simple load balancing w/ static number of replicas and no consistency
 */

public class LoadBalancedFrontendPolicy extends DefaultSapphirePolicy {

    /**
     * LoadBalancedFrontend client policy. The client will LoadBalance among the Sapphire Server replica objects.
     * client side DM instance should randomise the order in which it performs round robin against replicas
     * @author SrinivasChilveri
     *
     */
    public static class SimpleRateLimiter {
        private Semaphore semaphore;
        private int maxPermits;
        private TimeUnit timeUnit;
        private int period;
        private ScheduledExecutorService scheduler;

        public static SimpleRateLimiter create(int permits, int period, TimeUnit timeUnit) {
            SimpleRateLimiter limiter = new SimpleRateLimiter(permits, period, timeUnit);
            limiter.schedulePermitReplenishment();
            return limiter;
        }

        private SimpleRateLimiter(int permits, int period, TimeUnit timeUnit) {
            this.semaphore = new Semaphore(permits,true);
            this.maxPermits = permits;
            this.timeUnit = timeUnit;
            this.period = period;
        }

        public boolean tryAcquire() {
            return semaphore.tryAcquire();
        }

        public void stop() {
            scheduler.shutdownNow();
        }

        public void schedulePermitReplenishment() {
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(new Runnable() {
                public void run() { semaphore.release(maxPermits - semaphore.availablePermits());}
            }, 1, period, timeUnit);

        }
    }

    public static class ClientPolicy extends DefaultSapphirePolicy.DefaultClientPolicy {
        private static int     index;
        private static boolean flag = true;
        ArrayList<SapphireServerPolicy> replicaList = null;

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            Object ret = null;

            if(flag){
                replicaList = getGroup().getServers();
                // get all the servers which has replicated Objects only once dynamically added replicas
                // are considered later
                index = (int)(Math.random()*replicaList.size());
                flag = false;
            }
            else {
                if (++index >= replicaList.size()){
                    index = 0;
                }
            }

            ret = ((ServerPolicy)(replicaList.get(index))).onRPC(method,params);
            return ret;
        }
    }

    /**
     * LoadBalancedFrontend server policy. throws .
     * a configurable value for the number of concurrent requests supported per replica per should be provided
     * If the number of concurrent requests against a given replica exceeds that number, requests to that server
     * replica should fail (in the server DM) with an appropriate exception (indicating server overload).
     * @author SrinivasChilveri
     *
     */
    public static class ServerPolicy extends DefaultSapphirePolicy.DefaultServerPolicy {
        private static Logger logger = Logger.getLogger(GroupPolicy.class.getName());
        private static int STATIC_RPS = 2000 ; //currently its hard coded we can read from config or annotations
        private static int PERIOD = 1;
        transient SimpleRateLimiter limiter ;

        @Override
        public void onCreate(SapphireGroupPolicy group) {
            super.onCreate(group);
            this.limiter = SimpleRateLimiter.create(STATIC_RPS, PERIOD, TimeUnit.SECONDS);
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if (limiter.tryAcquire()) { //rps is not reached
                return super.onRPC(method, params);
            } else {
                throw new ServerOverLoadException("The Replica of the SappahireObject on this Kernel Server Over Loaded");
            }
        }
    }

    /**
     * Group policy. creates the configured num of replicas.
     * @author SrinivasChilveri
     *
     */
    public static class GroupPolicy extends DefaultSapphirePolicy.DefaultGroupPolicy {
        private ArrayList<SapphireServerPolicy> nodes;
        private static int STATIC_REPLICAS = 2 ; //currently its hard coded we can read from config or annotations
        private static Logger logger = Logger.getLogger(GroupPolicy.class.getName());

        @Override
        public void addServer(SapphireServerPolicy server) {
            nodes.add(server);
        }

        @Override
        public void onFailure(SapphireServerPolicy server) {
            // TODO
        }

        @Override
        public SapphireServerPolicy onRefRequest() {
            return nodes.get(0);
        }

        @Override
        public ArrayList<SapphireServerPolicy> getServers() {
            return nodes;
        }

        @Override
        public void onCreate(SapphireServerPolicy server) {
            nodes = new ArrayList<SapphireServerPolicy>();
            int count = 1 ;

            try {
                ArrayList<String> regions = sapphire_getRegions();

                for (int i = 1; i < regions.size(); i++) {

                    if (count == STATIC_REPLICAS) {
                        break; //break on reaching the configured replica creation
                    }

                    KernelServer kernelServer = sapphire_getServerRefInRegion(regions.get(i));

                    kernelServer.createSapphireObjectReplica(RMIUtil.getShortName(server.getClass()),
                            RMIUtil.getShortName(getClass()) + GlobalStubConstants.STUB_SUFFIX,
                            $__getKernelOID(), (AppObjectStub) server.sapphire_getAppObject().getObject());

                    count++;
                }

                addServer(server);

                if (count != STATIC_REPLICAS)  {
                    logger.warning("Configured Static replicas are not created may be num of regions are less");
                    logger.warning("The Num of regions are "+ regions.size());
                }

            } catch (Exception e) {
                e.printStackTrace();
                throw new Error("Could not create new group policy");
            }
        }
    }

}
package sapphire.policy.scalability;


import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import sapphire.kernel.common.KernelObjectStub;
import sapphire.policy.SapphirePolicy;


/**
 * Created by SrinivasChilveri on 15/2/18.
 * Simple load balancing w/ static number of replicas and no consistency
 */

public class LoadBalancedFrontendPolicy extends SapphirePolicy {

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

    public static class ClientPolicy extends SapphireClientPolicy {

        protected ServerPolicy server;
        protected GroupPolicy  group;
        private static int     index;
        private static boolean flag = true;
        ArrayList<SapphireServerPolicy> replicaList = null;

        @Override
        public void onCreate(SapphireGroupPolicy group) {
            this.group = (GroupPolicy) group;
        }

        @Override
        public void setServer(SapphireServerPolicy server) {
            this.server = (ServerPolicy)server;
        }

        @Override
        public SapphireServerPolicy getServer() {
            return server;
        }

        @Override
        public SapphireGroupPolicy getGroup() {
            return group;
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            Object ret = null;

            if(flag){
                 replicaList = group.getServers();
                 // get all the servers which has replicated Objects only once dynamically added replicas
                 // are considered later.
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
    public static class ServerPolicy extends SapphireServerPolicy {
        private static Logger logger = Logger.getLogger(GroupPolicy.class.getName());
        private GroupPolicy group;
        private static int STATIC_RPS = 2000 ; //currently its hard coded we can read from config or annotations
        private static int PERIOD = 1;
        transient SimpleRateLimiter limiter ;

        public void dynamicInit()
        {
            this.limiter = SimpleRateLimiter.create(STATIC_RPS, PERIOD, TimeUnit.SECONDS);
        }

        @Override
        public void onCreate(SapphireGroupPolicy group) {
            this.group = (GroupPolicy)group;
            //this.limiter = SimpleRateLimiter.create(STATIC_RPS,TimeUnit.SECONDS);
        }

        @Override
        public SapphireGroupPolicy getGroup() {
            return group;
        }

        @Override
        public void onMembershipChange() {

        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if (limiter.tryAcquire()) { //rps is not reached
                return super.onRPC(method, params);
            } else {
                logger.warning("Trowing Exception on server overload");
                throw new ServerOverLoadException("The Replica of the SappahireObject on this Kernel Server Over Loaded");
            }
        }
        public ServerPolicy onSapphireObjectReplicate() {
            logger.info("onSapphireObjectReplicate is Invoked");
            return (ServerPolicy) this.sapphire_replicate();
        }
        public void onSapphirePin(String region) throws RemoteException {
            logger.info("onSapphirePin is Invoked");
            sapphire_pin(region, "public void sapphire.policy.scalability.LoadBalancedFrontendPolicy$ServerPolicy.dynamicInit()");
        }
    }

    /**
     * Group policy. creates the configured num of replicas.
     * @author SrinivasChilveri
     *
     */
    public static class GroupPolicy extends SapphireGroupPolicy {
        ServerPolicy server;
        private ArrayList<SapphireServerPolicy> nodes;
        private static int STATIC_REPLICAS = 2 ; //currently its hard coded we can read from config or annotations
        private static Logger logger = Logger.getLogger(GroupPolicy.class.getName());



        public void addReplicaServer(SapphireServerPolicy server) {
           nodes.add(server);
        }

        @Override
        public void addServer(SapphireServerPolicy server) {
            this.server = (ServerPolicy) server;
            //nodes.add(server);
        }

        @Override
        public void onFailure(SapphireServerPolicy server) {
            // TODO
        }

        @Override
        public SapphireServerPolicy onRefRequest() {
            return server;
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

                ServerPolicy nodeServer = (ServerPolicy)server;

                for (int i = 1; i < regions.size(); i++) {

                    if (count == STATIC_REPLICAS) {
                        break; //break on reaching the configured replica creation
                    }

                    ServerPolicy replica = nodeServer.onSapphireObjectReplicate();
                    replica.onSapphirePin(regions.get(i));
                    //oms().lookupKernelObject(replica.$__getKernelOID())
                    //replica.$__updateHost(regions.get(0).)
                    ((KernelObjectStub)replica).$__updateHostname(sapphire_getServerInRegion(regions.get(i)));
                    addReplicaServer(replica);
                    count++;
                }

                if (!(((KernelObjectStub)nodeServer).$__getHostname().toString().equals(regions.get(0)))) {
                    nodeServer.onSapphirePin(regions.get(0));
                    ((KernelObjectStub)nodeServer).$__updateHostname(sapphire_getServerInRegion(regions.get(0)));
                }
                else
                {
                    nodeServer.dynamicInit();
                }

                addReplicaServer(nodeServer);
                if (count != STATIC_REPLICAS)  {
                    logger.warning("Configured Static replicas are not created may be num of regions are less");
                    logger.warning("The Num of regions are "+ regions.size());
                }

            } catch (RemoteException e) {
                e.printStackTrace();
                throw new Error("Could not create new group policy because the oms is not available.");
            }
        }

    }

}
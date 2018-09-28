package sapphire.policy.scalability;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import sapphire.app.DMSpec;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.policy.DefaultSapphirePolicy;

/**
 * Created by SrinivasChilveri on 2/19/18. Simple load balancing w/ static number of replicas and no
 * consistency
 */
public class LoadBalancedFrontendPolicy extends DefaultSapphirePolicy {
    public static final int STATIC_REPLICA_COUNT = 2;
    public static final int MAX_CONCURRENT_REQUESTS = 2000;

    /** Configurations for LoadBalancedFrontendPolicy */
    public static class Config implements SapphirePolicyConfig {
        private int maxConcurrentReq = MAX_CONCURRENT_REQUESTS;
        private int replicaCount = STATIC_REPLICA_COUNT;

        public int getMaxConcurrentReq() {
            return maxConcurrentReq;
        }

        public void setMaxConcurrentReq(int maxConcurrentReq) {
            this.maxConcurrentReq = maxConcurrentReq;
        }

        public int getReplicaCount() {
            return replicaCount;
        }

        public void setReplicaCount(int replicaCount) {
            this.replicaCount = replicaCount;
        }

        @Override
        public DMSpec toDMSpec() {
            DMSpec spec = new DMSpec();
            // Use fully qualified class name!
            spec.setName(LoadBalancedFrontendPolicy.class.getName());
            spec.addProperty("maxConcurrentReq", String.valueOf(maxConcurrentReq));
            spec.addProperty("replicaCount", String.valueOf(replicaCount));
            return spec;
        }

        @Override
        public SapphirePolicyConfig fromDMSpec(DMSpec spec) {
            if (!LoadBalancedFrontendPolicy.class.getName().equals(spec.getName())) {
                throw new IllegalArgumentException(
                        String.format(
                                "DM %s is not able to process spec %s",
                                LoadBalancedFrontendPolicy.class.getName(), spec));
            }

            Config config = new Config();
            Map<String, String> properties = spec.getProperties();
            if (properties != null) {
                config.setReplicaCount(Integer.parseInt(properties.get("replicaCount")));
                config.setMaxConcurrentReq(Integer.parseInt(properties.get("maxConcurrentReq")));
            }

            return config;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Config config = (Config) o;
            return maxConcurrentReq == config.maxConcurrentReq
                    && replicaCount == config.replicaCount;
        }

        @Override
        public int hashCode() {
            return Objects.hash(maxConcurrentReq, replicaCount);
        }
    }

    /* @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface LoadBalancedFrontendPolicyConfigAnnotation {
        int maxConcurrentReq() default MAX_CONCURRENT_REQUESTS;

        int replicaCount() default STATIC_REPLICA_COUNT;
    }
    */

    /**
     * LoadBalancedFrontend client policy. The client will LoadBalance among the Sapphire Server
     * replica objects. client side DM instance should randomise the order in which it performs
     * round robin against replicas
     *
     * @author SrinivasChilveri
     */
    public static class ClientPolicy extends DefaultSapphirePolicy.DefaultClientPolicy {
        private int index;
        protected ArrayList<SapphireServerPolicy> replicaList;
        private static Logger logger = Logger.getLogger(ClientPolicy.class.getName());

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            ServerPolicy server = getCurrentServer();
            return server.onRPC(method, params);
        }

        private synchronized ServerPolicy getCurrentServer() throws Exception {

            if (null == replicaList || replicaList.isEmpty()) {
                // get all the servers which has replicated Objects only once
                // dynamically added replicas are considered later
                replicaList = getGroup().getServers();
                index = (int) (Math.random() * 100);
            }
            index = ++index % replicaList.size();
            return (ServerPolicy) replicaList.get(index);
        }
    }

    /**
     * LoadBalancedFrontend server policy. a configurable value for the number of concurrent
     * requests supported per replica should be provided If the number of concurrent requests
     * against a given replica exceeds that number, requests to that server replica should fail (in
     * the server DM) with an appropriate exception (indicating server overload).
     *
     * @author SrinivasChilveri
     */
    public static class ServerPolicy extends DefaultSapphirePolicy.DefaultServerPolicy {
        private static Logger logger = Logger.getLogger(ServerPolicy.class.getName());
        // we can read from default config or annotations
        protected int maxConcurrentReq = MAX_CONCURRENT_REQUESTS;
        protected Semaphore limiter;

        @Override
        public void onCreate(SapphireGroupPolicy group, Map<String, DMSpec> dmSpecMap) {
            super.onCreate(group, dmSpecMap);
            if (dmSpecMap != null) {
                DMSpec spec = dmSpecMap.get(LoadBalancedFrontendPolicy.class.getName());
                if ((spec != null) && (spec.getProperty("maxConcurrentReq") != null)) {
                    this.maxConcurrentReq = Integer.valueOf(spec.getProperty("maxConcurrentReq"));
                }
            }

            if (this.limiter == null) {
                this.limiter = new Semaphore(this.maxConcurrentReq, true);
            }
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            boolean acquired = false;
            acquired = limiter.tryAcquire();
            if (acquired) { // concurrent requests count not reached
                try {
                    return super.onRPC(method, params);
                } finally {
                    limiter.release();
                }
            } else {
                logger.warning(
                        "Throwing Exception on server overload on reaching the concurrent requests count"
                                + maxConcurrentReq);
                throw new ServerOverLoadException(
                        "The Replica of the SappahireObject on this Kernel Server Over Loaded on reaching the concurrent requests count "
                                + maxConcurrentReq);
            }
        }
    }

    /**
     * Group policy. creates the configured num of replicas.
     *
     * @author SrinivasChilveri
     */
    public static class GroupPolicy extends DefaultSapphirePolicy.DefaultGroupPolicy {
        private static Logger logger = Logger.getLogger(GroupPolicy.class.getName());
        private int replicaCount = STATIC_REPLICA_COUNT; // we can read from config or annotations

        @Override
        public void onCreate(SapphireServerPolicy server, Map<String, DMSpec> dmSpecMap)
                throws RemoteException {
            super.onCreate(server, dmSpecMap);

            if (dmSpecMap != null) {
                DMSpec spec = dmSpecMap.get(LoadBalancedFrontendPolicy.class.getName());
                if (spec != null) {
                    if (spec.getProperty("replicaCount") != null) {
                        this.replicaCount = Integer.valueOf(spec.getProperty("replicaCount"));
                    }
                }
            }
            // count is compared below < STATIC_REPLICAS-1 excluding the present server
            int count = 0;
            int numnodes = 0; // num of nodes/servers in the selected region

            /* Creation of group happens when the first instance of sapphire object is
            being created. Loop through all the kernel servers and replicate the
            sapphire objects on them based on the static replica count */
            try {

                /* Find the current region and the kernel server on which this first instance of
                sapphire object is being created. And try to replicate the
                sapphire objects in the same region(excluding this kernel server) */
                String region = server.sapphire_getRegion();
                InetSocketAddress addr =
                        server.sapphire_locate_kernel_object(server.$__getKernelOID());

                ArrayList<InetSocketAddress> kernelServers = sapphire_getServersInRegion(region);

                /* Create the replicas on different kernelServers belongs to same region*/
                if (kernelServers != null) {

                    kernelServers.remove(addr);
                    numnodes = kernelServers.size();

                    for (count = 0; count < numnodes && count < replicaCount - 1; count++) {
                        ServerPolicy replica = (ServerPolicy) server.sapphire_replicate();
                        replica.sapphire_pin_to_server(kernelServers.get(count));
                        updateReplicaHostName(replica, kernelServers.get(count));
                    }
                }

                /* If the replicas created are less than the number of replicas configured,
                log a warning message */
                if (count != replicaCount - 1) {
                    logger.severe(
                            "Configured replicas count: "
                                    + replicaCount
                                    + ", created replica count : "
                                    + count
                                    + "insufficient servers in region "
                                    + numnodes
                                    + "to create required replicas");
                    throw new Error(
                            "Configured replicas count: "
                                    + replicaCount
                                    + ", created replica count : "
                                    + count);
                }

            } catch (RemoteException e) {
                logger.severe("Received RemoteException may be oms is down ");
                throw new Error(
                        "Could not create new group policy because the oms is not available.", e);
            } catch (SapphireObjectNotFoundException e) {
                throw new Error("Failed to find sapphire object.", e);
            } catch (SapphireObjectReplicaNotFoundException e) {
                throw new Error("Failed to find sapphire object replica.", e);
            }
        }
    }
}

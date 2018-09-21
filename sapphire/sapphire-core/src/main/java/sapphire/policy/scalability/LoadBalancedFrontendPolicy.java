package sapphire.policy.scalability;

import static sapphire.common.Utils.getAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.policy.DefaultSapphirePolicy;

/**
 * Created by SrinivasChilveri on 2/19/18. Simple load balancing w/ static number of replicas and no
 * consistency
 */
public class LoadBalancedFrontendPolicy extends DefaultSapphirePolicy {

    static final int STATIC_REPLICA_COUNT = 2;
    static final int MAX_CONCURRENT_REQUESTS = 2000;

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface LoadBalancedFrontendPolicyConfigAnnotation {
        int maxconcurrentReq() default MAX_CONCURRENT_REQUESTS;

        int replicacount() default STATIC_REPLICA_COUNT;
    }

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
        protected int maxConcurrentReq =
                MAX_CONCURRENT_REQUESTS; // we can read from default config or annotations
        protected Semaphore limiter;

        @Override
        public void onCreate(SapphireGroupPolicy group, Annotation[] annotations) {
            super.onCreate(group, annotations);
            LoadBalancedFrontendPolicyConfigAnnotation annotation =
                    getAnnotation(annotations, LoadBalancedFrontendPolicyConfigAnnotation.class);
            if (annotation != null) {
                this.maxConcurrentReq = annotation.maxconcurrentReq();
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
        public void onCreate(SapphireServerPolicy server, Annotation[] annotations) {
            LoadBalancedFrontendPolicyConfigAnnotation annotation =
                    getAnnotation(annotations, LoadBalancedFrontendPolicyConfigAnnotation.class);

            if (annotation != null) {
                this.replicaCount = annotation.replicacount();
            }

            /* Creation of group happens when the first instance of sapphire object is
            being created. Loop through all the kernel servers and replicate the
            sapphire objects on them based on the static replica count */
            try {
                // Initialize and consider this server
                addServer(server);

                List<InetSocketAddress> kernelServers =
                        GlobalKernelReferences.nodeServer.oms.getServers();
                if (kernelServers == null || kernelServers.size() == 0) {
                    throw new IllegalStateException("Cannot find any kernel server.");
                }

                if (kernelServers.size() < replicaCount) {
                    logger.warning(
                            String.format(
                                    "The number of kernel servers (%d) is less than "
                                            + "the number of replicas (%d). We have to allocate multiple replicas "
                                            + "to one kernel server.",
                                    kernelServers.size(), replicaCount));
                }

                InetSocketAddress addr =
                        server.sapphire_locate_kernel_object(server.$__getKernelOID());
                int startingIndex = kernelServers.indexOf(addr) + 1;
                for (int i = 0; i < replicaCount - 1; i++) {
                    ServerPolicy replica = (ServerPolicy) server.sapphire_replicate();
                    InetSocketAddress dst =
                            kernelServers.get((startingIndex + i) % kernelServers.size());
                    replica.sapphire_pin_to_server(dst);
                    ((KernelObjectStub) replica).$__updateHostname(dst);
                }
            } catch (NotBoundException e) {
                throw new Error(
                        "Could not create new group policy because the oms is not available.", e);
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

package sapphire.policy.scalability;

import static sapphire.common.Utils.getAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.policy.util.ResettableTimer;

/**
 * ScaleUpFrontEnd DM: Load-balancing w/ dynamic allocation of replicas and no consistency Created
 * by Venugopal Reddy K 00900280 on 2/18/18.
 */
public class ScaleUpFrontendPolicy extends LoadBalancedFrontendPolicy {
    static final int REPLICA_CREATE_MIN_TIME_IN_MSEC = 100;

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface ScaleUpFrontendPolicyConfigAnnotation {
        int replicationRateInMs() default REPLICA_CREATE_MIN_TIME_IN_MSEC;

        LoadBalancedFrontendPolicyConfigAnnotation loadbalanceConfig();
    }

    public static class ClientPolicy extends LoadBalancedFrontendPolicy.ClientPolicy {
        private final AtomicInteger replicaListSyncCtr = new AtomicInteger();

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if (0 == (replicaListSyncCtr.getAndIncrement() % 100)) {
                synchronized (this) {
                    // TODO: Should device a mechanism to fetch the latest replica list
                    replicaList = null;
                }
            }

            return super.onRPC(method, params);
        }
    }

    public static class ServerPolicy extends LoadBalancedFrontendPolicy.ServerPolicy {
        private int replicationRateInMs = REPLICA_CREATE_MIN_TIME_IN_MSEC; // for n milliseconds
        private int replicaCount = 1; // 1 replica in n milliseconds
        private Semaphore replicaCreateLimiter;
        private transient volatile ResettableTimer timer; // Timer for limiting

        @Override
        public void onCreate(SapphireGroupPolicy group, Annotation[] annotations) {
            Annotation[] lbConfigAnnotations = annotations;
            ScaleUpFrontendPolicyConfigAnnotation annotation =
                    (ScaleUpFrontendPolicyConfigAnnotation)
                            getAnnotation(annotations, ScaleUpFrontendPolicyConfigAnnotation.class);
            if (annotation != null && null != annotation.loadbalanceConfig()) {
                lbConfigAnnotations = new Annotation[] {annotation.loadbalanceConfig()};
            }

            super.onCreate(group, lbConfigAnnotations);

            if (annotation != null) {
                replicationRateInMs = annotation.replicationRateInMs();
            }

            replicaCreateLimiter = new Semaphore(replicaCount, true);

            timer =
                    new ResettableTimer(
                            new TimerTask() {
                                public void run() {
                                    replicaCreateLimiter.release(
                                            replicaCount - replicaCreateLimiter.availablePermits());
                                    scaleDown();
                                }
                            },
                            replicationRateInMs);
            timer.start();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            timer.cancel();
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            try {
                return super.onRPC(method, params);
            } catch (ServerOverLoadException e) {
                if (!replicaCreateLimiter.tryAcquire()) {
                    throw new ScaleUpException(
                            "Replica creation rate exceeded for this sapphire object");
                }

                ((GroupPolicy) getGroup()).scaleUpReplica(sapphire_getRegion());
                throw e;
            }
        }

        private void scaleDown() {

            /* When the load at a given replica drops to approximately p * (m-2)/m
            (where m is the current number of replicas, and p is the maximum concurrency
            setting per replica), then the server-side DM for that replica should remove one
            replica (randomly chosen). This is because there are in theory two more replicas
            than required, so one can be removed. The number of replicas should not be
            reduced below 2 (in case one fails).
             */
            ArrayList<SapphireServerPolicy> replicaServers;
            try {
                replicaServers = getGroup().getServers();
            } catch (RemoteException e) {
                return;
            }

            double currentReplicas = replicaServers.size();
            if (currentReplicas <= 2) {
                // Scale down shouldn't happen if the replica count is less than or equal to 2
                return;
            }

            double maxConcurrencyLimit = maxConcurrentReq;
            double currentLoad = maxConcurrencyLimit - limiter.availablePermits();
            if (currentLoad < ((maxConcurrencyLimit * (currentReplicas - 2)) / currentReplicas)) {
                // delete this replica
                try {
                    ((GroupPolicy) getGroup()).scaleDownReplica(this);
                    synchronized (this) {
                        timer.cancel();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (ScaleDownException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class GroupPolicy extends LoadBalancedFrontendPolicy.GroupPolicy {
        private int replicationRateInMs = REPLICA_CREATE_MIN_TIME_IN_MSEC; // n milliseconds
        private int replicaCount = 1; // 1 replica in n milliseconds
        private Semaphore replicaCreateLimiter;
        private transient ResettableTimer timer; // Timer for limiting

        @Override
        public void onCreate(SapphireServerPolicy server, Annotation[] annotations) {
            Annotation[] lbConfigAnnotations = annotations;
            ScaleUpFrontendPolicyConfigAnnotation annotation =
                    (ScaleUpFrontendPolicyConfigAnnotation)
                            getAnnotation(annotations, ScaleUpFrontendPolicyConfigAnnotation.class);
            if (annotation != null && null != annotation.loadbalanceConfig()) {
                lbConfigAnnotations = new Annotation[] {annotation.loadbalanceConfig()};
            }

            super.onCreate(server, lbConfigAnnotations);

            if (annotation != null) {
                replicationRateInMs = annotation.replicationRateInMs();
            }
            if (replicaCreateLimiter == null) {
                replicaCreateLimiter = new Semaphore(replicaCount, true);
            }

            timer =
                    new ResettableTimer(
                            new TimerTask() {
                                public void run() {
                                    replicaCreateLimiter.release(
                                            replicaCount - replicaCreateLimiter.availablePermits());
                                }
                            },
                            replicationRateInMs);
            timer.start();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            timer.cancel();
        }

        public synchronized void scaleUpReplica(String region)
                throws ScaleUpException, RemoteException {
            if (!replicaCreateLimiter.tryAcquire()) {
                throw new ScaleUpException(
                        "Replica creation rate exceeded for this sapphire object.");
            }

            /* Get the list of available servers in region */
            ArrayList<InetSocketAddress> fullKernelList;
            fullKernelList = sapphire_getServersInRegion(region);
            if (null == fullKernelList) {
                throw new ScaleUpException("Scaleup failed. Couldn't fetch kernel server list.");
            }

            /* Get the list of servers on which replicas already exist */
            ArrayList<InetSocketAddress> sappObjReplicatedKernelList =
                    new ArrayList<InetSocketAddress>();
            for (SapphireServerPolicy tmp : getServers()) {
                sappObjReplicatedKernelList.add(((KernelObjectStub) tmp).$__getHostname());
            }

            /* Remove the servers which already have replicas of this sapphire object */
            fullKernelList.removeAll(sappObjReplicatedKernelList);

            if (!fullKernelList.isEmpty()) {
                /* create a replica on the first server in the list */
                SapphireServerPolicy server = getServers().get(0);
                SapphireServerPolicy replica = server.sapphire_replicate();
                try {
                    replica.sapphire_pin_to_server(fullKernelList.get(0));
                } catch (SapphireObjectNotFoundException e) {
                    throw new ScaleUpException(
                            "Failed to find sapphire object. Probably deleted.", e);
                } catch (SapphireObjectReplicaNotFoundException e) {
                    throw new ScaleUpException(
                            "Failed to find replicate sapphire object. Probably deleted.", e);
                }
                ((KernelObjectStub) replica).$__updateHostname(fullKernelList.get(0));
            } else {
                throw new ScaleUpException(
                        "Replica cannot be created for this sapphire object. All kernel servers have its replica.");
            }
        }

        public synchronized void scaleDownReplica(SapphireServerPolicy server)
                throws RemoteException, ScaleDownException {
            ArrayList<SapphireServerPolicy> serverList = getServers();

            if (2 >= serverList.size()) {
                throw new ScaleDownException(
                        "Cannot scale down. Current replica count is " + serverList.size());
            }

            SapphireServerPolicy serverToRemove = null;
            for (SapphireServerPolicy serverPolicyStub : serverList) {
                if (serverPolicyStub.$__getKernelOID().equals(server.$__getKernelOID())) {
                    serverToRemove = serverPolicyStub;
                    break;
                }
            }

            if (serverToRemove != null) {
                try {
                    sapphire_deleteReplica(serverToRemove);
                    removeServer(serverToRemove);
                } catch (SapphireObjectNotFoundException e) {
                    throw new ScaleDownException(
                            "Scale down failed. Sapphire object not found.", e);
                } catch (SapphireObjectReplicaNotFoundException e) {
                    throw new ScaleDownException(
                            "Scale down failed. Sapphire object not found.", e);
                }
            }
        }
    }
}

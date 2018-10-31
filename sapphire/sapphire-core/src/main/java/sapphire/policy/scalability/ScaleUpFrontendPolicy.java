package sapphire.policy.scalability;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
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

    /** Configurations for ScaleUpFrontendPolicy */
    public static class Config implements SapphirePolicyConfig {
        private int replicationRateInMs = REPLICA_CREATE_MIN_TIME_IN_MSEC;

        public int getReplicationRateInMs() {
            return replicationRateInMs;
        }

        public void setReplicationRateInMs(int replicationRateInMs) {
            this.replicationRateInMs = replicationRateInMs;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Config config = (Config) o;
            return replicationRateInMs == config.replicationRateInMs;
        }

        @Override
        public int hashCode() {
            return Objects.hash(replicationRateInMs);
        }
    }

    public static class ClientPolicy extends LoadBalancedFrontendPolicy.ClientPolicy {
        private final AtomicInteger replicaListSyncCtr = new AtomicInteger();
        private static Logger logger = Logger.getLogger(ClientPolicy.class.getName());

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if (0 == (replicaListSyncCtr.getAndIncrement() % 100)) {
                synchronized (this) {
                    // TODO: Should device a mechanism to fetch the latest replica list
                    replicaList = null;
                }
            }

            final int MAX_RETRY = 5;
            int retryCnt = 1, waitInMilliseconds = 50;
            Exception lastException;
            do {
                try {
                    return super.onRPC(method, params);
                } catch (Exception e) {
                    lastException = e;
                    logger.warning(
                            String.format(
                                    "Failed to execute method %s: %s", method, e.getMessage()));
                    Thread.sleep(waitInMilliseconds);
                    waitInMilliseconds <<= 1;
                }
            } while (++retryCnt <= MAX_RETRY);

            throw new Exception(
                    String.format("Failed to execute method %s after retries", method),
                    lastException);

        }
    }

    public static class ServerPolicy extends LoadBalancedFrontendPolicy.ServerPolicy {
        private static Logger logger = Logger.getLogger(ServerPolicy.class.getName());
        private int replicationRateInMs = REPLICA_CREATE_MIN_TIME_IN_MSEC; // for n milliseconds
        private int replicaCount = 1; // 1 replica in n milliseconds
        private Semaphore replicaCreateLimiter;
        private transient volatile ResettableTimer timer; // Timer for limiting

        @Override
        public void onCreate(
                SapphireGroupPolicy group, Map<String, SapphirePolicyConfig> configMap) {
            super.onCreate(group, configMap);

            if (configMap != null) {
                SapphirePolicyConfig config =
                        configMap.get(ScaleUpFrontendPolicy.Config.class.getName());
                if (config != null) {
                    replicationRateInMs = ((Config) config).getReplicationRateInMs();
                }
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
                    logger.warning("Replica creation rate exceeded for this sapphire object.");
                    throw new ScaleUpException(
                            "Replica creation rate exceeded for this sapphire object.");
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
                } catch (Exception e) {
                    logger.warning("Replica scale down failed. Will try later.");
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
        public void onCreate(
                String region,
                SapphireServerPolicy server,
                Map<String, SapphirePolicyConfig> configMap)
                throws RemoteException {
            super.onCreate(region, server, configMap);

            if (configMap != null) {
                SapphirePolicyConfig config =
                        configMap.get(ScaleUpFrontendPolicy.Config.class.getName());
                if (config != null) {
                    replicationRateInMs = ((Config) config).getReplicationRateInMs();
                }
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
        public void onDestroy() throws RemoteException {
            super.onDestroy();
            timer.cancel();
        }

        protected SapphireServerPolicy addReplica(
                SapphireServerPolicy replicaSource, InetSocketAddress dest)
                throws RemoteException, SapphireObjectNotFoundException,
                        SapphireObjectReplicaNotFoundException {
            SapphireServerPolicy replica =
                    replicaSource.sapphire_replicate(replicaSource.getProcessedPolicies());
            try {
                replica.sapphire_pin_to_server(replica, dest);
                updateReplicaHostName(replica, dest);
            } catch (Exception e) {
                try {
                    removeReplica(replica);
                } catch (Exception innerException) {
                }
                throw e;
            }
            return replica;
        }

        public void scaleUpReplica(String region) throws ScaleUpException, RemoteException {
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
            ArrayList<SapphireServerPolicy> servers = getServers();
            for (SapphireServerPolicy tmp : servers) {
                sappObjReplicatedKernelList.add(((KernelObjectStub) tmp).$__getHostname());
            }

            /* Remove the servers which already have replicas of this sapphire object */
            fullKernelList.removeAll(sappObjReplicatedKernelList);

            if (!fullKernelList.isEmpty()) {
                try {
                    /* create a replica on the first server in the list */
                    addReplica(servers.get(0), fullKernelList.get(0));
                } catch (SapphireObjectNotFoundException e) {
                    throw new ScaleUpException(
                            "Failed to find sapphire object. Probably deleted.", e);
                } catch (SapphireObjectReplicaNotFoundException e) {
                    throw new ScaleUpException(
                            "Failed to find replicate sapphire object. Probably deleted.", e);
                }
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
                    removeReplica(serverToRemove);
                } catch (SapphireObjectNotFoundException e) {
                    throw new ScaleDownException(
                            "Scale down failed. Sapphire object not found.", e);
                } catch (SapphireObjectReplicaNotFoundException e) {
                    throw new ScaleDownException(
                            "Scale down failed. Sapphire object not found.", e);
                } catch (RuntimeException e) {
                    throw new ScaleDownException("Scale down failed. Replica deletion failed.", e);
                }
            }
        }
    }
}

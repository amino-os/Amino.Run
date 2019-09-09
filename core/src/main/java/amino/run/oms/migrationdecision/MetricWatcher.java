package amino.run.oms.migrationdecision;

import amino.run.common.MicroServiceID;
import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.common.ReplicaID;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.KernelServerNotFoundException;
import amino.run.kernel.metric.NodeMetric;
import amino.run.kernel.metric.RPCMetric;
import amino.run.oms.KernelServerManager;
import amino.run.oms.MicroServiceManager;
import amino.run.policy.DefaultPolicy;
import amino.run.policy.MigrationNotification;
import amino.run.policy.util.ResettableTimer;
import amino.run.runtime.EventHandler;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Class watch over metric collected in {@link MicroServiceManager} and {@link KernelServerManager}.
 * For evaluating best candidate for migration it maintains a heap based on time consumption in
 * Micro service replica.
 *
 * @author AmitRoushan
 */
public class MetricWatcher {
    private static final Logger logger = Logger.getLogger(MetricWatcher.class.getName());
    private MicroServiceManager microServiceManager;
    private KernelServerManager serverManager;
    private MigrationPredictor predictor;
    private transient ResettableTimer timer;
    private TreeSet<AbstractMap.SimpleEntry<ReplicaID, Long>>
            replicaHeap; // should only used in getBestCandidates for selecting best microservice
    // replica for migration
    private final int MAX_TOP_CANDIDATE = 3;
    /** Metric watch timer interval */
    public final int METRIC_WATCH_INTERVAL = 30000; // in milli seconds

    public MetricWatcher(
            KernelServerManager serverManager, MicroServiceManager microServiceManager) {
        this.microServiceManager = microServiceManager;
        this.serverManager = serverManager;
        this.predictor = new MigrationPredictor(serverManager, microServiceManager);
        replicaHeap =
                new TreeSet<AbstractMap.SimpleEntry<ReplicaID, Long>>(
                        new Comparator<AbstractMap.SimpleEntry<ReplicaID, Long>>() {
                            @Override
                            public int compare(
                                    AbstractMap.SimpleEntry<ReplicaID, Long> replicaStat1,
                                    AbstractMap.SimpleEntry<ReplicaID, Long> replicaStat2) {
                                if (replicaStat1.getValue() < replicaStat2.getValue()) return 1;
                                else if (replicaStat1.getValue() > replicaStat2.getValue())
                                    return -1;
                                return 0;
                            }
                        });
        timer =
                new ResettableTimer(
                        new TimerTask() {
                            public void run() {
                                // get replica list
                                List<ReplicaID> replicaIDs = getReplicaIDs();
                                if (replicaIDs.isEmpty()) {
                                    logger.info("Replica not available for migration");
                                    timer.reset();
                                    return;
                                }

                                // get best candidates for migration
                                List<ReplicaID> bestCandidateReplicaIds =
                                        getBestCandidates(replicaIDs);
                                logger.fine(
                                        String.format(
                                                "Chosen Replicas [%s]",
                                                Arrays.toString(
                                                        bestCandidateReplicaIds.toArray())));

                                // try to migrate one of best replica candidate
                                for (ReplicaID bestCandidateReplicaId : bestCandidateReplicaIds) {
                                    // TODO check if recently migrated (check for cool down
                                    //  time)

                                    try {
                                        if (tryMigrate(bestCandidateReplicaId)) {
                                            break;
                                        }
                                    } catch (MicroServiceNotFoundException e) {
                                        logger.warning(
                                                String.format(
                                                        "Skipping Replica [%s] for migration as micro service not found",
                                                        bestCandidateReplicaId));
                                        e.printStackTrace();
                                    } catch (MicroServiceReplicaNotFoundException e) {
                                        logger.warning(
                                                String.format(
                                                        "Skipping Replica [%s] for migration as micro service replica not found",
                                                        bestCandidateReplicaId));
                                        e.printStackTrace();
                                    }
                                }

                                // reset timer if no migration
                                timer.reset();
                            }
                        },
                        METRIC_WATCH_INTERVAL);
    }

    /** Starts metric watcher */
    public void start() {
        timer.start();
    }

    /**
     * Try to migrate replica to best available kernel server and return true if successful
     *
     * @param replicaID micro service replica ID
     * @return true if migration success
     * @throws MicroServiceNotFoundException
     * @throws MicroServiceReplicaNotFoundException
     */
    private boolean tryMigrate(ReplicaID replicaID)
            throws MicroServiceNotFoundException, MicroServiceReplicaNotFoundException {
        // get replica instance event handler
        EventHandler replica = microServiceManager.getReplicaDispatcher(replicaID);
        InetSocketAddress currentKernelServer = replica.getHost();

        // predict new kernel server
        InetSocketAddress kernelServer =
                predictor.getBestKernelServer(replicaID, currentKernelServer);
        logger.fine(
                String.format(
                        "Replica [%s] should migrate from %s to %s",
                        replicaID, currentKernelServer, kernelServer));

        // skipping migration if same kernel server selected
        if (kernelServer.equals(currentKernelServer)) {
            logger.info(
                    "Skipping migration as predicted kernel server is same as replica kernel server");
            return false;
        }

        // trigger migration
        return triggerMigration(replicaID, kernelServer);
    }

    /**
     * Select best candidate based on processing time among available miceroserivce replica
     *
     * @param replicas list for microservice replica
     * @return list to microservice replica
     */
    private synchronized List<ReplicaID> getBestCandidates(List<ReplicaID> replicas) {
        List<ReplicaID> bestCandidates = new ArrayList<ReplicaID>();

        // calculate processing time for each replica and add them in sorted
        // heap
        for (ReplicaID replicaID : replicas) {
            try {
                replicaHeap.add(
                        new AbstractMap.SimpleEntry<ReplicaID, Long>(
                                replicaID, avgProcessingTime(replicaID)));
            } catch (MicroServiceNotFoundException e) {
                logger.warning(
                        String.format(
                                "Skipping Replica [%s] evaluation as micro service not found",
                                replicaID));
                e.printStackTrace();
            } catch (MicroServiceReplicaNotFoundException e) {
                logger.warning(
                        String.format(
                                "Skipping Replica [%s] evaluation as micro service replica not found",
                                replicaID));
                e.printStackTrace();
            } catch (KernelServerNotFoundException e) {
                logger.warning(
                        String.format(
                                "Skipping Replica [%s] evaluation as micro service replica's kernel server not available",
                                replicaID));
                e.printStackTrace();
            }
        }

        // add and print best replica and their processing time in sorted order
        for (AbstractMap.SimpleEntry<ReplicaID, Long> replicaStat : replicaHeap) {
            logger.fine(String.format("Replica status : [%s]", replicaStat));
            if (bestCandidates.size() <= MAX_TOP_CANDIDATE) {
                bestCandidates.add(replicaStat.getKey());
            }
        }

        // clear replica heap
        replicaHeap.clear();
        return bestCandidates;
    }

    /**
     * Migrate replica to intended kernel server
     *
     * @param replicaID micro service replica ID
     * @param kernelServer kernel server address
     * @return return true if migration success
     */
    private boolean triggerMigration(ReplicaID replicaID, InetSocketAddress kernelServer) {
        MicroServiceID microServiceID = replicaID.getOID();

        // start migration
        try {
            KernelOID koid = microServiceManager.getRootGroupId(microServiceID);
            EventHandler groupPolicyEventHandler =
                    microServiceManager.getGroupDispatcher(microServiceID, koid);
            DefaultPolicy.DefaultGroupPolicy replicaGroupPolicy =
                    (DefaultPolicy.DefaultGroupPolicy) groupPolicyEventHandler.getObjects().get(0);
            replicaGroupPolicy.onNotification(new MigrationNotification(replicaID, kernelServer));
        } catch (MicroServiceNotFoundException e) {
            logger.warning(
                    String.format(
                            "Failed to migrate replica [%s] as micro service not found",
                            replicaID));
            e.printStackTrace();
            return false;
        } catch (RemoteException e) {
            logger.warning(
                    String.format(
                            "Failed to migrate replica [%s] with remote exception : %s",
                            replicaID, e.getMessage()));
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Calculate and return average processing time of replica on current kernel server
     *
     * @param replicaID
     * @return average processing time
     * @throws MicroServiceNotFoundException
     * @throws MicroServiceReplicaNotFoundException
     * @throws KernelServerNotFoundException
     */
    private long avgProcessingTime(ReplicaID replicaID)
            throws MicroServiceNotFoundException, MicroServiceReplicaNotFoundException,
                    KernelServerNotFoundException {
        /*
           To measure time consumption by vertex Vk we use the following formula
           t(k) = T(k) + summation { ( S(k,i) + S(i,k) ) / R(k,i) + L(k,i) }
           where
           t(k) => processing time of replica on kernel server k
           T(k) => elapsed time of replica at kernel server k
           S(k,i) => size of data transferred from kernel server k to kernel client i
           S(i,k) => size of data transferred from kernel client i to kernel server k
           R(k,i) => transfer rate between kernel client i to kernel server k
           L(k,i) => latency between kernel client i to kernel server k
        */

        // get current kernel server for replica
        InetSocketAddress currentKernelServer =
                microServiceManager.getReplicaDispatcher(replicaID).getHost();

        // get metrics stat of RPC processing time from different kernel client
        // TODO currently considering processing time from different kernel client for simplicity.
        //  Need to get processing time within micro service replica
        Map<InetSocketAddress, RPCMetric> replicaMetrics =
                microServiceManager.getMicroServiceMetric(replicaID);

        // calculate time factor impact on RPC with latency and bandwidth
        long totElapsedTime = 0;
        long rttSum = 0;
        long dataSize;
        NodeMetric nodeMetric;
        Map<InetSocketAddress, NodeMetric> ksMetric;
        for (Map.Entry<InetSocketAddress, RPCMetric> replicaMetric : replicaMetrics.entrySet()) {
            totElapsedTime += replicaMetric.getValue().elapsedTime;

            dataSize = replicaMetric.getValue().dataSize;
            // kernel server metrics for which RPC data is getting processed
            try {
                ksMetric = serverManager.getKernelServerMetric(replicaMetric.getKey());
            } catch (KernelServerNotFoundException e) {
                // typical scenario like end user application(kvstore, minnieTwitter) where kernel
                // client is utilized by micro service
                // but kernel server do not run. In such scenario RPC metrics are collected against
                // kernel client
                // but as the client is not part of kernel server so no node metric are collected.
                // Hence ignoring time computation
                logger.info(
                        String.format(
                                "skipping kernel client [%s] not part of kernel server",
                                replicaMetric.getKey()));
                continue;
            }

            nodeMetric = ksMetric.get(currentKernelServer);
            rttSum += (dataSize / nodeMetric.rate + nodeMetric.latency);
        }

        long avgElapsedTime = 0;
        long avgRtt = 0;
        if (!replicaMetrics.isEmpty()) {
            avgElapsedTime = totElapsedTime / replicaMetrics.size();
            avgRtt = rttSum / replicaMetrics.size();
        }

        return avgElapsedTime + avgRtt;
    }

    /**
     * Return list of replica IDs
     *
     * @return microservice replica ID list
     */
    private List<ReplicaID> getReplicaIDs() {
        List<ReplicaID> replicaIDs = new ArrayList<ReplicaID>();
        List<MicroServiceID> microServiceIDS;
        try {
            microServiceIDS = microServiceManager.getAllMicroServices();

        } catch (RemoteException e) {
            logger.warning(
                    "Failed to get micro service replicas. Skipping replicas in migration evaluation");
            e.printStackTrace();
            return replicaIDs;
        }

        // TODO Assuming all replica can migrate. In future, user can
        //  restrict replica migration to specific set of replica.
        for (MicroServiceID microServiceID : microServiceIDS) {
            try {
                replicaIDs.addAll(microServiceManager.getReplicaIDs(microServiceID));
            } catch (MicroServiceNotFoundException e) {
                logger.warning(
                        String.format(
                                "MicroService [%s] not found. Skipping for migration evaluation",
                                microServiceID));
            }
        }
        return replicaIDs;
    }

    /** Stop metric watcher timer */
    public void stop() {
        timer.cancel();
    }
}

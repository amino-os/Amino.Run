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
import java.util.*;
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
    private TreeSet<AbstractMap.SimpleEntry<ReplicaID, Long>> replicaHeap;
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
                                List<ReplicaID> replicaIDs = getMigrationReplicas();
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
                                    if (migrate(bestCandidateReplicaId)) {
                                        timer.reset();
                                        return;
                                    }
                                }
                            }
                        },
                        METRIC_WATCH_INTERVAL);
    }

    /** Starts metric watcher */
    public void start() {
        timer.start();
    }

    private boolean migrate(ReplicaID replicaID) {
        InetSocketAddress currentKernelServer;
        try {
            // get replica instance event handler
            EventHandler replica = microServiceManager.getReplicaDispatcher(replicaID);
            currentKernelServer = replica.getHost();
        } catch (Exception e) {
            logger.warning("Failed to get replica dispatcher");
            e.printStackTrace();
            return false;
        }

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

    private synchronized List<ReplicaID> getBestCandidates(List<ReplicaID> replicas) {
        List<ReplicaID> bestCandidates = new ArrayList<ReplicaID>();

        // calculate processing time for each replica and add them in sorted
        // heap
        for (ReplicaID replicaID : replicas) {
            try {
                replicaHeap.add(
                        new AbstractMap.SimpleEntry<ReplicaID, Long>(
                                replicaID, aggregatedProcessingTime(replicaID)));
            } catch (MicroServiceNotFoundException e) {
                logger.warning(
                        String.format(
                                "Skipping Replica [%s] evaluation as micro service not available",
                                replicaID));
                e.printStackTrace();
            } catch (MicroServiceReplicaNotFoundException e) {
                logger.warning(
                        String.format(
                                "Skipping Replica [%s] evaluation as micro service replica not available",
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
                            "Failed to migrate replica [%s] as micro service not available",
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

    private long aggregatedProcessingTime(ReplicaID replicaID)
            throws MicroServiceNotFoundException, MicroServiceReplicaNotFoundException,
                    KernelServerNotFoundException {
        /*
           To measure time consumption by vertex Vk we use the following formula
           tk = Tk,posk+i + summation { (Ski+Sik)/Rposk,posi + Lposk,posi }
        */

        // get current kernel server for replica
        InetSocketAddress replicaKernelServer =
                microServiceManager.getReplicaDispatcher(replicaID).getHost();
        // get metric stat for current replica kernel server
        Map<InetSocketAddress, NodeMetric> replicaNodeMetric =
                serverManager.getKernelServerMetric(replicaKernelServer);

        // get metrics stat of RPC processing time from different kernel client
        // TODO currently considering processing time from different kernel client for simplicity.
        //  Need to get processing time within micro service replica
        Map<InetSocketAddress, RPCMetric> replicaMetrics =
                microServiceManager.getMicroServiceMetric(replicaID);

        // calculate time factor impact on RPC with latency and bandwidth
        long averageExecutionTime = 0;
        long summation = 0;
        long data;
        NodeMetric nodeMetric;
        for (Map.Entry<InetSocketAddress, RPCMetric> replicaMetric : replicaMetrics.entrySet()) {
            averageExecutionTime += replicaMetric.getValue().elapsedTime;

            data = replicaMetric.getValue().dataSize;
            nodeMetric = replicaNodeMetric.get(replicaMetric.getKey());
            // kernel client utilized by micro service but not running in kernel server are
            // ignored in process time computation because no metric data available of kernel server
            // from there.
            if (nodeMetric == null) {
                logger.info("skipping kernel client not part of kernel server");
                continue;
            }

            summation += (data / nodeMetric.rate + nodeMetric.latency);
        }

        if (!replicaMetrics.isEmpty()) {
            averageExecutionTime = averageExecutionTime / replicaMetrics.size();
        }

        return averageExecutionTime + summation;
    }

    private List<ReplicaID> getMigrationReplicas() {
        List<ReplicaID> replicaIDs = new ArrayList<ReplicaID>();

        try {
            List<MicroServiceID> microServiceIDS = microServiceManager.getAllMicroServices();
            // TODO Assuming all replica can migrate. In future, user can
            //  restrict replica migration to specific set of replica.
            for (MicroServiceID microServiceID : microServiceIDS) {
                try {
                    replicaIDs.addAll(microServiceManager.getReplicaIDs(microServiceID));
                } catch (MicroServiceNotFoundException e) {
                    logger.warning(
                            String.format(
                                    "MicroService [%s] not available. Skipping replicas in migration evaluation",
                                    microServiceID));
                    e.printStackTrace();
                }
            }
        } catch (RemoteException e) {
            logger.warning(
                    "Failed to get micro service replicas. Skipping replicas in migration evaluation");
            e.printStackTrace();
        }
        return replicaIDs;
    }

    /** Stop metric watcher timer */
    public void stop() {
        timer.cancel();
    }
}

package amino.run.oms.migrationDecision;

import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.common.ReplicaID;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.KernelServerNotFoundException;
import amino.run.kernel.common.metric.NodeMetric;
import amino.run.kernel.common.metric.RPCMetric;
import amino.run.oms.KernelServerManager;
import amino.run.oms.MicroServiceManager;
import amino.run.policy.DefaultPolicy;
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
    private Logger logger = Logger.getLogger(MetricWatcher.class.getName());
    private MicroServiceManager microServiceManager;
    private KernelServerManager serverManager;
    private MigrationPredictor predictor;
    private transient volatile ResettableTimer timer;
    private TreeSet<AbstractMap.SimpleEntry<ReplicaID, Long>> replicaHeap;
    /** Time interval of metric watch timer */
    public static int METRIC_WATCH_INTERVAL = 30000; // in milli seconds

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
    }

    /** Starts metric watcher. */
    public void start() {
        if (timer != null) {
            synchronized (this) {
                timer =
                        new ResettableTimer(
                                new TimerTask() {
                                    public void run() {
                                        // TODO there might be duplicate ReplicaStat in
                                        //  replicaStatsHeap.Check it

                                        // get replica list
                                        // TODO Assuming all replica can migrate. In future user can
                                        //  restrict replica migration to specific set of replica.
                                        List<ReplicaID> replicas =
                                                microServiceManager.getAllReplicaIDs();
                                        if (replicas.isEmpty()) {
                                            metricWatchReset();
                                            return;
                                        }

                                        // get best candidate for migration
                                        // TODO check for 3 candidate for migration for testing
                                        //  currently using one
                                        ReplicaID bestCandidateReplicaId =
                                                getBestCandidate(replicas);
                                        logger.fine(
                                                String.format(
                                                        "chosen Replica status [%s]",
                                                        bestCandidateReplicaId));
                                        // TODO check if recently migrated (check for cool down
                                        //  time)

                                        // get replica instance event handler
                                        EventHandler replica;
                                        try {
                                            replica =
                                                    microServiceManager.getReplicaDispatcher(
                                                            bestCandidateReplicaId);
                                        } catch (Exception e) {
                                            logger.warning("Failed to get replica dispatcher");
                                            e.printStackTrace();
                                            metricWatchReset();
                                            return;
                                        }

                                        // predict new kernel server
                                        InetSocketAddress kernelServer =
                                                predictor.getBestKernelServer(
                                                        bestCandidateReplicaId, replica);
                                        logger.fine(
                                                String.format(
                                                        "Replica [%s] should migrate from %s to %s",
                                                        replica, replica.getHost(), kernelServer));
                                        // skipping migration if same kernel server selected
                                        if (kernelServer.equals(replica.getHost())) {
                                            logger.info(
                                                    "skipping migration as predicted kernel server is same as replica kernel server");
                                            metricWatchReset();
                                            return;
                                        }

                                        // trigger migration
                                        triggerMigration(replica, kernelServer);
                                        metricWatchReset();
                                    }
                                },
                                METRIC_WATCH_INTERVAL);
            }
            timer.start();
        }
    }

    private void metricWatchReset() {
        replicaHeap.clear();
        timer.reset();
    }

    private ReplicaID getBestCandidate(List<ReplicaID> replicas) {
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
                                "skipping Replica [%s] evaluation as micro service not available",
                                replicaID));
                e.printStackTrace();
            } catch (MicroServiceReplicaNotFoundException e) {
                logger.warning(
                        String.format(
                                "skipping Replica [%s] evaluation as micro service replica not available",
                                replicaID));
                e.printStackTrace();
            } catch (KernelServerNotFoundException e) {
                logger.warning(
                        String.format(
                                "skipping Replica [%s] evaluation as micro service replica's kernel server not available",
                                replicaID));
                e.printStackTrace();
            }
        }

        // print replica and their processing time in sorted order
        for (AbstractMap.SimpleEntry<ReplicaID, Long> replicaStat : replicaHeap) {
            logger.info(String.format("Replica status -> [%s]", replicaStat));
        }
        // get best candidate for migration
        AbstractMap.SimpleEntry<ReplicaID, Long> bestCandidateReplicaStat = replicaHeap.first();
        logger.info(String.format("chosen Replica status -> [%s]", bestCandidateReplicaStat));

        return replicaHeap.first().getKey();
    }

    private void triggerMigration(EventHandler replica, InetSocketAddress kernelServer) {
        DefaultPolicy.ServerPolicy serverPolicy =
                (DefaultPolicy.ServerPolicy) replica.getObjects().get(0);

        DefaultPolicy.DefaultGroupPolicy replicaGroupPolicy;
        try {
            KernelOID koid =
                    microServiceManager.getRootGroupId(serverPolicy.getReplicaId().getOID());
            EventHandler groupPolicyEventHandler =
                    microServiceManager.getGroupDispatcher(
                            serverPolicy.getReplicaId().getOID(), koid);
            replicaGroupPolicy =
                    (DefaultPolicy.DefaultGroupPolicy) groupPolicyEventHandler.getObjects().get(0);
            replicaGroupPolicy.pin(serverPolicy, kernelServer);
        } catch (MicroServiceNotFoundException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (MicroServiceReplicaNotFoundException e) {
            e.printStackTrace();
        }
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

        // get metrics stat of RPC processing time from different kernel server
        // TODO currently considering processing time from different kernel server for simplicity.
        //  Need to get processing time within micro service replica
        Map<InetSocketAddress, RPCMetric> replicaMetrics =
                microServiceManager.getMicroServiceMetric(replicaID);

        // calculate time factor impact on RPC with latency and bandwidth
        long averageExecutionTime = 0;
        long summation = 0;
        long data;
        NodeMetric nodeMetric;
        for (Map.Entry<InetSocketAddress, RPCMetric> replicaMetric : replicaMetrics.entrySet()) {
            averageExecutionTime += replicaMetric.getValue().processTime;

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

    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }
}

package amino.run.oms.migrationDecision;

import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.ReplicaID;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.KernelServerNotFoundException;
import amino.run.kernel.common.metric.NodeMetric;
import amino.run.kernel.common.metric.RPCMetric;
import amino.run.oms.KernelServerManager;
import amino.run.oms.MicroServiceManager;
import amino.run.runtime.EventHandler;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Class used for predicting processing time for a replica.
 *
 * @author AmitRoushan
 */
public class MigrationPredictor {
    private MicroServiceManager microServiceManager;
    private KernelServerManager serverManager;
    private Logger logger = Logger.getLogger(MigrationPredictor.class.getName());

    public MigrationPredictor(
            KernelServerManager serverManager, MicroServiceManager microServiceManager) {
        this.serverManager = serverManager;
        this.microServiceManager = microServiceManager;
    }

    public InetSocketAddress getBestKernelServer(ReplicaID replicaID, EventHandler replica) {
        TreeSet<PredictedProcessingTime> predictedTime =
                new TreeSet<PredictedProcessingTime>(
                        new Comparator<PredictedProcessingTime>() {
                            @Override
                            public int compare(
                                    PredictedProcessingTime o1, PredictedProcessingTime o2) {
                                if (o1.processingTime < o2.processingTime) return 1;
                                else if (o1.processingTime > o2.processingTime) return -1;
                                return 0;
                            }
                        });

        // get all kernel server
        try {
            List<InetSocketAddress> kernelServers =
                    GlobalKernelReferences.nodeServer.oms.getServers(null);

            // calculate time consumption for each kernel server and push it into sorted heap
            for (InetSocketAddress ks : kernelServers) {
                predictedTime.add(
                        new PredictedProcessingTime(ks, getPredictedTime(replicaID, replica, ks)));
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return replica.getHost();
        }

        PredictedProcessingTime bestTimeOptimization = predictedTime.first();
        // if best time optimization is less or equal to zero then return current kernel server
        if (bestTimeOptimization.processingTime <= 0) {
            return replica.getHost();
        }
        // return index 0 element
        return bestTimeOptimization.kernelServer;
    }

    private long getPredictedTime(
            ReplicaID replicaID, EventHandler replica, InetSocketAddress futureKS) {
        long totalTimeOptimization = 0;

        // get replica metric with respect to kernel clients
        Map<InetSocketAddress, RPCMetric> replicaMetrics = null;
        try {
            replicaMetrics = microServiceManager.getMicroServiceMetric(replicaID);
        } catch (MicroServiceNotFoundException e) {
            e.printStackTrace();
            return 0;
        }

        // get current kernelServer CPU
        int currentKSCPU = serverManager.getKernelServerCPU(replica.getHost());
        // get future kernel server CPU
        int futureKSCPU = serverManager.getKernelServerCPU((futureKS));

        // get node metrics
        Map<InetSocketAddress, NodeMetric> currentNodeMetrics;
        Map<InetSocketAddress, NodeMetric> futureNodeMetrics;
        try {
            currentNodeMetrics = serverManager.getKernelServerMetric(replica.getHost());
            futureNodeMetrics = serverManager.getKernelServerMetric(futureKS);
        } catch (KernelServerNotFoundException e) {
            e.printStackTrace();
            return 0;
        }

        long currentProcessingTime;
        long currentByteExchanged;
        long futureProcessingTime;
        long currentNetworkFactorInProcessing;
        long futureNetworkFactorInProcessing;
        long timeOptimized;
        InetSocketAddress kernelClient;
        NodeMetric nodeMetric;
        // evaluate processing time optimization for each kernel client
        for (Map.Entry<InetSocketAddress, RPCMetric> kernelClientMetrics :
                replicaMetrics.entrySet()) {
            kernelClient = kernelClientMetrics.getKey();
            currentProcessingTime = kernelClientMetrics.getValue().processTime;
            futureProcessingTime = (currentProcessingTime * currentKSCPU) / futureKSCPU;
            // processingTimeMetric.getKey() = kernel client's kernelserver
            currentByteExchanged = kernelClientMetrics.getValue().dataSize;

            nodeMetric = currentNodeMetrics.get(kernelClient);
            if (replica.getHost().equals(kernelClient)) {
                currentNetworkFactorInProcessing = 0;
            } else if (nodeMetric == null) {
                logger.warning(
                        String.format(
                                "node metric not available between ks1 [%s] and ks2 [%s]",
                                replica.getHost(), kernelClientMetrics.getKey()));
                continue;
            } else {
                currentNetworkFactorInProcessing =
                        currentByteExchanged / nodeMetric.rate + nodeMetric.latency;
            }

            nodeMetric = futureNodeMetrics.get(kernelClient);
            if (futureKS.equals(kernelClient)) {
                futureNetworkFactorInProcessing = 0;
            } else if (nodeMetric == null) {
                logger.warning(
                        String.format(
                                "node metric not available between ks1 [%s] and ks2 [%s]",
                                futureKS, kernelClientMetrics.getKey()));
                continue;
            } else {
                futureNetworkFactorInProcessing =
                        currentByteExchanged / nodeMetric.rate + nodeMetric.latency;
            }

            logger.info(
                    String.format(
                            "Current processing time [%s], current network factor in processing time [%s], "
                                    + "future processing time [%s], future network factor in processing time [%s] from kernel client [%s]",
                            currentProcessingTime,
                            currentNetworkFactorInProcessing,
                            futureProcessingTime,
                            futureNetworkFactorInProcessing,
                            kernelClient));

            timeOptimized =
                    (currentProcessingTime + currentNetworkFactorInProcessing)
                            - (futureProcessingTime + futureNetworkFactorInProcessing);

            // TODO Define SLA for Kernel client
            if (timeOptimized < 0) {
                logger.info(
                        String.format(
                                "skipping kernel server [%s] as time optimization is negative",
                                futureKS));
                return 0;
            }

            totalTimeOptimization += timeOptimized;
        }
        logger.info(
                String.format(
                        "Time optimization for replica [%s] on KS [%s] : %s",
                        replicaID, futureKS, totalTimeOptimization));
        return totalTimeOptimization;
    }

    public class PredictedProcessingTime {
        public InetSocketAddress kernelServer;
        public long processingTime;

        public PredictedProcessingTime(InetSocketAddress kernelServer, long processingTime) {
            this.kernelServer = kernelServer;
            this.processingTime = processingTime;
        }
    }
}

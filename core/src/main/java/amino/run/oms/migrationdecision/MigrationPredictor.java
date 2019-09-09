package amino.run.oms.migrationdecision;

import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.ReplicaID;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.KernelServerNotFoundException;
import amino.run.kernel.metric.NodeMetric;
import amino.run.kernel.metric.RPCMetric;
import amino.run.oms.KernelServerManager;
import amino.run.oms.MicroServiceManager;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
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
    private TreeSet<AbstractMap.SimpleEntry<InetSocketAddress, Long>> predictedTime;
    private final int PREDICTED_TIME_THRESHOLD = 5; // in percentage

    public MigrationPredictor(
            KernelServerManager serverManager, MicroServiceManager microServiceManager) {
        this.serverManager = serverManager;
        this.microServiceManager = microServiceManager;
        predictedTime =
                new TreeSet<AbstractMap.SimpleEntry<InetSocketAddress, Long>>(
                        new Comparator<AbstractMap.SimpleEntry<InetSocketAddress, Long>>() {
                            @Override
                            public int compare(
                                    AbstractMap.SimpleEntry<InetSocketAddress, Long> o1,
                                    AbstractMap.SimpleEntry<InetSocketAddress, Long> o2) {
                                if (o1.getValue() < o2.getValue()) return 1;
                                else if (o1.getValue() > o2.getValue()) return -1;
                                return 0;
                            }
                        });
    }

    /**
     * Predict best kernel server for replica by constructing heap based on time optimization on
     * available kernel server
     *
     * @param replicaID
     * @param currentKernelServer
     * @return kernel server address
     */
    public InetSocketAddress getBestKernelServer(
            ReplicaID replicaID, InetSocketAddress currentKernelServer) {
        // get all kernel server
        try {
            List<InetSocketAddress> kernelServers =
                    GlobalKernelReferences.nodeServer.oms.getServers(null);

            // predict time optimization for replica on each kernel server and push it into sorted
            // heap

            for (InetSocketAddress ks : kernelServers) {
                try {
                    predictedTime.add(
                            new AbstractMap.SimpleEntry<InetSocketAddress, Long>(
                                    ks,
                                    predictTimeOptimization(replicaID, currentKernelServer, ks)));
                } catch (KernelServerNotFoundException e) {
                    logger.warning(e.getMessage());
                    continue;
                }
            }
        } catch (RemoteException e) {
            // TODO: Add logs for this exception
            e.printStackTrace();
            return currentKernelServer;
        }

        AbstractMap.SimpleEntry<InetSocketAddress, Long> bestTimeOptimization =
                predictedTime.first();

        // if best time optimization is less or equal to zero then return current kernel server
        if (bestTimeOptimization.getValue() <= 0) {
            logger.warning("Optimization time is negative.Suggesting current kernel server");
            return currentKernelServer;
        }

        predictedTime.clear();
        // return index 0 element
        return bestTimeOptimization.getKey();
    }

    /**
     * Predict total time optimization of replica when moved to future kernel server
     *
     * @param replicaID replica ID
     * @param currentKernelServer current kernel server of replica
     * @param futureKernelServer future kernel server of replica
     * @return total predicted optimized time
     * @throws KernelServerNotFoundException
     */
    private long predictTimeOptimization(
            ReplicaID replicaID,
            InetSocketAddress currentKernelServer,
            InetSocketAddress futureKernelServer)
            throws KernelServerNotFoundException {
        long totalPredictedOptimizedTime = 0;

        // get replica metric with respect to kernel clients
        Map<InetSocketAddress, RPCMetric> replicaMetrics = null;
        try {
            replicaMetrics = microServiceManager.getMicroServiceMetric(replicaID);
        } catch (MicroServiceNotFoundException e) {
            e.printStackTrace();
            return 0;
        }

        // get current kernelServer processor count
        int currentKernelServerProcessorCount =
                serverManager.getKernelServerProcessorCount(currentKernelServer);
        // get future kernel server processor count
        int futureKernelServerProcessorCount =
                serverManager.getKernelServerProcessorCount(futureKernelServer);

        // get node metrics
        Map<InetSocketAddress, NodeMetric> currentKernelServerMetrics =
                serverManager.getKernelServerMetric(currentKernelServer);
        Map<InetSocketAddress, NodeMetric> futureKernelServerMetrics =
                serverManager.getKernelServerMetric(futureKernelServer);

        long currentProcessingTime;
        long currentByteExchanged;
        long futureProcessingTime;
        long currentIOTime;
        long futureIOTime;
        long optimizedTime;
        InetSocketAddress kernelClient;

        // evaluate processing time optimization for each kernel client
        for (Map.Entry<InetSocketAddress, RPCMetric> kernelClientMetrics :
                replicaMetrics.entrySet()) {
            kernelClient = kernelClientMetrics.getKey();
            currentProcessingTime = kernelClientMetrics.getValue().elapsedTime;
            futureProcessingTime =
                    (currentProcessingTime * currentKernelServerProcessorCount)
                            / futureKernelServerProcessorCount;
            // processingTimeMetric.getKey() = kernel client's kernel server
            currentByteExchanged = kernelClientMetrics.getValue().dataSize;

            // get IO metrics for current and future kernel server
            try {
                // get io time between kernel client and current kernel server
                currentIOTime =
                        getIOTime(
                                currentKernelServerMetrics,
                                kernelClient,
                                currentKernelServer,
                                currentByteExchanged);
                // get io time between kernel client and future kernel server
                futureIOTime =
                        getIOTime(
                                futureKernelServerMetrics,
                                kernelClient,
                                futureKernelServer,
                                currentByteExchanged);
            } catch (MetricNotFoundException e) {
                // In some typical scenario kernel client is not running along side kernel server.
                // Hence IO attributes between kernel client and future kernel server can not get
                // calculated.
                logger.info(e.getMessage());
                currentIOTime = 0;
                futureIOTime = 0;
            }
            logger.fine(
                    String.format(
                            "Current processing time [%s], current network factor in processing time [%s], "
                                    + "future processing time [%s], future network factor in processing time [%s] from kernel client [%s]",
                            currentProcessingTime,
                            currentIOTime,
                            futureProcessingTime,
                            futureIOTime,
                            kernelClient));

            optimizedTime =
                    (currentProcessingTime + currentIOTime) - (futureProcessingTime + futureIOTime);

            // TODO Define SLA for Kernel client
            if (optimizedTime
                    < ((PREDICTED_TIME_THRESHOLD / 100)
                            * (currentProcessingTime + currentIOTime))) {
                logger.info(
                        String.format(
                                "Skipping kernel server [%s] as time predicted time is less than threshold",
                                futureKernelServer));
                return 0;
            }

            totalPredictedOptimizedTime += optimizedTime;
        }
        logger.fine(
                String.format(
                        "Time optimization for replica [%s] on KS [%s] : %s",
                        replicaID, futureKernelServer, totalPredictedOptimizedTime));
        return totalPredictedOptimizedTime;
    }

    private long getIOTime(
            Map<InetSocketAddress, NodeMetric> nodeMetrics,
            InetSocketAddress kernelClient,
            InetSocketAddress KernelServer,
            long currentByteExchanged)
            throws MetricNotFoundException {
        if (KernelServer.equals(kernelClient)) {
            // if kernel client is with kernel server then IO remain negligible
            return 0;
        }

        NodeMetric nodeMetric = nodeMetrics.get(kernelClient);
        if (nodeMetric == null) {
            String msg =
                    String.format(
                            "Node metric not available between kernel client [%s] and kernel server [%s]",
                            kernelClient, KernelServer);
            throw new MetricNotFoundException(msg);
        }

        return ((currentByteExchanged / nodeMetric.rate) + nodeMetric.latency);
    }
}

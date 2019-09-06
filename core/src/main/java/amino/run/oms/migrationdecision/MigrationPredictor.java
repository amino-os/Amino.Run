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
    private TreeSet<AbstractMap.SimpleEntry<InetSocketAddress, Long>> predictedTime;
    private final int TIME_OPTIMIZATION_THRESHOLD = 10; // in percentage

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
                predictedTime.add(
                        new AbstractMap.SimpleEntry<InetSocketAddress, Long>(
                                ks, getOptimizedTime(replicaID, currentKernelServer, ks)));
            }
        } catch (RemoteException e) {
            e
                    .printStackTrace(); // TODO: quinton: Do better logging and exception handling
                                        // than this.
            return currentKernelServer;
        } catch (KernelServerNotFoundException e) {
            e
                    .printStackTrace(); // TODO: quinton: Do better logging and exception handling
                                        // than this.
            return currentKernelServer;
        }

        AbstractMap.SimpleEntry<InetSocketAddress, Long> bestTimeOptimization =
                predictedTime.first();
        // if best time optimization is less or equal to zero then return current kernel server
        if (bestTimeOptimization.getValue() <= 0) {
            return currentKernelServer;
        }

        predictedTime.clear();
        // return index 0 element
        return bestTimeOptimization.getKey();
    }

    private long getOptimizedTime(
            ReplicaID replicaID,
            InetSocketAddress currentKernelServer,
            InetSocketAddress futureKernelServer)
            throws KernelServerNotFoundException {
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
        int currentKernelServerCPUCores =
                serverManager.getKernelServerProcessorCount(currentKernelServer);
        // get future kernel server CPU
        int futureKernelServerCPUCores =
                serverManager.getKernelServerProcessorCount((futureKernelServer));

        // get node metrics
        Map<InetSocketAddress, NodeMetric> currentKernelServerMetrics;
        Map<InetSocketAddress, NodeMetric> futureKernelServerMetrics;
        try {
            currentKernelServerMetrics = serverManager.getKernelServerMetric(currentKernelServer);
            futureKernelServerMetrics = serverManager.getKernelServerMetric(futureKernelServer);
        } catch (KernelServerNotFoundException e) {
            e.printStackTrace();
            return 0;
        }

        long currentProcessingTime;
        long currentByteExchanged;
        long futureProcessingTime;
        double currentIOTime;
        double futureIOTime;
        double timeOptimized;
        InetSocketAddress kernelClient;
        // evaluate processing time optimization for each kernel client
        for (Map.Entry<InetSocketAddress, RPCMetric> kernelClientMetrics :
                replicaMetrics.entrySet()) {
            kernelClient = kernelClientMetrics.getKey();
            currentProcessingTime = kernelClientMetrics.getValue().elapsedTime;
            futureProcessingTime =
                    (currentProcessingTime * currentKernelServerCPUCores)
                            / futureKernelServerCPUCores;
            // processingTimeMetric.getKey() = kernel client's kernel server
            currentByteExchanged = kernelClientMetrics.getValue().dataSize;

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
            } catch (Exception e) {
                // if kernel client runs in kernel server not registered with OMS. Ignore it.
                continue;
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

            timeOptimized =
                    ((double) currentProcessingTime + currentIOTime)
                            - ((double) futureProcessingTime + futureIOTime);

            // TODO Define SLA for Kernel client
            if (timeOptimized
                    < ((TIME_OPTIMIZATION_THRESHOLD / 100)
                            * (currentProcessingTime + currentIOTime))) {
                logger.info(
                        String.format(
                                "Skipping kernel server [%s] as time optimization is less than threshold",
                                futureKernelServer));
                return 0;
            }

            totalTimeOptimization += timeOptimized;
        }
        logger.fine(
                String.format(
                        "Time optimization for replica [%s] on KS [%s] : %s",
                        replicaID, futureKernelServer, totalTimeOptimization));
        return totalTimeOptimization;
    }

    private double getIOTime(
            Map<InetSocketAddress, NodeMetric> nodeMetrics,
            InetSocketAddress kernelClient,
            InetSocketAddress KernelServer,
            long currentByteExchanged)
            throws Exception {
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
            logger.warning(msg);
            throw new Exception(msg);
        }

        return currentByteExchanged / nodeMetric.rate + nodeMetric.latency;
    }
}

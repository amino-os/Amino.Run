package amino.run.policy.mobility.automigration;

import amino.run.app.SapphireObjectSpec;
import amino.run.app.labelselector.Labels;
import amino.run.common.SapphireReplicaID;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.oms.OMSServer;
import amino.run.policy.DefaultSapphirePolicy;
import amino.run.policy.mobility.explicitmigration.NotFoundDestinationKernelServerException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Logger;

public class AutoMigrationPolicy extends DefaultSapphirePolicy {
    // TODO define default labels for Metric collector
    private static final long DM_METRIC_UPDATE_FREQUENCY = 3000; // milliseconds

    /** Configuration for Metric Policy. */
    public static class Config implements SapphirePolicyConfig {
        private Labels metricLabels = Labels.newBuilder().create();
        private long metricUpdateFrequency = DM_METRIC_UPDATE_FREQUENCY;

        public long getMetricUpdateFrequency() {
            return metricUpdateFrequency;
        }

        public void setMetricUpdateFrequency(long frequency) {
            this.metricUpdateFrequency = frequency;
        }

        public Labels getMetricLabels() {
            return metricLabels;
        }

        public void setMetricLabels(Labels labels) {
            this.metricLabels = labels;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Config config = (Config) o;
            return metricLabels.toString().equals(config.metricLabels.toString());
        }

        @Override
        public int hashCode() {
            return Objects.hash(metricLabels);
        }
    }

    public static class AutoMigrationClientPolicy extends DefaultClientPolicy {}

    public static class AutoMigrationServerPolicy extends DefaultServerPolicy {
        private static Logger logger = Logger.getLogger(AutoMigrationServerPolicy.class.getName());
        private MetricAggregator aggregator;

        @Override
        public void onCreate(SapphireGroupPolicy group, SapphireObjectSpec spec) {
            super.onCreate(group, spec);
            aggregator = new MetricAggregator(spec, group.getSapphireObjId(), getReplicaId());
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if (!aggregator.initialize()) {
                aggregator.$__initialize();
            }

            aggregator.incRpcCounter();

            return aggregator.executionTime(() -> super.onRPC(method, params));
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            aggregator.stop();
        }

        /**
         * Migrates Sapphire Object to different Server
         *
         * @throws Exception migrateObject migrates the object to the specified Kernel Server
         */
        public void migrateObject(InetSocketAddress destinationAddr) throws Exception {
            logger.info(
                    "Performing Auto Migration of the object to Destination Kernel Server with address as "
                            + destinationAddr);
            OMSServer oms = GlobalKernelReferences.nodeServer.oms;
            ArrayList<InetSocketAddress> servers = new ArrayList<>(oms.getServers(null));

            KernelServerImpl localKernel = GlobalKernelReferences.nodeServer;
            InetSocketAddress localAddress = localKernel.getLocalHost();

            logger.info(
                    "Performing Auto Migration of object from "
                            + localAddress
                            + " to "
                            + destinationAddr);

            if (!(servers.contains(destinationAddr))) {
                throw new NotFoundDestinationKernelServerException(
                        destinationAddr,
                        "The destinations address passed is not present as one of the Kernel Servers");
            }

            if (!localAddress.equals(destinationAddr)) {
                localKernel.moveKernelObjectToServer(this, destinationAddr);
            }

            logger.info(
                    "Successfully performed Auto Migration of object from "
                            + localAddress
                            + " to "
                            + destinationAddr);
        }
    }

    public static class AutoMigrationGroupPolicy extends DefaultGroupPolicy {
        private static Logger logger = Logger.getLogger(AutoMigrationGroupPolicy.class.getName());

        /**
         * Migrates Sapphire Object to different Server
         *
         * @throws Exception migrateObject migrates the object to the specified Kernel Server
         */
        public void migrateObject(SapphireReplicaID rid, InetSocketAddress destinationAddr)
                throws Exception {
            logger.info(
                    "Performing Auto Migration of the object to Destination Kernel Server with address as "
                            + destinationAddr);
            AutoMigrationServerPolicy serverPolicy = null;
            for (SapphireServerPolicy server : getServers()) {
                if (server.getReplicaId() == rid) {
                    if (!(server instanceof AutoMigrationServerPolicy)) {
                        throw new Exception("Invalid server");
                    }
                    serverPolicy = (AutoMigrationServerPolicy) server;
                }
            }

            if (serverPolicy == null) {
                throw new Exception("Server with replica ID does not exist");
            }

            KernelServerImpl localKernel = GlobalKernelReferences.nodeServer;
            InetSocketAddress localAddress = localKernel.getLocalHost();
            logger.info("Performing Auto Migration of object to " + destinationAddr);

            serverPolicy.migrateObject(destinationAddr);

            logger.info(
                    "Successfully performed Auto Migration of object from to " + destinationAddr);
        }
    }
}

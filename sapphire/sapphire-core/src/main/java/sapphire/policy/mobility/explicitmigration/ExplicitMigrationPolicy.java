package sapphire.policy.mobility.explicitmigration;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import sapphire.app.DMSpec;
import sapphire.common.Utils;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelObjectMigratingException;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.policy.DefaultSapphirePolicy;

/**
 * Created by Malepati Bala Siva Sai Akhil on 1/22/18.
 *
 * <p>Migrate an object to another Kernel Server explicitly The SO Must implement migrateObject()
 * method by implementing the ExplicitMigrator interface (or by simply extending the
 * ExplicitMigratorImpl class). TODO: improve this by using annotations instead
 */
public class ExplicitMigrationPolicy extends DefaultSapphirePolicy {
    private static final int DEFAULT_RETRY_TIMEOUT = 15000;
    private static final int DEFAULT_MIN_WAIT_INTERVAL = 100;
    private static final String RETRY_TIMEOUT = "retryTimeoutInMillis";
    private static final String MIN_WAIT_TIMEOUT = "minWaitIntervalInMillis";

    public static class Config implements SapphirePolicyConfig {
        // Maximum time interval for wait before timing out (in milliseconds)
        private long retryTimeoutInMillis = DEFAULT_RETRY_TIMEOUT;
        // Minimum time interval for wait before retrying (in milliseconds)
        private long minWaitIntervalInMillis = DEFAULT_MIN_WAIT_INTERVAL;

        public long getRetryTimeout() {
            return retryTimeoutInMillis;
        }

        public void setRetryTimeout(long retryTimeoutInMillis) {
            this.retryTimeoutInMillis = retryTimeoutInMillis;
        }

        public long getMinWaitTimeout() {
            return minWaitIntervalInMillis;
        }

        public void setMinWaitTimeout(long minWaitIntervalInMillis) {
            this.minWaitIntervalInMillis = minWaitIntervalInMillis;
        }

        @Override
        public DMSpec toDMSpec() {
            DMSpec spec = new DMSpec();
            spec.setName(ExplicitMigrationPolicy.class.getName());
            spec.addProperty(RETRY_TIMEOUT, String.valueOf(retryTimeoutInMillis));
            spec.addProperty(MIN_WAIT_TIMEOUT, String.valueOf(minWaitIntervalInMillis));
            return spec;
        }

        @Override
        public SapphirePolicyConfig fromDMSpec(DMSpec spec) {
            if (!ExplicitMigrationPolicy.class.getName().equals(spec.getName())) {
                throw new IllegalArgumentException(
                        String.format(
                                "DM %s is not able to process spec %s",
                                ExplicitMigrationPolicy.class.getName(), spec));
            }

            ExplicitMigrationPolicy.Config config = new ExplicitMigrationPolicy.Config();
            Map<String, String> properties = spec.getProperties();
            if (properties != null) {
                config.setMinWaitTimeout(Long.parseLong(properties.get(MIN_WAIT_TIMEOUT)));
                config.setRetryTimeout(Long.parseLong(properties.get(RETRY_TIMEOUT)));
            }

            return config;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExplicitMigrationPolicy.Config config = (ExplicitMigrationPolicy.Config) o;
            return (retryTimeoutInMillis == config.retryTimeoutInMillis)
                    && (minWaitIntervalInMillis == config.minWaitIntervalInMillis);
        }

        @Override
        public int hashCode() {
            return Objects.hash(retryTimeoutInMillis, minWaitIntervalInMillis);
        }
    }

    public static class ClientPolicy extends DefaultClientPolicy {
        // Maximum time interval for wait before timing out (in milliseconds)
        private long retryTimeoutInMillis = DEFAULT_RETRY_TIMEOUT;
        // Minimum time interval for wait before retrying (in milliseconds)
        private long minWaitIntervalInMillis = DEFAULT_MIN_WAIT_INTERVAL;
        private static Logger logger =
                Logger.getLogger(ExplicitMigrationPolicy.ClientPolicy.class.getName());

        @Override
        public void onCreate(SapphireGroupPolicy group, Map<String, DMSpec> dmSpecMap) {
            super.onCreate(group, dmSpecMap);

            // TODO: Maintain name for DM which can be used to pass in DMSpec list
            DMSpec spec = Utils.getDMSpec(dmSpecMap, ExplicitMigrationPolicy.class.getName());
            if (spec != null) {
                try {
                    Config dmConfig = (Config) Utils.toConfig(spec);
                    retryTimeoutInMillis = dmConfig.getRetryTimeout();
                    minWaitIntervalInMillis = dmConfig.getMinWaitTimeout();
                } catch (Exception e) {
                    logger.info("Failed to get DMSpec values, using default values ");
                }
            }
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            long startTime = System.currentTimeMillis();
            long currentTime = System.currentTimeMillis();
            // While migrating if an RPC comes to the Server Side, KernelObjectMigratingException is
            // thrown from server side and exception would go to user, in order to avoid throwing
            // exception
            // to user, catching the exception here in the client policy and retrying. If even after
            // retryTimes,
            // we get the same KernelObjectMigratingException, then we throw the same to the user
            for (long delay = minWaitIntervalInMillis;
                    currentTime < (startTime + retryTimeoutInMillis - delay);
                    delay *= 2, currentTime = System.currentTimeMillis()) {
                try {
                    return super.onRPC(method, params);
                } catch (KernelObjectMigratingException e) {
                    logger.info(
                            "Caught KernelObjectMigratingException at client policy of ExplicitMigrator Policy: "
                                    + e
                                    + " retrying migration again");
                    Thread.sleep(delay);
                }
            }
            logger.info(
                    "Retry times has exceeded, so throwing the KernelObjectMigratingException to user");
            throw new KernelObjectMigratingException();
        }
    }

    public static class ServerPolicy extends DefaultServerPolicy {
        private static Logger logger =
                Logger.getLogger(ExplicitMigrationPolicy.ServerPolicy.class.getName());
        private String migrateObjectMethodName = "migrateObject";

        @Override
        public void onCreate(SapphireGroupPolicy group, Map<String, DMSpec> dmSpecMap) {
            super.onCreate(group, dmSpecMap);

            // TODO(multi-lang): Remove ExplicitMigrationPolicySpec annotation completely
            DMSpec spec = dmSpecMap.get(ExplicitMigrationPolicySpec.class.getSimpleName());
            if (spec != null) {
                migrateObjectMethodName = spec.getProperty("migrateObjectMethodName");
            }
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if (isMigrateObject(method)) {
                migrateObject((InetSocketAddress) params.get(0));
                return null;
            } else {
                return super.onRPC(method, params);
            }
        }

        /**
         * Migrates Sapphire Object to different Server
         *
         * @throws Exception migrateObject migrates the object to the specified Kernel Server
         */
        public void migrateObject(InetSocketAddress destinationAddr) throws Exception {
            logger.info(
                    "Performing Explicit Migration of the object to Destination Kernel Server with address as "
                            + destinationAddr);
            OMSServer oms = GlobalKernelReferences.nodeServer.oms;
            ArrayList<InetSocketAddress> servers = oms.getServers();

            KernelServerImpl localKernel = GlobalKernelReferences.nodeServer;
            InetSocketAddress localAddress = localKernel.getLocalHost();

            logger.info(
                    "Performing Explicit Migration of object from "
                            + localAddress
                            + " to "
                            + destinationAddr);

            if (!(servers.contains(destinationAddr))) {
                throw new NotFoundDestinationKernelServerException(
                        destinationAddr,
                        "The destinations address passed is not present as one of the Kernel Servers");
            }

            if (!localAddress.equals(destinationAddr)) {
                localKernel.moveKernelObjectToServer(destinationAddr, this.oid);
            }

            logger.info(
                    "Successfully performed Explicit Migration of object from "
                            + localAddress
                            + " to "
                            + destinationAddr);
        }

        Boolean isMigrateObject(String method) {
            return method.contains("." + migrateObjectMethodName + "(");
        }
    }

    public static class GroupPolicy extends DefaultGroupPolicy {}
}

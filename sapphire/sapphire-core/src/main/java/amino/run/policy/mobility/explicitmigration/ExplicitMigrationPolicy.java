package amino.run.policy.mobility.explicitmigration;

import amino.run.app.MicroServiceSpec;
import amino.run.common.Utils;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.KernelObjectMigratingException;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.oms.OMSServer;
import amino.run.policy.DefaultPolicy;
import amino.run.policy.Policy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Created by Malepati Bala Siva Sai Akhil on 1/22/18.
 *
 * <p>Migrate an object to another Kernel Server explicitly The SO Must implement migrateObject()
 * method by implementing the ExplicitMigrator interface (or by simply extending the
 * ExplicitMigratorImpl class). TODO: improve this by using annotations instead
 */
public class ExplicitMigrationPolicy extends DefaultPolicy {
    public static final int DEFAULT_RETRY_TIMEOUT_IN_MILLISEC = 15000;
    public static final int DEFAULT_MIN_WAIT_INTERVAL_IN_MILLISEC = 100;
    public static final String DEFAULT_MIGRATE_OBJECT_METHOD_NAME = "migrateObject";

    /** Configurations for ExplicitMigrationPolicy */
    public static class Config implements SapphirePolicyConfig {
        private int retryTimeoutInMillis = DEFAULT_RETRY_TIMEOUT_IN_MILLISEC;
        private int minWaitIntervalInMillis = DEFAULT_MIN_WAIT_INTERVAL_IN_MILLISEC;
        private String migrateObjectMethodName = DEFAULT_MIGRATE_OBJECT_METHOD_NAME;

        public int getretryTimeoutInMillis() {
            return retryTimeoutInMillis;
        }

        public void setRetryTimeoutInMillis(int retryTimeoutInMillis) {
            this.retryTimeoutInMillis = retryTimeoutInMillis;
        }

        public int getMinWaitIntervalInMillis() {
            return minWaitIntervalInMillis;
        }

        public void setMinWaitIntervalInMillis(int minWaitInterval) {
            this.minWaitIntervalInMillis = minWaitInterval;
        }

        public String getMigrateObjectMethodName() {
            return migrateObjectMethodName;
        }

        public void setMigrateObjectMethodName(String methodName) {
            this.migrateObjectMethodName = methodName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExplicitMigrationPolicy.Config config = (ExplicitMigrationPolicy.Config) o;
            return retryTimeoutInMillis == config.retryTimeoutInMillis
                    && minWaitIntervalInMillis == config.minWaitIntervalInMillis
                    && migrateObjectMethodName.equals(config.minWaitIntervalInMillis);
        }

        @Override
        public int hashCode() {
            return Objects.hash(retryTimeoutInMillis, minWaitIntervalInMillis);
        }
    }

    public static class ClientPolicy extends DefaultClientPolicy {
        private static Logger logger = Logger.getLogger(ClientPolicy.class.getName());

        // Maximum time interval for wait before timing out (in milliseconds)
        private long retryTimeoutInMillis = DEFAULT_RETRY_TIMEOUT_IN_MILLISEC;
        // Minimum time interval for wait before retrying (in milliseconds)
        private long minWaitIntervalInMillis = DEFAULT_MIN_WAIT_INTERVAL_IN_MILLISEC;

        @Override
        public void onCreate(
                Policy.GroupPolicy group, Policy.ServerPolicy server, MicroServiceSpec spec) {
            super.onCreate(group, server, spec);

            Map<String, SapphirePolicyConfig> configMap =
                    Utils.fromDMSpecListToFlatConfigMap(spec.getDmList());
            if (configMap != null) {
                SapphirePolicyConfig config =
                        configMap.get(ExplicitMigrationPolicy.Config.class.getName());
                if (config != null) {
                    this.retryTimeoutInMillis = ((Config) config).getretryTimeoutInMillis();
                    this.minWaitIntervalInMillis = ((Config) config).getMinWaitIntervalInMillis();
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
        private static Logger logger = Logger.getLogger(ServerPolicy.class.getName());
        private String migrateObjectMethodName = DEFAULT_MIGRATE_OBJECT_METHOD_NAME;

        @Override
        public void onCreate(Policy.GroupPolicy group, MicroServiceSpec spec) {
            super.onCreate(group, spec);

            SapphirePolicyConfig config =
                    configMap.get(ExplicitMigrationPolicy.Config.class.getName());
            if (config != null) {
                migrateObjectMethodName = ((Config) config).getMigrateObjectMethodName();
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
            ArrayList<InetSocketAddress> servers = new ArrayList<>(oms.getServers(null));

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
                localKernel.moveKernelObjectToServer(this, destinationAddr);
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

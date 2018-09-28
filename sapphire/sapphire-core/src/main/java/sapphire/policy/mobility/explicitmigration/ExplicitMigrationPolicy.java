package sapphire.policy.mobility.explicitmigration;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import sapphire.app.DMSpec;
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
    public static final int RETRYTIMEOUTINMILLIS = 15000;
    public static final int MINWAITINTERVALINMILLIS = 100;
    public static final String MIGRATEOBJECTMETHODNAME = "migrateObject";

    /** Configurations for ExplicitMigrationPolicy */
    public static class Config implements SapphirePolicyConfig {
        private int retryTimeoutInMillis = RETRYTIMEOUTINMILLIS;
        private int minWaitIntervalInMillis = MINWAITINTERVALINMILLIS;
        private String migrateObjectMethodName = MIGRATEOBJECTMETHODNAME;

        public int getretryTimeout() {
            return retryTimeoutInMillis;
        }

        public void setretryTimeout(int retryTimeoutInMillis) {
            this.retryTimeoutInMillis = retryTimeoutInMillis;
        }

        public int getminWaitInterval() {
            return minWaitIntervalInMillis;
        }

        public void setminWaitInterval(int minWaitInterval) {
            this.minWaitIntervalInMillis = minWaitInterval;
        }

        public String getmigrateObjectMethodName() {
            return migrateObjectMethodName;
        }

        public void setmigrateObjectMethodName(String methodName) {
            this.migrateObjectMethodName = methodName;
        }

        @Override
        public DMSpec toDMSpec() {
            DMSpec spec = new DMSpec();
            // Use fully qualified class name!
            spec.setName(ExplicitMigrationPolicy.class.getName());
            spec.addProperty("minWaitIntervalInMillis", String.valueOf(minWaitIntervalInMillis));
            spec.addProperty("retryTimeoutInMillis", String.valueOf(retryTimeoutInMillis));
            spec.addProperty("migrateObjectMethodName", String.valueOf(migrateObjectMethodName));
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
                config.setretryTimeout(Integer.parseInt(properties.get("retryTimeoutInMillis")));
                config.setminWaitInterval(
                        Integer.parseInt(properties.get("minWaitIntervalInMillis")));
                config.setmigrateObjectMethodName((properties.get("migrateObjectMethodName")));
            }

            return config;
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
        private static Logger logger =
                Logger.getLogger(ExplicitMigrationPolicy.ClientPolicy.class.getName());

        // Maximum time interval for wait before timing out (in milliseconds)
        private long retryTimeoutInMillis = RETRYTIMEOUTINMILLIS;
        // Minimum time interval for wait before retrying (in milliseconds)
        private long minWaitIntervalInMillis = MINWAITINTERVALINMILLIS;

        @Override
        public void onCreate(SapphireGroupPolicy group, Map<String, DMSpec> dmSpecMap) {
            super.onCreate(group, dmSpecMap);

            if (dmSpecMap != null) {

                DMSpec spec = dmSpecMap.get(ExplicitMigrationPolicy.class.getName());
                if ((spec != null) && (spec.getProperty("retryTimeoutInMillis") != null)) {
                    this.retryTimeoutInMillis =
                            Integer.valueOf(spec.getProperty("retryTimeoutInMillis"));
                }
                if ((spec != null) && (spec.getProperty("minWaitIntervalInMillis") != null)) {
                    this.minWaitIntervalInMillis =
                            Integer.valueOf(spec.getProperty("minWaitIntervalInMillis"));
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
        private String migrateObjectMethodName = MIGRATEOBJECTMETHODNAME;

        @Override
        public void onCreate(SapphireGroupPolicy group, Map<String, DMSpec> dmSpecMap) {
            super.onCreate(group, dmSpecMap);

            // TODO(multi-lang): Remove ExplicitMigrationPolicySpec annotation completely
            DMSpec spec = dmSpecMap.get(ExplicitMigrationPolicy.class.getName());
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

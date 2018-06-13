package sapphire.policy.mobility.explicitmigration;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.kernel.common.KernelObjectMigratingException;

/**
 * Created by Malepati Bala Siva Sai Akhil on 1/22/18.
 *
 * Migrate an object to another Kernel Server explicitly
 * The SO Must implement migrateObject() method by implementing the ExplicitMigrator interface
 * (or by simply extending the ExplicitMigratorImpl class).
 * TODO: improve this by using annotations instead
 **/

public class ExplicitMigrationPolicy extends DefaultSapphirePolicy {
    public static class ClientPolicy extends DefaultClientPolicy {
        private static Logger logger = Logger.getLogger(ExplicitMigrationPolicy.ClientPolicy.class.getName());

        // ToDo: Provide an option to take or change value of RETRY_TIMEOUT from user
        // Maximum time interval for wait before timing out (in milliseconds)
        private static final long RETRY_TIMEOUT = 15000;
        // Minimum time interval for wait before retrying (in milliseconds)
        private static final long MIN_WAIT_INTERVAL = 100;

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            long startTime = System.currentTimeMillis();
            long currentTime = System.currentTimeMillis();
            // While migrating if an RPC comes to the Server Side, KernelObjectMigratingException is
            // thrown from server side and exception would go to user, in order to avoid throwing exception
            // to user, catching the exception here in the client policy and retrying. If even after retryTimes,
            // we get the same KernelObjectMigratingException, then we throw the same to the user
            for (long delay = MIN_WAIT_INTERVAL; currentTime < (startTime + RETRY_TIMEOUT - delay); delay *= 2, currentTime = System.currentTimeMillis()) {
                try {
                    return super.onRPC(method, params);
                } catch (KernelObjectMigratingException e) {
                    logger.info("Caught KernelObjectMigratingException at client policy of ExplicitMigrator Policy: " + e + " retrying migration again");
                    Thread.sleep(delay);
                }
            }
            logger.info("Retry times has exceeded, so throwing the KernelObjectMigratingException to user");
            throw new KernelObjectMigratingException();
        }
    }

    public static class ServerPolicy extends DefaultServerPolicy {
        private static Logger logger = Logger.getLogger(ExplicitMigrationPolicy.ServerPolicy.class.getName());

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if (isMigrateObject(method)) {
                migrateObject((InetSocketAddress)params.get(0));
                return null;
            }
            else {
                return super.onRPC(method, params);
            }
        }

        /**
        * Migrates Sapphire Object to different Server
        * @throws Exception
        * migrateObject migrates the object to the specified Kernel Server
        */
        public void migrateObject(InetSocketAddress destinationAddr) throws Exception {
            logger.info("Performing Explicit Migration of the object to Destination Kernel Server with address as " + destinationAddr);
            OMSServer oms = GlobalKernelReferences.nodeServer.oms;
            ArrayList<InetSocketAddress> servers = oms.getServers();

            KernelServerImpl localKernel = GlobalKernelReferences.nodeServer;
            InetSocketAddress localAddress = localKernel.getLocalHost();

            logger.info("Performing Explicit Migration of object from " + localAddress + " to " + destinationAddr);

            if (!(servers.contains(destinationAddr))) {
                throw new NotFoundDestinationKernelServerException(destinationAddr, "The destinations address passed is not present as one of the Kernel Servers");
            }

            if (!localAddress.equals(destinationAddr)) {
                localKernel.moveKernelObjectToServer(destinationAddr, this.oid);
            }

            logger.info("Successfully performed Explicit Migration of object from " + localAddress + " to " + destinationAddr);
        }

        Boolean isMigrateObject(String method) {
            // TODO better check than simple base name
            return method.contains(".migrateObject(");
        }
    }
    public static class GroupPolicy extends DefaultGroupPolicy {}
}

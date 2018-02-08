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
 * The SO Must implement migrateObject() method by implementing the ExplicitMigration interface
 * (or by simply extending the ExplicitMigrationImpl class).
 * TODO: improve this by using annotations instead
 **/

public class ExplicitMigrationPolicy extends DefaultSapphirePolicy {
    public static class ClientPolicy extends DefaultClientPolicy {
        private static Logger logger = Logger.getLogger(ExplicitMigrationPolicy.ClientPolicy.class.getName());

        // Maximum time interval for wait before retrying (in seconds)
        private static final long MAX_WAIT_INTERVAL = 10;
        // Minimum time interval for wait before retrying (in seconds)
        private static final long MIN_WAIT_INTERVAL = 1;

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            int retryCount = 0;
            long delay;
            // While migrating if an RPC comes to the Server Side, KernelObjectMigratingException is
            // thrown from server side and exception would go to user, in order to avoid throwing exception
            // to user, catching the exception here in the client policy and retrying. If even after retryTimes,
            // we get the same KernelObjectMigratingException, then we throw the same to the user
            while (true) {
                try {
                    return super.onRPC(method, params);
                } catch (KernelObjectMigratingException e) {
                    logger.info("Caught KernelObjectMigratingException at client policy of ExplicitMigration Policy: " + e + " retrying migration for count " + retryCount++);
                    delay = getWaitTimeExp(retryCount);
                    if (delay < MAX_WAIT_INTERVAL) {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(delay));
                        // as delay is within MAX_WAIT_INTERVAL, trying again to perform the RPC before throwing the exception to the user
                        continue;
                    } else {
                        logger.info("Retry times has exceeded, so throwing the KernelObjectMigratingException to user");
                        throw new KernelObjectMigratingException();
                    }
                }
            }
        }

        // getWaitTimeExp(...) returns the next wait interval, in seconds, using
        // an exponential backoff algorithm
        public long getWaitTimeExp(int retryCount) {
            long waitTime = ((long) Math.pow(2, retryCount) * MIN_WAIT_INTERVAL);
            return waitTime;
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

            if (localAddress.equals(destinationAddr)) {
                logger.info("Explicit Migration of the object successfully done");
                return;
            } else if (!(servers.contains(destinationAddr))) {
                throw new NotFoundDestinationKernelServerException("The destinations address passed is not present as one of the Kernel Servers");
            }

            logger.info("Explicitly Migrating object " + this.oid + " to " + destinationAddr);
            localKernel.moveKernelObjectToServer(destinationAddr, this.oid);
        }

        Boolean isMigrateObject(String method) {
            // TODO better check than simple base name
            return method.contains(".migrateObject(");
        }
    }
    public static class GroupPolicy extends DefaultGroupPolicy {}
}

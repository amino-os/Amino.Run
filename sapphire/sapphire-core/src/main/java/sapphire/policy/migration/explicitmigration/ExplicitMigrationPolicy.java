package sapphire.policy.migration.explicitmigration;


import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Logger;

import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.policy.checkpoint.CheckpointPolicyBase;
import sapphire.policy.migration.MigrationPolicyBase;

/**
 * Created by mbssaiakhil on 1/22/18.
 *
 * Migrate an object to another Kernel Server explicitly
 * The SO Must implement migrateObject() method by implementing the ExplicitMigration interface
 * (or by simply extending the ExplicitMigrationImpl class).
 * TODO: improve this by using annotations instead
 **/

public class ExplicitMigrationPolicy extends MigrationPolicyBase {
    public static class ClientPolicy extends MigrationPolicyBase.ClientPolicy {}

    public static class ServerPolicy extends MigrationPolicyBase.ServerPolicy {
        private static Logger logger = Logger.getLogger(SapphireServerPolicy.class.getName());

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if (isMigrateObject(method)) {
                migrateObject();
                return null;
            }
            else {
                return super.onRPC(method, params);
            }
        }

        /**
         * Migrates Sapphire Object to different Server
         * @throws Exception
         * Currently the migrateObject migrates the object to a random Server other than the one SO
         * is currently on. Later it can be extended by using many metrics like CPU, Memory
         * utilization by other servers. Can be thought more regarding this.
         */
        public void migrateObject() throws Exception {
            logger.info("[ExplicitMigrationPolicy] Performing explicit migration of the object");
            OMSServer oms = GlobalKernelReferences.nodeServer.oms;
            ArrayList<InetSocketAddress> servers = oms.getServers();

            KernelServerImpl localKernel = GlobalKernelReferences.nodeServer;
            InetSocketAddress localAddress = localKernel.getLocalHost();
            ArrayList<InetSocketAddress> otherKernelServers = getOtherKernelServers(servers, localAddress);

            if (otherKernelServers.isEmpty()) {
                logger.info("[ExplicitMigrationPolicy] There are no target servers to migrate Sapphire Object" );
            } else {
                Random rand = new Random();
                InetSocketAddress randomTarget = otherKernelServers.get(rand.nextInt(otherKernelServers.size()));
                logger.info("[ExplicitMigrationPolicy] Shifting object " + this.oid + " to " + randomTarget);
                localKernel.moveKernelObjectToServer(randomTarget, this.oid);
            }
        }

        Boolean isMigrateObject(String method) {
            // TODO better check than simple base name
            return method.contains(".migrateObject(");
        }
    }
    public static class GroupPolicy extends CheckpointPolicyBase.ServerPolicy {}
}

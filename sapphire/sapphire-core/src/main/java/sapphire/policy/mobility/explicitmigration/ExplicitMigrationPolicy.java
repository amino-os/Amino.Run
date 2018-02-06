package sapphire.policy.mobility.explicitmigration;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.logging.Logger;

import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.policy.DefaultSapphirePolicy;

/**
 * Created by Malepati Bala Siva Sai Akhil on 1/22/18.
 *
 * Migrate an object to another Kernel Server explicitly
 * The SO Must implement migrateObject() method by implementing the ExplicitMigration interface
 * (or by simply extending the ExplicitMigrationImpl class).
 * TODO: improve this by using annotations instead
 **/

public class ExplicitMigrationPolicy extends DefaultSapphirePolicy {
    public static class ClientPolicy extends DefaultClientPolicy {}

    public static class ServerPolicy extends DefaultServerPolicy {
        private static Logger logger = Logger.getLogger(SapphireServerPolicy.class.getName());

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
            logger.info("[ExplicitMigrationPolicy] Performing Explicit Migration of the object to Destination Kernel Server with address as " + destinationAddr);
            OMSServer oms = GlobalKernelReferences.nodeServer.oms;
            ArrayList<InetSocketAddress> servers = oms.getServers();

            KernelServerImpl localKernel = GlobalKernelReferences.nodeServer;
            InetSocketAddress localAddress = localKernel.getLocalHost();

            if (localAddress.equals(destinationAddr)) {
                throw new DestinationSameAsSourceKernelServerException("The local and destinations Kernel Server address of migrations are same");

            } else if (!(servers.contains(destinationAddr))) {
                throw new NotFoundDestinationKernelServerException("The destinations address passed is not present as one of the Kernel Servers");
            }

            logger.info("[ExplicitMigrationPolicy] Explicitly Migrating object " + this.oid + " to " + destinationAddr);
            localKernel.moveKernelObjectToServer(destinationAddr, this.oid);
        }

        Boolean isMigrateObject(String method) {
            // TODO better check than simple base name
            return method.contains(".migrateObject(");
        }
    }
    public static class GroupPolicy extends DefaultGroupPolicy {}
}

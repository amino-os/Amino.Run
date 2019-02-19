package amino.run.policy.mobility.explicitmigration;

import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.kernel.common.KernelObjectStub;
import amino.run.policy.DefaultPolicy;
import amino.run.policy.Policy;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by Malepati Bala Siva Sai Akhil on 1/22/18.
 *
 * <p>Migrate an object to another Kernel Server explicitly The SO Must implement migrateObject()
 * method by implementing the ExplicitMigrator interface (or by simply extending the
 * ExplicitMigratorImpl class). TODO: improve this by using annotations instead
 */
public class ExplicitMigrationPolicy extends DefaultPolicy {
    public static class ClientPolicy extends DefaultClientPolicy {
        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if (isMigrateObject(method)) {
                /* If the method name is migrateObject and where to migrate address is not same as that of the locally
                cached remote server policy's kernel server address, request group policy to migrate remote server
                policy object */
                InetSocketAddress destAddress = (InetSocketAddress) params.get(0);
                if (!((KernelObjectStub) getServer()).$__getHostname().equals(destAddress)) {
                    ((GroupPolicy) getGroup())
                            .migrateMicroServiceInstance(getServer(), destAddress);
                    /* Update cached remote server policy object */
                    setServer(getGroup().onRefRequest());
                }
                return null;
            } else {
                return super.onRPC(method, params);
            }
        }

        Boolean isMigrateObject(String method) {
            return method.contains(".migrateMicroServiceInstance(");
        }
    }

    public static class ServerPolicy extends DefaultServerPolicy {}

    public static class GroupPolicy extends DefaultGroupPolicy {
        private static Logger logger = Logger.getLogger(GroupPolicy.class.getName());
        /**
         * Method to migrate server policy object(and microservice instance) to the given
         * destination address
         *
         * @param server server policy object to migrate
         * @param destinationAddress where to migrate address
         * @throws MigrationException
         * @throws RemoteException
         */
        public void migrateMicroServiceInstance(
                Policy.ServerPolicy server, InetSocketAddress destinationAddress)
                throws MigrationException, RemoteException {
            Policy.ServerPolicy sourceServer;
            /* Check if server policy is valid */
            sourceServer = getServer(server.getReplicaId());
            if (sourceServer == null) {
                throw new MigrationException("Not found the server policy object to migrate");
            }

            /* Get available kernel servers address list and check if the destination address is one among them */
            List<InetSocketAddress> servers = getAddressList(null, null);
            if (!(servers.contains(destinationAddress))) {
                throw new NotFoundDestinationKernelServerException(
                        destinationAddress, "Kernel Server is not available at given address");
            }

            try {
                /* Pin the source server policy object to given destination address */
                pin(sourceServer, destinationAddress);
            } catch (MicroServiceReplicaNotFoundException e) {
                throw new MigrationException("Not found the server policy object to migrate");
            } catch (MicroServiceNotFoundException e) {
                throw new MigrationException("MicroService not found");
            }

            logger.info(
                    String.format(
                            "Migrated server policy object from %s to %s",
                            ((KernelObjectStub) server).$__getHostname(), destinationAddress));
        }
    }
}

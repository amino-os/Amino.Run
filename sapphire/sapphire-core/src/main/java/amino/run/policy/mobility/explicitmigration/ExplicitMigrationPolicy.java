package amino.run.policy.mobility.explicitmigration;

import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.kernel.common.KernelObjectStub;
import amino.run.policy.DefaultPolicy;
import amino.run.policy.Policy;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Created by Malepati Bala Siva Sai Akhil on 1/22/18.
 *
 * <p>Migrate an server policy object(and its associated microService instance) to another Kernel
 * Server explicitly. The SO Must implement migrateTo() method by implementing the ExplicitMigrator
 * interface (or by simply extending the ExplicitMigratorImpl class). TODO: improve this by using
 * annotations instead
 */
public class ExplicitMigrationPolicy extends DefaultPolicy {
    public static class ClientPolicy extends DefaultClientPolicy {
        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if (isMigrateTo(method)) {
                /* If the method name is migrateTo, request group policy to migrate remote server policy object */
                ((GroupPolicy) getGroup()).migrate(getServer(), (InetSocketAddress) params.get(0));
                return null;
            } else {
                return super.onRPC(method, params);
            }
        }

        Boolean isMigrateTo(String method) {
            return method.contains(".migrateTo(");
        }
    }

    public static class ServerPolicy extends DefaultServerPolicy {}

    public static class GroupPolicy extends DefaultGroupPolicy {
        private static Logger logger = Logger.getLogger(GroupPolicy.class.getName());
        /**
         * Method to migrate server policy(and associated microService instance) to the given
         * destination address
         *
         * @param server Server policy object to migrate
         * @param destinationAddress Where to migrate address
         * @throws MigrationException
         * @throws RemoteException
         */
        public void migrate(Policy.ServerPolicy server, InetSocketAddress destinationAddress)
                throws MigrationException, RemoteException {
            Policy.ServerPolicy sourceServer;
            /* Check if server policy is valid */
            sourceServer = getServer(server.getReplicaId());
            if (sourceServer == null) {
                throw new MigrationException("Not found the server policy object to migrate");
            }

            if (((KernelObjectStub) sourceServer).$__getHostname().equals(destinationAddress)) {
                /* Current location of server policy is same as where it has to migrate. Just return */
                return;
            }

            /* Get available kernel servers address list and check if the destination address is one among them */
            if (!(getAddressList(null, null).contains(destinationAddress))) {
                throw new MigrationException(
                        String.format(
                                "Kernel Server is not available at %s address",
                                destinationAddress));
            }

            try {
                /* Pin the source server policy object to given destination address */
                pin(sourceServer, destinationAddress);
            } catch (MicroServiceReplicaNotFoundException e) {
                throw new MigrationException("Not found the server policy object to migrate", e);
            } catch (MicroServiceNotFoundException e) {
                throw new MigrationException("MicroService not found", e);
            }

            logger.info(
                    String.format(
                            "Migrated server policy object from %s to %s",
                            ((KernelObjectStub) server).$__getHostname(), destinationAddress));
        }
    }
}

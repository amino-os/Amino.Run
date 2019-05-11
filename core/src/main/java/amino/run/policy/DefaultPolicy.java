package amino.run.policy;

import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.common.Notification;
import amino.run.common.ReplicaID;
import amino.run.kernel.common.KernelObjectStub;
import amino.run.policy.util.ResettableTimer;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultPolicy extends Policy {

    public static class DefaultServerPolicy extends ServerPolicy {
        private GroupPolicy group;
        private transient ResettableTimer metricsNotificationTimer;
        private int METRICS_NOTIFICATION_TIME = 1000; // This may be a configurable param

        @Override
        public GroupPolicy getGroup() {
            return group;
        }

        @Override
        public void onMembershipChange() {}

        @Override
        public void onNotification(Notification notification) throws RemoteException {}

        @Override
        public void onCreate(GroupPolicy group) {
            this.group = group;

            /* TODO: Need to start periodic notification of microservice metrics to group policy only iff microservice
            is configured with objective functions to do dynamic migration */
            final GroupPolicy groupPolicy = group;
            metricsNotificationTimer =
                    new ResettableTimer(
                            new TimerTask() {
                                MetricsNotification notification =
                                        new MetricsNotification(getReplicaId());

                                public void run() {
                                    try {
                                        notification.metrics = getRPCMetrics();
                                        groupPolicy.onNotification(notification);
                                        metricsNotificationTimer.reset();
                                    } catch (Exception e) {
                                        logger.warning(
                                                String.format(
                                                        "Failed to notify metrics to group. Exception : %s",
                                                        e));
                                    }
                                }
                            },
                            METRICS_NOTIFICATION_TIME);
            metricsNotificationTimer.start();
        }

        @Override
        public void onDestroy() {
            if (metricsNotificationTimer != null) {
                metricsNotificationTimer.cancel();
            }
        }
    }

    public static class DefaultClientPolicy extends ClientPolicy {
        private ServerPolicy server;
        private GroupPolicy group;

        @Override
        public void setServer(ServerPolicy server) {
            this.server = server;
        }

        @Override
        public ServerPolicy getServer() throws RemoteException {
            if (server == null) {
                server = getGroup().onRefRequest();
            }

            return server;
        }

        @Override
        public GroupPolicy getGroup() {
            return group;
        }

        @Override
        public void onCreate(GroupPolicy group) {
            this.group = group;
        }
    }

    public static class DefaultGroupPolicy extends GroupPolicy {
        private static final Logger logger = Logger.getLogger(DefaultGroupPolicy.class.getName());
        private ConcurrentHashMap<ReplicaID, ServerPolicy> servers =
                new ConcurrentHashMap<ReplicaID, ServerPolicy>();

        protected void addServer(ServerPolicy server) {
            servers.put(server.getReplicaId(), server);
        }

        protected void removeServer(ServerPolicy server) {
            servers.remove(server.getReplicaId());
        }

        @Override
        public ArrayList<ServerPolicy> getServers() throws RemoteException {
            return new ArrayList<ServerPolicy>(servers.values());
        }

        @Override
        public void onCreate(String region, ServerPolicy server) throws RemoteException {
            InetSocketAddress host = ((KernelObjectStub) server).$__getHostname();

            try {
                if (!server.shouldSkipPinning()) {
                    this.pin(server, host);
                }
            } catch (RemoteException e) {
                logger.log(
                        Level.SEVERE,
                        String.format(
                                "Failed to pin original Microservice to Remote %s with Remote Exception. Exception: %s",
                                host, e),
                        e);
                throw new Error(e);
            } catch (MicroServiceNotFoundException e) {
                logger.log(Level.SEVERE, "Failed to pin original Microservice to " + host, e);
                throw new Error(e);
            } catch (MicroServiceReplicaNotFoundException e) {
                logger.log(
                        Level.SEVERE,
                        String.format(
                                "Failed to pin original Microservice to %s because replica was not found.",
                                host),
                        e);
                throw new Error(e);
            }

            addServer(server);
        }

        @Override
        public void onNotification(Notification notification) throws RemoteException {
            if (notification instanceof MetricsNotification) {
                MetricsNotification object = (MetricsNotification) notification;
                /* Store these metrics to make them available for statistics and decision making module */
                updateMetric(object.replicaId, object.metrics);
            } else if (notification instanceof MigrationNotification) {
                MigrationNotification migrationNotification = (MigrationNotification) notification;

                /*Migrate replica to kernel server receive in notification */
                ReplicaID replicaID = migrationNotification.replicaId;
                ServerPolicy serverPolicy = getServer(replicaID);
                if (serverPolicy == null) {
                    throw new RemoteException(
                            String.format("Replica [%s] not available", replicaID));
                }

                try {
                    pin(serverPolicy, migrationNotification.kernelServer);
                } catch (MicroServiceReplicaNotFoundException e) {
                    throw new RemoteException(e.getMessage());
                } catch (MicroServiceNotFoundException e) {
                    throw new RemoteException(e.getMessage());
                }
            }
        }

        @Override
        public void onDestroy() throws RemoteException {
            /* Delete all the servers */
            for (ServerPolicy server : getServers()) {
                terminate(server);
            }
        }

        /** Below methods can be used by all the DMs extending this default DM. */

        /**
         * Gets the server policy having the given replica Id from this group policy's servers map
         *
         * @param serverId Server policy replica Id
         * @return Server policy
         */
        protected ServerPolicy getServer(ReplicaID serverId) {
            return servers.get(serverId);
        }

        /**
         * This method is used to replicate a server policy at the given source considering itself
         * as reference copy and pin it to kernel server with specified host. And adds it to its
         * local server list.
         *
         * @param replicaSource Server policy on which a new replica is created considering itself
         *     as reference copy
         * @param dest Host address on which replicated copy need to pin
         * @param region Region in which replicated server has to be pinned. It is passed down to
         *     all downstream DMs till leaf. And downstream DM pinning the chain ensures to pin on
         *     kernel server belonging to the region
         * @return New replica of server policy
         * @throws RemoteException
         * @throws MicroServiceNotFoundException
         * @throws MicroServiceReplicaNotFoundException
         */
        protected ServerPolicy replicate(
                ServerPolicy replicaSource, InetSocketAddress dest, String region)
                throws RemoteException, MicroServiceNotFoundException,
                        MicroServiceReplicaNotFoundException {
            ServerPolicy replica = replicaSource.replicate(region);

            if (replicaSource.isLastPolicy()) {
                pin(replica, dest);
            }

            addServer(replica);
            return replica;
        }

        /**
         * Pin the server policy to kernel server with specified host
         *
         * @param server
         * @param host
         * @throws MicroServiceReplicaNotFoundException
         * @throws RemoteException
         * @throws MicroServiceNotFoundException
         */
        protected void pin(ServerPolicy server, InetSocketAddress host)
                throws MicroServiceReplicaNotFoundException, RemoteException,
                        MicroServiceNotFoundException {
            if (server.isLastPolicy()) {
                server.pin_to_server(host);
                ((KernelObjectStub) server).$__updateHostname(host);
            }
        }

        /**
         * Destroys the given server policy from the kernel server where it resides. And removes it
         * from its local server list
         *
         * @param server
         * @throws RemoteException
         */
        protected void terminate(ServerPolicy server) throws RemoteException {
            server.terminate();
            removeServer(server);
        }
    }
}

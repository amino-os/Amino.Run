package amino.run.policy;

import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.common.Notification;
import amino.run.common.ReplicaID;
import amino.run.kernel.common.KernelObjectStub;
import amino.run.kernel.common.KernelServerNotFoundException;
import amino.run.kernel.common.ServerUnreachable;
import amino.run.kernel.common.metric.metricHandler.MicroServiceMetricManager;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class DefaultPolicy extends Policy {

    public static class DefaultServerPolicy extends ServerPolicy {
        private GroupPolicy group;
        private MicroServiceMetricManager metricManager;

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if (metricManager != null) {
                return metricManager.onRPC(method, params);
            }
            return upRPCCall(method, params);
        }

        public Object upRPCCall(String method, ArrayList<Object> params) throws Exception {
            return super.onRPC(method, params);
        }

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
            // get micro service specification information from group policy
            if (isLastPolicy()) {
                metricManager = MicroServiceMetricManager.create(this, getSpec());
            }
        }

        @Override
        public void onDestroy() {
            if (metricManager != null) {
                metricManager.destroy();
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
        private ConcurrentHashMap<ReplicaID, ServerPolicy> servers =
                new ConcurrentHashMap<ReplicaID, ServerPolicy>();

        @Override
        protected void addServer(ServerPolicy server) {
            servers.put(server.getReplicaId(), server);
        }

        @Override
        protected void removeServer(ServerPolicy server) {
            servers.remove(server.getReplicaId());
        }

        @Override
        protected void replicate()
                throws RemoteException, MicroServiceNotFoundException,
                        KernelServerNotFoundException {
            /* TODO: In case of single instance DM, should replicate here. But there is no reference copy to replicate.
            And the microservice state is not presisted. Need to consider it further. Restore from a snapshot that was
            stored somewhere other than on the original (now dead) server. e.g. on remote persistent storage, like
            Tapir, EBS or similar. Create a new, uninitialized instance (suitable for stateless microservices). */
        }

        @Override
        public ArrayList<ServerPolicy> getServers() throws RemoteException {
            return new ArrayList<ServerPolicy>(servers.values());
        }

        @Override
        public void onCreate(String region, ServerPolicy server) throws RemoteException {
            InetSocketAddress host = null;

            try {
                if (!server.shouldSkipPinning()) {
                    this.pin(server, ((KernelObjectStub) server).$__getHostname());
                }
            } catch (RemoteException e) {
                logger.log(
                        Level.SEVERE,
                        String.format(
                                "Failed to pin original Microservice to %s due to Remote Exception to %s. Exception: %s",
                                host, server),
                        e);
                throw new Error(e);
            } catch (MicroServiceNotFoundException e) {
                logger.log(Level.SEVERE, "Failed to pin original Microservice to " + host, e);
                throw new Error(e);
            }

            addServer(server);
        }

        @Override
        public void onNotification(Notification notification) throws RemoteException {
            if (notification instanceof ServerUnreachable) {
                onServerUnReachable((ServerUnreachable) notification);
            }

            return;
        }

        @Override
        public void onDestroy() throws RemoteException {
            /* Delete all the servers */
            for (ServerPolicy server : getServers()) {
                try {
                    terminate(server);
                } catch (Exception e) {
                    logger.warning(String.format("Exception occurred : %s", e.getMessage()));
                    try {
                        terminateLocal(server);
                    } catch (Exception e1) {
                        logger.warning(String.format("Exception occurred : %s", e1.getMessage()));
                    }
                }
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
         */
        protected ServerPolicy replicate(
                ServerPolicy replicaSource, InetSocketAddress dest, String region)
                throws RemoteException, MicroServiceNotFoundException {
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
                throws RemoteException, MicroServiceNotFoundException {
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

        private void onServerUnReachable(ServerUnreachable unreachable) throws RemoteException {
            /* Check if there is a replica on the unreachable server. If yes, remove the replica resources and create a new replica and pin it on an other server. */
            InetSocketAddress host = unreachable.serverInfo.getHost();
            for (ServerPolicy server : getServers()) {
                if (((KernelObjectStub) server).$__getHostname().equals(host)) {
                    /* Kernel server is not reachable anymore. Remove the server policy from local servers list and
                    Replicate a new server policy object */
                    removeServer(server);
                    try {
                        /* Remove the resource allocated(such as replica id kernel oid etc.) for server policy on OMS */
                        terminateLocal(server);
                        replicate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

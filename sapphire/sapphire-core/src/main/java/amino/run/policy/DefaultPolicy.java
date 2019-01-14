package amino.run.policy;

import amino.run.app.MicroServiceSpec;
import amino.run.common.SapphireObjectNotFoundException;
import amino.run.common.SapphireObjectReplicaNotFoundException;
import amino.run.kernel.common.KernelObjectStub;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class DefaultPolicy extends Policy {

    public static class DefaultServerPolicy extends ServerPolicy {
        @Override
        public GroupPolicy getGroup() {
            return group;
        }

        @Override
        public void onMembershipChange() {}

        @Override
        public void onCreate(GroupPolicy group, MicroServiceSpec spec) {
            super.onCreate(group, spec);
        }

        public void onDestroy() {
            super.onDestroy();
        }
    }

    public static class DefaultClientPolicy extends ClientPolicy {
        private DefaultServerPolicy server;
        private DefaultGroupPolicy group;

        @Override
        public void setServer(ServerPolicy server) {
            this.server = (DefaultServerPolicy) server;
        }

        @Override
        public ServerPolicy getServer() {
            return server;
        }

        @Override
        public GroupPolicy getGroup() {
            return group;
        }

        @Override
        public void onCreate(GroupPolicy group, MicroServiceSpec spec) {
            this.group = (DefaultGroupPolicy) group;
        }
    }

    public static class DefaultGroupPolicy extends GroupPolicy {
        private ArrayList<ServerPolicy> servers = new ArrayList<ServerPolicy>();
        protected String region = "";
        protected MicroServiceSpec spec = null;

        protected synchronized void addServer(ServerPolicy server) {
            servers.add(server);
        }

        protected synchronized void removeServer(ServerPolicy server) {
            servers.remove(server);
        }

        @Override
        public ArrayList<ServerPolicy> getServers() throws RemoteException {
            return new ArrayList<ServerPolicy>(servers);
        }

        @Override
        public void onCreate(String region, ServerPolicy server, MicroServiceSpec spec)
                throws RemoteException {
            this.region = region;
            this.spec = spec;
            addServer(server);
        }

        @Override
        public synchronized void onDestroy() throws RemoteException {
            super.onDestroy();
            servers.clear();
        }

        /** Below methods can be used by all the DMs extending this default DM. */

        /**
         * This method is used to replicate a server policy at the given source considering itself
         * as reference copy and pin it to kernel server with specified host. And adds the replica
         * to its local server list.
         *
         * @param replicaSource Server policy on which a new replica is created considering itself
         *     as reference copy
         * @param dest Host address on which replicated copy need to pin
         * @param region Region
         * @param pinned Flag indicating whether pinning is required or not
         * @return New replica of server policy
         * @throws RemoteException
         * @throws SapphireObjectNotFoundException
         * @throws SapphireObjectReplicaNotFoundException
         */
        protected SapphireServerPolicy addReplica(
                SapphireServerPolicy replicaSource,
                InetSocketAddress dest,
                String region,
                boolean pinned)
                throws RemoteException, SapphireObjectNotFoundException,
                        SapphireObjectReplicaNotFoundException {
            SapphireServerPolicy replica =
                    replicaSource.sapphire_replicate(replicaSource.getProcessedPolicies(), region);
            if (!pinned) {
                // If the chain is not pinned at downstream nodes, pin it
                pinReplica(replica, dest);
            }

            addServer(replica);
            return replica;
        }

        /**
         * Pin the server policy to kernel server with specified host
         *
         * @param server
         * @param host
         * @throws SapphireObjectReplicaNotFoundException
         * @throws RemoteException
         * @throws SapphireObjectNotFoundException
         */
        protected void pinReplica(SapphireServerPolicy server, InetSocketAddress host)
                throws SapphireObjectReplicaNotFoundException, RemoteException,
                        SapphireObjectNotFoundException {
            server.sapphire_pin_to_server(server, host);
            ((KernelObjectStub) server).$__updateHostname(host);
        }

        /**
         * Deletes the replica from the kernel server where it resides. And removes it from its
         * local server list
         *
         * @param server
         * @throws RemoteException
         */
        protected void removeReplica(SapphireServerPolicy server) throws RemoteException {
            server.sapphire_remove_replica();
            removeServer(server);
        }
    }
}

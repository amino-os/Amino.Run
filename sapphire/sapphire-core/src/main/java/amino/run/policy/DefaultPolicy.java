package amino.run.policy;

import amino.run.app.MicroServiceSpec;
import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
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
            ServerPolicy replica =
                    replicaSource.sapphire_replicate(replicaSource.getProcessedPolicies(), region);
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
            server.sapphire_pin_to_server(host);
            ((KernelObjectStub) server).$__updateHostname(host);
        }

        /**
         * Destroys the given server policy from the kernel server where it resides. And removes it
         * from its local server list
         *
         * @param server
         * @throws RemoteException
         */
        protected void terminate(ServerPolicy server) throws RemoteException {
            server.sapphire_terminate();
            removeServer(server);
        }
    }
}

package amino.run.policy;

import amino.run.app.DMSpec;
import amino.run.app.MicroServiceSpec;
import amino.run.app.NodeSelectorSpec;
import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.common.NoKernelServerFoundException;
import amino.run.common.ReplicaID;
import amino.run.kernel.common.KernelObjectNotFoundException;
import amino.run.kernel.common.KernelObjectStub;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

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
        private ConcurrentHashMap<ReplicaID, ServerPolicy> servers =
                new ConcurrentHashMap<ReplicaID, ServerPolicy>();
        protected MicroServiceSpec spec = null;

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
        public void onCreate(ServerPolicy server, MicroServiceSpec spec) throws RemoteException {
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
         *     as reference copy all downstream DMs till leaf. And downstream DM pinning the chain
         *     ensures to pin on kernel server belonging to the region
         * @return New replica of server policy
         * @throws RemoteException
         * @throws MicroServiceNotFoundException
         * @throws MicroServiceReplicaNotFoundException
         */
        protected ServerPolicy replicate(ServerPolicy replicaSource)
                throws RemoteException, MicroServiceNotFoundException,
                        MicroServiceReplicaNotFoundException, NoKernelServerFoundException,
                        KernelObjectNotFoundException {
            ServerPolicy replica =
                    replicaSource.sapphire_replicate(replicaSource.getProcessedPolicies());
            if (replicaSource.isLastPolicy()) {
                pin(replica, getKernelServer());
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

        /**
         * Select kernel server address for based on MicroServiceSpec
         *
         * @return
         * @throws NoKernelServerFoundException
         * @throws RemoteException
         */
        protected InetSocketAddress getKernelServer()
                throws NoKernelServerFoundException, RemoteException,
                        KernelObjectNotFoundException {
            NodeSelectorSpec nodeSelector = spec.getNodeSelectorSpec();
            List<InetSocketAddress> serversInRegion;

            String region = selectRegion();

            nodeSelector = new NodeSelectorSpec();
            // TODO: append node selector from user in MicroService spec and DM spec
            nodeSelector.addAndLabel(region);
            serversInRegion = oms().getServers(nodeSelector);
            if (serversInRegion == null || serversInRegion.isEmpty()) {
                String msg =
                        String.format(
                                "No kernel servers were found for %s & %s",
                                spec.getNodeSelectorSpec(), region);
                logger.log(Level.SEVERE, msg);
                throw new NoKernelServerFoundException();
            }

            // logic for selecting region with round robin fashion
            // check server count of each region
            HashMap<InetSocketAddress, Integer> serversCount = new HashMap<>();
            InetSocketAddress serverSocketAddress;
            Integer count;
            for (ServerPolicy server : servers.values()) {
                serverSocketAddress = ((KernelObjectStub) server).$__getHostname();
                count = serversCount.get(serverSocketAddress);
                if (count == null) {
                    serversCount.put(serverSocketAddress, 1);
                    continue;
                }

                serversCount.put(serverSocketAddress, count + 1);
            }

            // get region with least count
            int selectedAddressCount = 0;
            int addressCount;
            InetSocketAddress selectedAddress = null;
            for (InetSocketAddress server : serversInRegion) {
                addressCount = serversCount.get(server);
                if (addressCount == 0) {
                    return server;
                }
                if (selectedAddressCount < addressCount) {
                    continue;
                }

                selectedAddress = server;
                selectedAddressCount = addressCount;
            }

            return selectedAddress;
        }

        private String selectRegion() throws RemoteException {
            NodeSelectorSpec microServiceNodeSpec = spec.getNodeSelectorSpec();
            boolean topologicalAffinity = microServiceNodeSpec.isTopologicalAffinity();

            // check for topological affinity and get region
            ArrayList<String> regions = oms().getRegions();
            for (DMSpec dmSpec : spec.getDmList()) {
                NodeSelectorSpec dmNodeSpec = dmSpec.getNodeSpec();
                if (dmNodeSpec != null && !dmNodeSpec.isTopologicalAffinity()) {
                    topologicalAffinity = !dmNodeSpec.isTopologicalAffinity();
                }
            }

            if (topologicalAffinity) {
                if (servers.size() == 0) {
                    return regions.get(new Random().nextInt(regions.size()));
                }
                // return region of first server
                for (ServerPolicy server : servers.values()) {
                    return server.sapphire_getRegion();
                }
            }

            // logic for selecting region with round robin fashion
            // check server count of each region
            HashMap<String, Integer> serversRegions = new HashMap<>();
            String serverRegion;
            Integer count;
            for (ServerPolicy server : servers.values()) {
                serverRegion = server.sapphire_getRegion();
                count = serversRegions.get(serverRegion);
                if (count == null) {
                    serversRegions.put(serverRegion, 1);
                    continue;
                }

                serversRegions.put(serverRegion, count + 1);
            }

            // get region with least count
            int selectedRegionServerCount = 0;
            String selectedRegion = "";
            int regionServerCount;
            for (String region : regions) {
                regionServerCount = serversRegions.get(region);
                if (regionServerCount == 0) {
                    return region;
                }
                if (selectedRegionServerCount < regionServerCount) {
                    continue;
                }

                selectedRegion = region;
                selectedRegionServerCount = regionServerCount;
            }

            return selectedRegion;
        }
    }
}

package amino.run.policy.dht;

import amino.run.app.DMSpec;
import amino.run.app.MicroServiceSpec;
import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.common.NoKernelServerFoundException;
import amino.run.common.Utils;
import amino.run.kernel.common.KernelObjectNotFoundException;
import amino.run.policy.DefaultPolicy;
import amino.run.policy.Policy;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DHTPolicy extends DefaultPolicy {
    private static final int DEFAULT_NUM_OF_SHARDS = 3;

    /** Configuration for DHT Policy. */
    public static class Config implements SapphirePolicyConfig {
        private int numOfShards = DEFAULT_NUM_OF_SHARDS;

        public int getNumOfShards() {
            return numOfShards;
        }

        public void setNumOfShards(int numOfShards) {
            this.numOfShards = numOfShards;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Config config = (Config) o;
            return numOfShards == config.numOfShards;
        }

        @Override
        public int hashCode() {
            return Objects.hash(numOfShards);
        }
    }

    public static class ClientPolicy extends DefaultClientPolicy {
        private static Logger logger = Logger.getLogger(ClientPolicy.class.getName());
        private DHTChord dhtChord;

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            DHTKey key = new DHTKey(params.get(0).toString());
            if (dhtChord == null) {
                dhtChord = ((GroupPolicy) getGroup()).getChord();
            }

            DHTNode node = dhtChord.getResponsibleNode(key);
            logger.fine("Responsible node for: " + key + " is: " + node.id);
            return node.server.onRPC(method, params);
        }
    }

    public static class ServerPolicy extends DefaultServerPolicy {}

    public static class GroupPolicy extends DefaultGroupPolicy {
        private static Logger logger = Logger.getLogger(GroupPolicy.class.getName());
        private int numOfShards = DEFAULT_NUM_OF_SHARDS;
        private DHTChord dhtChord;

        @Override
        public void onCreate(Policy.ServerPolicy server, MicroServiceSpec spec)
                throws RemoteException {
            InetSocketAddress newServerAddress = null;
            dhtChord = new DHTChord();
            super.onCreate(server, spec);

            if (spec != null) {
                List<DMSpec> dmSpecList = spec.getDmList();
                Map<String, SapphirePolicyConfig> configMap =
                        Utils.fromDMSpecListToFlatConfigMap(dmSpecList);
                if (configMap != null) {
                    SapphirePolicyConfig config = configMap.get(DHTPolicy.Config.class.getName());
                    if (config != null) {
                        this.numOfShards = ((Config) config).getNumOfShards();
                    }
                }

                if (dmSpecList != null) {
                    for (DMSpec dmSpec : dmSpecList) {
                        if (dmSpec.getName().equals(DHTPolicy.Config.class.getName())) {
                            dmSpec.getNodeSpec().enableTopologicalAffinity();
                        }
                    }
                }
            }

            try {
                ArrayList<String> regions = sapphire_getRegions();

                if (server.isLastPolicy()) {
                    // TODO: Make deployment kernel pin primary replica once node selection
                    // constraint is implemented.
                    InetSocketAddress address = getKernelServer();
                    pin(server, address);
                }

                // Create replicas based on annotation
                for (int i = 1; i < numOfShards; i++) {
                    replicate(server);
                }
            } catch (RemoteException e) {
                throw new Error(
                        "Could not create new group policy because the oms is not available.");
            } catch (MicroServiceNotFoundException e) {
                throw new Error("Could not find Sapphire Object to pin to " + newServerAddress);
            } catch (MicroServiceReplicaNotFoundException e) {
                throw new Error("Could not find replica to pin to " + newServerAddress);
            } catch (NoKernelServerFoundException | KernelObjectNotFoundException e) {
                throw new Error("No kernel servers were found");
            }
        }

        @Override
        protected void addServer(Policy.ServerPolicy server) {
            super.addServer(server);
            dhtChord.add((ServerPolicy) server);
        }

        @Override
        protected void removeServer(Policy.ServerPolicy server) {
            super.removeServer(server);
            // TODO: Need to remove from chord too
        }

        public DHTChord getChord() {
            return this.dhtChord;
        }

        @Override
        public Policy.ServerPolicy onRefRequest() {
            // TODO
            return null;
        }

        /**
         * Get a new host address for the given region.
         *
         * @param region
         * @throws NoKernelServerFoundException
         * @throws RemoteException
         * @throws MicroServiceNotFoundException
         * @throws MicroServiceReplicaNotFoundException
         */
        // TODO: Consider moving to Library so that other DMs can use this. Note that
        // some DMs may want a different behavior for exception e.g., null list should be fine if
        // the DM can switch to another label.
        private InetSocketAddress getAddress(String region)
                throws NoKernelServerFoundException, RemoteException {
            List<InetSocketAddress> addressList =
                    sapphire_getAddressList(spec.getNodeSelectorSpec(), region);
            if (addressList == null || addressList.isEmpty()) {
                String msg =
                        String.format(
                                "No kernel servers were found for %s & %s",
                                spec.getNodeSelectorSpec(), region);
                logger.log(Level.SEVERE, msg);
                throw new NoKernelServerFoundException();
            }

            // TODO: this behavior may need to be changed since it always returns the first address.
            return addressList.get(0);
        }
    }
}

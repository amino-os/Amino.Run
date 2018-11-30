package sapphire.policy.dht;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.NoKernelServerFoundException;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.common.Utils;
import sapphire.policy.DefaultSapphirePolicy;

public class DHTPolicy extends DefaultSapphirePolicy {
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

    public static class DHTClientPolicy extends DefaultClientPolicy {
        private static Logger logger = Logger.getLogger(DHTPolicy.DHTClientPolicy.class.getName());
        private DHTChord dhtChord;

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            DHTKey key = new DHTKey(params.get(0).toString());
            if (dhtChord == null) {
                dhtChord = ((DHTGroupPolicy) getGroup()).getChord();
            }

            DHTNode node = dhtChord.getResponsibleNode(key);
            logger.fine("Responsible node for: " + key + " is: " + node.id);
            return node.server.onRPC(method, params);
        }
    }

    public static class DHTServerPolicy extends DefaultServerPolicy {}

    public static class DHTGroupPolicy extends DefaultGroupPolicy {
        private static Logger logger = Logger.getLogger(DHTGroupPolicy.class.getName());
        private int numOfShards = DEFAULT_NUM_OF_SHARDS;
        private DHTChord dhtChord;

        @Override
        public void onCreate(String region, SapphireServerPolicy server, SapphireObjectSpec spec)
                throws RemoteException {
            InetSocketAddress newServerAddress = null;
            dhtChord = new DHTChord();
            super.onCreate(region, server, spec);

            if (spec != null) {
                Map<String, SapphirePolicyConfig> configMap =
                        Utils.fromDMSpecListToFlatConfigMap(spec.getDmList());
                if (configMap != null) {
                    SapphirePolicyConfig config = configMap.get(DHTPolicy.Config.class.getName());
                    if (config != null) {
                        this.numOfShards = ((Config) config).getNumOfShards();
                    }
                }
            }

            try {
                ArrayList<String> regions = sapphire_getRegions();
                boolean pinned = server.isAlreadyPinned();

                if (!pinned) {
                    InetSocketAddress address = getAddress(region);
                    server.sapphire_pin_to_server(server, address);
                }

                // TODO: Current implementation assumes shards are spread out across regions.
                // This assumption may not be true if the policy wants to locate all shards per
                // region.

                // Create replicas based on annotation
                for (int i = 1; i < numOfShards; i++) {
                    region = regions.get(i % regions.size());
                    if (region == null) {
                        throw new IllegalStateException("no region available for DHT DM");
                    }

                    logger.info(String.format("Creating shard %s in region %s", i, region));
                    InetSocketAddress address = getAddress(region);
                    addReplica(server, address, region, pinned);
                }
            } catch (RemoteException e) {
                throw new Error(
                        "Could not create new group policy because the oms is not available.");
            } catch (SapphireObjectNotFoundException e) {
                throw new Error("Could not find Sapphire Object to pin to " + newServerAddress);
            } catch (SapphireObjectReplicaNotFoundException e) {
                throw new Error("Could not find replica to pin to " + newServerAddress);
            } catch (NoKernelServerFoundException e) {
                throw new Error("No kernel servers were found");
            }
        }

        @Override
        public void addServer(SapphireServerPolicy server) throws RemoteException {
            super.addServer(server);
            dhtChord.add((DHTServerPolicy) server);
        }

        public DHTChord getChord() {
            return this.dhtChord;
        }

        @Override
        public void onFailure(SapphireServerPolicy server) {
            // TODO
        }

        @Override
        public SapphireServerPolicy onRefRequest() {
            // TODO
            return null;
        }

        /**
         * Get a new host address for the given region.
         *
         * @param region
         * @throws NoKernelServerFoundException
         * @throws RemoteException
         * @throws SapphireObjectNotFoundException
         * @throws SapphireObjectReplicaNotFoundException
         */
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

            return addressList.get(0);
        }
    }
}

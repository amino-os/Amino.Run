package amino.run.policy.dht;

import amino.run.app.SapphireObjectSpec;
import amino.run.common.NoKernelServerFoundException;
import amino.run.common.SapphireObjectNotFoundException;
import amino.run.common.SapphireObjectReplicaNotFoundException;
import amino.run.common.Utils;
import amino.run.policy.DefaultSapphirePolicy;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        private static Logger logger = Logger.getLogger(DHTClientPolicy.class.getName());
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
                /* In current implementation, each replica is in a different region. And each replica serves as a shard.
                Skips the region where first shard was created. If number of regions are less than the required number
                of replicas, once we reach the end of regions, start from first region again and continue in
                round robin fashion until all the necessary replicas are created. In effect, ensuring replicas are
                created in unique regions and are evenly distributed to the best. */
                int primaryReplicaIndex = regions.indexOf(region);
                int shardCount = primaryReplicaIndex < numOfShards ? numOfShards : numOfShards - 1;
                // Create replicas based on annotation
                for (int i = 0; i < shardCount; i++) {
                    region = regions.get(i % regions.size());
                    if (region == null) {
                        throw new IllegalStateException("no region available for DHT DM");
                    }

                    if (i == primaryReplicaIndex) {
                        /* Skip the region where first shard was created */
                        continue;
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
        // TODO: Consider moving to SapphirePolicyLibrary so that other DMs can use this. Note that
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

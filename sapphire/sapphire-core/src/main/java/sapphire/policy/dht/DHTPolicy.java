package sapphire.policy.dht;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import sapphire.app.SapphireObjectSpec;
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
                DHTServerPolicy dhtServer = (DHTServerPolicy) server;
                // TODO: hack for demo.
                // Sungwook will raise another PR to address it shortly
                // dhtServer.sapphire_pin(server, regions.get(0));

                // Create replicas based on annotation
                InetSocketAddress newServerAddress = null;
                for (int i = 1; i < numOfShards; i++) {
                    region = regions.get(i % regions.size());
                    if (region == null) {
                        throw new IllegalStateException("no region available for DHT DM");
                    }
                    logger.info(String.format("Creating shard %s in region %s", i, region));

                    // TODO (Sungwook, 2018-10-2) Passing processedPolicies may not be necessary as
                    // they are already available.
                    SapphireServerPolicy replica =
                            dhtServer.sapphire_replicate(server.getProcessedPolicies(), region);

                    // TODO: hack for demo. When we demo DHT + master slave, DHT
                    // is not the last DM, and therefore should not call sapphire_pin
                    // to relocate server policy.
                    // newServerAddress = oms().getServerInRegion(region);
                    // dhtServer.sapphire_pin_to_server(replica,
                    // newServerAddress);
                }
            } catch (RemoteException e) {
                throw new Error(
                        "Could not create new group policy because the oms is not available.");
            }
        }

        @Override
        public void addServer(SapphireServerPolicy server) throws RemoteException {
            super.addServer(server);
            DHTServerPolicy dhtServer = (DHTServerPolicy) server;
            dhtChord.add(dhtServer);
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
    }
}

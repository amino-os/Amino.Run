package sapphire.policy.dht;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
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
            DHTKey key = new DHTKey((String) params.get(0));
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
        public void onCreate(
                SapphireServerPolicy server, Map<String, SapphirePolicyConfig> configMap)
                throws RemoteException {
            dhtChord = new DHTChord();
            super.onCreate(server, configMap);

            if (configMap != null) {
                SapphirePolicyConfig config = configMap.get(DHTPolicy.Config.class.getName());
                if (config != null) {
                    this.numOfShards = ((Config) config).getNumOfShards();
                }
            }

            try {
                ArrayList<String> regions = sapphire_getRegions();
                DHTServerPolicy dhtServer = (DHTServerPolicy) server;

                // Create replicas based on annotation
                InetSocketAddress newServerAddress = null;
                for (int i = 1; i < numOfShards; i++) {
                    newServerAddress = oms().getServerInRegion(regions.get(i % regions.size()));

                    // TODO (Sungwook, 2018-10-2) Passing processedPolicies may not be necessary as
                    // they are already available.
                    SapphireServerPolicy replica =
                            dhtServer.sapphire_replicate(server.getProcessedPolicies());
                    dhtServer.sapphire_pin_to_server(replica, newServerAddress);
                }
                dhtServer.sapphire_pin(server, regions.get(0));
            } catch (RemoteException e) {
                throw new Error(
                        "Could not create new group policy because the oms is not available.");
            } catch (SapphireObjectNotFoundException e) {
                throw new Error("Failed to find sapphire object.", e);
            } catch (SapphireObjectReplicaNotFoundException e) {
                throw new Error("Failed to find sapphire object replica.", e);
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

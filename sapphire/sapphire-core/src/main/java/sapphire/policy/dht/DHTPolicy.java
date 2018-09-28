package sapphire.policy.dht;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Logger;
import sapphire.app.DMSpec;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.policy.DefaultSapphirePolicy;

public class DHTPolicy extends DefaultSapphirePolicy {
    private static final int DEFAULT_NUM_OF_SHARDS = 3;

    public static class Config implements SapphirePolicyConfig {
        private int numOfShards = DEFAULT_NUM_OF_SHARDS;

        public int getNumOfShards() {
            return numOfShards;
        }

        public void setNumOfShards(int numOfShards) {
            this.numOfShards = numOfShards;
        }

        @Override
        public DMSpec toDMSpec() {
            DMSpec spec = new DMSpec();
            spec.setName(DHTPolicy.class.getName());
            spec.addProperty("numOfShards", String.valueOf(numOfShards));
            return spec;
        }

        @Override
        public SapphirePolicyConfig fromDMSpec(DMSpec spec) {
            if (!DHTPolicy.class.getName().equals(spec.getName())) {
                throw new IllegalArgumentException(
                        String.format(
                                "DM %s is not able to process spec %s",
                                DHTPolicy.class.getName(), spec));
            }

            Config config = new Config();
            Map<String, String> properties = spec.getProperties();
            if (properties != null) {
                config.setNumOfShards(Integer.parseInt(properties.get("numOfShards")));
            }

            return config;
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

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface DHTConfigure {
        int numOfShards() default DEFAULT_NUM_OF_SHARDS;
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
        private Random generator = new Random(System.currentTimeMillis());

        @Override
        public void onCreate(SapphireServerPolicy server, Map<String, DMSpec> dmSpecMap)
                throws RemoteException {
            dhtChord = new DHTChord();
            super.onCreate(server, dmSpecMap);

            if (dmSpecMap != null) {
                DMSpec spec = dmSpecMap.get(DHTPolicy.class.getSimpleName());
                if ((spec != null) && (spec.getProperty("numOfShards") != null)) {
                    this.numOfShards = Integer.parseInt(spec.getProperty("numOfShards"));
                }
            }

            try {
                ArrayList<String> regions = sapphire_getRegions();
                DHTServerPolicy dhtServer = (DHTServerPolicy) server;

                // Create replicas based on annotation
                for (int i = 1; i < numOfShards; i++) {
                    DHTServerPolicy replica = (DHTServerPolicy) dhtServer.sapphire_replicate();
                    replica.sapphire_pin(regions.get(i % regions.size()));
                }
                dhtServer.sapphire_pin(regions.get(0));
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
            DHTKey id = new DHTKey(Integer.toString(generator.nextInt(Integer.MAX_VALUE)));
            DHTServerPolicy dhtServer = (DHTServerPolicy) server;
            DHTNode newNode = new DHTNode(id, dhtServer);
            dhtChord.add(newNode);
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

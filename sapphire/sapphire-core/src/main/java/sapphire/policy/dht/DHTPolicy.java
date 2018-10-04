package sapphire.policy.dht;

import static sapphire.common.Utils.getAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Logger;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.policy.DefaultSapphirePolicy;

public class DHTPolicy extends DefaultSapphirePolicy {
    static final int DEFAULT_NUM_OF_SHARDS = 3;

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
        public void onCreate(SapphireServerPolicy server, Annotation[] annotations)
                throws RemoteException {
            dhtChord = new DHTChord();
            super.onCreate(server, annotations);

            DHTConfigure annotation = getAnnotation(annotations, DHTConfigure.class);
            if (annotation != null) {
                this.numOfShards = annotation.numOfShards();
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

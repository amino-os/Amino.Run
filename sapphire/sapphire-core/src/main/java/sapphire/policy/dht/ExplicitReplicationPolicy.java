package sapphire.policy.dht;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Logger;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.policy.DefaultSapphirePolicy;

/**
 * This is a class for testing replication scenario. It creates as many replicas as defined in the
 * configuration.
 */
public class ExplicitReplicationPolicy extends DefaultSapphirePolicy {

    public static class ExplicitReplicationClientPolicy extends DefaultClientPolicy {

        // Signature for Sapphire Policy method (as opposed to application method).
        String sapphirePolicyStr = "sapphire.policy";
        ExplicitReplicationServerPolicy server = null;
        ExplicitReplicationGroupPolicy group = null;

        @Override
        public void onCreate(SapphireGroupPolicy group, Annotation[] annotations) {
            this.group = (ExplicitReplicationGroupPolicy) group;
        }

        @Override
        public void setServer(SapphireServerPolicy server) {
            this.server = (ExplicitReplicationServerPolicy) server;
        }

        @Override
        public SapphireServerPolicy getServer() {
            return server;
        }

        @Override
        public SapphireGroupPolicy getGroup() {
            return group;
        }

        @Override
        /** params may be an application parameter or method name in case of multi-DM. */
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            // This code path is about calling method in application.
            System.out.println("Client) onRPC ExplicitReplicationPolicy");

            ArrayList<SapphireServerPolicy> allServers = group.getServers();
            Object ret = null;
            for (SapphireServerPolicy server : allServers) {
                ret = server.onRPC(method, params);
            }

            return ret;
        }
    }

    public static class ExplicitReplicationServerPolicy extends DefaultServerPolicy {
        private static Logger logger = Logger.getLogger(DefaultServerPolicy.class.getName());
        private ExplicitReplicationGroupPolicy group = null;

        @Override
        public void onCreate(SapphireGroupPolicy group, Annotation[] annotations) {
            this.group = (ExplicitReplicationGroupPolicy) group;
        }

        @Override
        public void initialize() {}

        @Override
        public SapphireGroupPolicy getGroup() {
            return group;
        }

        @Override
        public void onMembershipChange() {}

        @Override
        /** params may be an application parameter or method name in case of multi-DM. */
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            System.out.println("Server) onRPC at ExplicitReplicationPolicy called.");
            return super.onRPC(method, params);
        }
    }

    public static class ExplicitReplicationGroupPolicy extends DefaultGroupPolicy {
        private static Logger logger = Logger.getLogger(DefaultGroupPolicy.class.getName());
        private HashMap<Integer, ReplicationNode> nodes;
        private int totalMembers = 2;
        private int kernelServerBeginIdForExplicitReplication =
                1; // replication chain will be put on from the third kernel server.
        private int id = 0;
        private int membersAdded = 0;

        private class ReplicationNode implements Serializable {
            public int id;
            public ExplicitReplicationServerPolicy server;

            public ReplicationNode(int id, ExplicitReplicationServerPolicy server) {
                this.id = id;
                this.server = server;
            }
        }

        @Override
        public void onCreate(SapphireServerPolicy server, Annotation[] annotations) {
            nodes = new HashMap<Integer, ReplicationNode>();
            System.out.println("Group.onCreate at ReplicationPolicy");
            try {
                ArrayList<String> regions = sapphire_getRegions();

                // Add the first DHT node
                ExplicitReplicationServerPolicy replicationServer =
                        (ExplicitReplicationServerPolicy) server;

                ReplicationNode newNode = new ReplicationNode(this.id, replicationServer);
                nodes.put(this.id, newNode);
                this.membersAdded++;

                InetSocketAddress newServerAddress = null;
                for (int i = this.kernelServerBeginIdForExplicitReplication;
                        i < this.kernelServerBeginIdForExplicitReplication + this.totalMembers - 1;
                        i++) {
                    newServerAddress = oms().getServerInRegion(regions.get(i));
                    SapphireServerPolicy replica =
                            replicationServer.sapphire_replicate(server.getProcessedPolicies());
                    replicationServer.sapphire_pin_to_server(replica, newServerAddress);
                }
                replicationServer.sapphire_pin(regions.get(0));
            } catch (RemoteException e) {
                e.printStackTrace();
                throw new Error(
                        "Could not create new group policy because the oms is not available.");
            } catch (SapphireObjectNotFoundException e) {
                throw new Error("Failed to find sapphire object.", e);
            } catch (SapphireObjectReplicaNotFoundException e) {
                throw new Error("Failed to find sapphire object replica.", e);
            }
        }

        @Override
        public void addServer(SapphireServerPolicy server) {
            try {
                ExplicitReplicationServerPolicy replicationServer =
                        (ExplicitReplicationServerPolicy) server;
                ReplicationNode newNode = new ReplicationNode(this.membersAdded, replicationServer);
                nodes.put(this.membersAdded, newNode);
                this.membersAdded++;
            } catch (Exception e) {
                e.printStackTrace();
                throw new Error("Could not add DHTServer.");
            }

            logger.info("Members added so far = " + this.membersAdded);
        }

        @Override
        public ArrayList<SapphireServerPolicy> getServers() {
            ArrayList servers = new ArrayList<SapphireServerPolicy>();
            for (int id : nodes.keySet()) {
                servers.add(nodes.get(id).server);
            }

            return servers;
        }

        @Override
        public void removeServer(SapphireServerPolicy server) {}

        @Override
        public void onFailure(SapphireServerPolicy server) {
            // TODO
        }

        @Override
        public SapphireServerPolicy onRefRequest() {
            // TODO
            return null;
        }

        /** TODO: key is literally from the input value currently. It should be hashed key. */
        public ExplicitReplicationServerPolicy getRandomResponsibleNode() {
            Random randInt = new Random();
            int selectedId = randInt.nextInt(this.membersAdded);
            if (nodes.containsKey(selectedId)) {
                return nodes.get(selectedId).server;
            }

            System.out.println("No members exist for " + selectedId);
            return null;
        }
    }
}

package sapphire.policy.dht;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.graalvm.collections.Pair;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.oms.GlobalKernelObjectManager;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.SapphirePolicy;

// TODO (Sungwook, 2018-10-2) Discard after updating original DHT policy to work with multi policy
// chain.
public class DHTPolicy2 extends DefaultSapphirePolicy {

    public static class DHTClientPolicy extends DefaultClientPolicy {

        // Signature for Sapphire Policy method (as opposed to application method).
        String sapphirePolicyStr = "sapphire.policy";
        DHTServerPolicy server = null;
        DHTGroupPolicy group = null;

        @Override
        public void onCreate(
                SapphireGroupPolicy group, Map<String, SapphirePolicyConfig> configMap) {
            this.group = (DHTGroupPolicy) group;
        }

        @Override
        public void setServer(SapphireServerPolicy server) {
            this.server = (DHTServerPolicy) server;
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
        /**
         * Finds the responsible node based on the value of the first parameter in application, and
         * calls the method. If the call is not about application (e.g. call server policy method),
         * calls the first node from the server list. Note that params may contain another (method
         * name, ArrayLisy params) and so on.
         *
         * @param method Method name. Method can be either for policy or application.
         * @param params Parameters for the method.
         * @throws Exception
         */
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {

            ArrayList<Object> applicationParams = getApplicationParam(method, params);
            DHTServerPolicy responsibleNode;
            if (applicationParams != null && applicationParams.size() > 0) {
                responsibleNode =
                        group.dhtGetResponsibleNode(
                                Integer.parseInt(applicationParams.get(0).toString()));
            } else {
                responsibleNode = (DHTServerPolicy) group.getServers().get(0);
            }

            return responsibleNode.onRPC(method, params);
        }

        /**
         * Find the application parameters, and return them.
         *
         * @param method Method name. Method can be either for policy or application.
         * @param params Parameters for the method.
         * @return Application parameters.
         */
        private ArrayList<Object> getApplicationParam(String method, ArrayList<Object> params) {
            ArrayList<Object> currentParams = params;
            Object firstParam = method;

            while (currentParams != null && currentParams.size() == 2) {
                if (currentParams.get(1) instanceof ArrayList) {
                    currentParams = (ArrayList) currentParams.get(1);
                    firstParam = currentParams.get(0);
                } else {
                    break;
                }
            }

            if (firstParam instanceof String && ((String) firstParam).contains(sapphirePolicyStr)) {
                return null;
            }

            return currentParams;
        }
    }

    public static class DHTServerPolicy extends DefaultServerPolicy {
        private static Logger logger = Logger.getLogger(DefaultServerPolicy.class.getName());
        private DHTGroupPolicy group = null;

        @Override
        public void onCreate(
                SapphireGroupPolicy group, Map<String, SapphirePolicyConfig> configMap) {
            this.group = (DHTGroupPolicy) group;
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
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            return super.onRPC(method, params);
        }
    }

    public static class DHTGroupPolicy extends DefaultGroupPolicy {
        private static Logger logger = Logger.getLogger(DefaultGroupPolicy.class.getName());
        private HashMap<Integer, DHTNode> nodes = new HashMap<Integer, DHTNode>();
        private int groupSize = 0;
        private String defaultRegion = "Region1";

        private class DHTNode implements Serializable {
            public int id;
            public DHTServerPolicy server;

            public DHTNode(int id, DHTServerPolicy server) {
                this.id = id;
                this.server = server;
            }
        }

        public DHTGroupPolicy() {
            setDefaultRegion(defaultRegion);
        }

        @Override
        public void onCreate(
                SapphireServerPolicy server,
                Map<String, SapphirePolicyConfig> configMap,
                String regionRestriction)
                throws RemoteException {
            // TODO (Sungwook): Is this sufficient? What about addserver for replica? How was
            // original code working?
            super.onCreate(server, configMap);

            nodes = new HashMap<Integer, DHTNode>();

            try {
                ArrayList<String> regions = sapphire_getRegions();

                // Add the first DHT node
                addServer(server);
                DHTServerPolicy dhtServer = (DHTServerPolicy) server;

                if (regionRestriction != null) {
                    List<InetSocketAddress> newServerAddresses =
                            oms().getServersInRegion(regionRestriction);

                    for (int i = 1; i < newServerAddresses.size(); i++) {
                        SapphireServerPolicy replica =
                                dhtServer.sapphire_replicate(
                                        server.getProcessedPolicies(), regionRestriction);
                        KernelOID replicaOid = replica.$__getKernelOID();

                        addServer(replica);
                        System.out.println(
                                "(DHT2) Pin replica "
                                        + replica.$__getKernelOID()
                                        + " to "
                                        + newServerAddresses.get(i).getHostName()
                                        + " at restricted "
                                        + regionRestriction);
//                        dhtServer.sapphire_pin_to_server(replica, newServerAddresses.get(i));
                    }
                } else {
                    for (int i = 1; i < regions.size(); i++) {
                        InetSocketAddress newServerAddress =
                                oms().getServerInRegion(regions.get(i));
                        SapphireServerPolicy replica =
                                dhtServer.sapphire_replicate(
                                        server.getProcessedPolicies(), regions.get(i));
                        KernelOID replicaOid = replica.$__getKernelOID();
                        addServer(replica);
                        // replica is null if it was migrated.
                        System.out.println(
                                "(DHT2) Pin replica "
                                        + replica.$__getKernelOID()
                                        + " to "
                                        + newServerAddress.getHostName()
                                        + " at "
                                        + regions.get(i));
//                        dhtServer.sapphire_pin_to_server(replica, newServerAddress);
                    }
                }

                if (regionRestriction == null) {
                    regionRestriction = regions.get(0);
                }
                KernelOID oid = server.$__getKernelOID();

                System.out.println(
                        "(DHT2) Pin original "
                                + oid
                                + " to "
                                + server
                                + " at "
                                + regionRestriction);
//                dhtServer.sapphire_pin(server, regionRestriction);

            } catch (RemoteException e) {
                e.printStackTrace();
                throw new Error(
                        "Could not create new group policy because the oms is not available.");
            }
        }

        @Override
        public void addServer(SapphireServerPolicy server) {
            try {
                DHTServerPolicy dhtServer = (DHTServerPolicy) server;

                for (DHTNode node : nodes.values()) {
                    if (dhtServer == node.server) {
                        int id = node.id;
                        DHTNode newNode = new DHTNode(id, dhtServer);
                        nodes.replace(id, node, newNode);
                        return;
                    }
                }

                // Create a new node and add to the node list.
                groupSize++;
                DHTNode newNode = new DHTNode(groupSize, dhtServer);
                nodes.put(groupSize, newNode);
            } catch (Exception e) {
                e.printStackTrace();
                throw new Error("Could not add DHTServer.");
            }

            logger.info("NODES: " + nodes.toString() + " Group size = " + groupSize);
        }

        @Override
        public ArrayList<SapphireServerPolicy> getServers() {
            ArrayList servers = new ArrayList<SapphireServerPolicy>();
            for (int id : nodes.keySet()) {
                servers.add(nodes.get(id).server);
            }

            return servers;
        }

        /** TODO: key is literally from the input value currently. It should be hashed key. */
        public DHTServerPolicy dhtGetResponsibleNode(int key) {
            key = key % groupSize + 1;
            if (nodes.containsKey(key)) {
                DHTServerPolicy server = nodes.get(key).server;
                logger.info("Retrieved the server for key " + key);
                return server;
            }

            logger.severe("Could not find server for key " + key);
            return null;
        }
    }
}
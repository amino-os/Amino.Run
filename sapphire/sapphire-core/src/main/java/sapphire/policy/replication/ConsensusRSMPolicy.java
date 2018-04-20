package sapphire.policy.replication;


import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.util.consensus.raft.StateMachineApplier;

/**
 * Created by quinton on 1/31/18.
 * Single cluster replicated SO w/ atomic RPCs across at least f + 1 replicas, using RAFT algorithm.
 * **/

public class ConsensusRSMPolicy extends DefaultSapphirePolicy {
    public static class RPC {
        String method;
        ArrayList<Object> params;
        public RPC(String method, ArrayList<Object> params) {
            this.method = method;
            this.params = params;
        }
    }

    public static class ClientPolicy extends DefaultSapphirePolicy.DefaultClientPolicy {}

    // TODO: ServerPolicy needs to be Serializable
    public static class ServerPolicy extends DefaultSapphirePolicy.DefaultServerPolicy  implements StateMachineApplier {
        static Logger logger = Logger.getLogger(ServerPolicy.class.getCanonicalName());
        // There are so many servers and clients in this code,
        // include full package name to make it clear to the reader.
        transient sapphire.policy.util.consensus.raft.Server raftServer;

        public sapphire.policy.util.consensus.raft.Server getRaftServer() {
            return raftServer;
        }

        @Override
        public void onCreate(SapphireGroupPolicy group) {
            super.onCreate(group);
            raftServer = new sapphire.policy.util.consensus.raft.Server(this);
        }

        /** TODO: Handle added and failed servers - i.e. quorum membership changes
        @Override
        public void onMembershipChange() {
            super.onMembershipChange();
            for(SapphireServerPolicy server: this.getGroup().getServers()) {
                ServerPolicy consensusServer = (ServerPolicy)server;
                this.raftServer.addServer(consensusServer.getRaftServer().getMyServerID(), consensusServer.getRaftServer());
            }
        }
         */

        /**
         * Initialize the RAFT protocol with the specified set of servers.
         * @param raftServers
         */
        public void initializeRaft(ConcurrentMap<UUID, ServerPolicy> raftServers){
            for(UUID id: raftServers.keySet()) {
                // TODO: We should add ServerPolicy, rather than Raft server,
                // because ServerPolicy has RMI capabilities.
                this.raftServer.addServer(id, raftServers.get(id).getRaftServer());
            }
            this.raftServer.start();
        }

        // TODO: This method should be thread safe
        // We should not have multiple threads calling apply concurrently
        // If we implement log compaction in the future, we also need to
        // ensure snapshot operation and apply operation are synchronized.
        public Object apply(Object operation) throws Exception {
            RPC rpc = (RPC)operation;
            logger.info(String.format("Applying %s(%s)", rpc.method, rpc.params));
            if (rpc.params == null) {
                rpc.params = new ArrayList<Object>();
            }
            // TODO: AppObject is not being initialized correctly.
            // Presumably because stub's have not yet been generated.
            // : return super.onRPC(rpc.method, rpc.params);
            return null;
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            return raftServer.applyToStateMachine(new RPC(method, params)); // first commit it to the logs of a consensus of replicas.
        }
    }

    // TODO: Group policy needs to be Serializable
    public static class GroupPolicy extends DefaultSapphirePolicy.DefaultGroupPolicy {
        private static Logger logger = Logger.getLogger(GroupPolicy.class.getName());
        public void onCreate(SapphireServerPolicy server) {
            try {
                ArrayList<String> regions = sapphire_getRegions();
                // Register the first replica, which has already been created.
                ServerPolicy consensusServer = (ServerPolicy) server;
                addServer(consensusServer);
                // Create additional replicas, one per region. TODO:  Create N-1 replicas on different servers in the same zone.
                for (int i = 1; i < regions.size(); i++) {
                    ServerPolicy replica = (ServerPolicy)consensusServer.sapphire_replicate();
                    replica.sapphire_pin(regions.get(i));
                    addServer(replica);
                }
                consensusServer.sapphire_pin(regions.get(0));
                // Tell all the servers about one another
                ConcurrentHashMap<UUID, ServerPolicy> allServers = new ConcurrentHashMap<UUID, ServerPolicy>();
                // First get the self-assigned ID from each server
                List<SapphireServerPolicy> servers = getServers();
                for(SapphireServerPolicy i: servers) {
                    ServerPolicy s = (ServerPolicy)i;
                    allServers.put(s.getRaftServer().getMyServerID(), s);
                }
                // Now tell each server about the location and ID of all the servers, and start the RAFT protocol on each server.
                for(ServerPolicy s: allServers.values()) {
                    s.initializeRaft(allServers);
                }

            } catch (RemoteException e) {
                // TODO: Sapphire Group Policy Interface does not allow throwing exceptions, so in the mean time convert to an Error.
                throw new Error("Could not create new group policy because the oms is not available.", e);
            }
        }
    }
}
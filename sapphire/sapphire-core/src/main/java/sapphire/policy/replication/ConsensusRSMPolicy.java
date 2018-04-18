package sapphire.policy.replication;


import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
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

    public static class ServerPolicy extends DefaultSapphirePolicy.DefaultServerPolicy  implements StateMachineApplier {
        static Logger logger = Logger.getLogger(ServerPolicy.class.getCanonicalName());
        // There are so many servers and clients in this code,
        // include full package name to make it clear to the reader.
        sapphire.policy.util.consensus.raft.Server raftServer;

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
            UUID myUUID = raftServer.getMyServerID();
            raftServer = new sapphire.policy.util.consensus.raft.Server(this);
            raftServer.setMyServerID(myUUID);
            for(UUID id: raftServers.keySet()) {
                this.raftServer.addServer(id, raftServers.get(id).getRaftServer());
            }
            this.raftServer.start();
        }
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

        public ConsensusRSMPolicy.ServerPolicy onSapphireObjectReplicate() {
            return (ConsensusRSMPolicy.ServerPolicy) this.sapphire_replicate();
        }
        public void onSapphirePin(String region) throws RemoteException {
            sapphire_pin(region);
        }
    }

    public static class GroupPolicy extends DefaultSapphirePolicy.DefaultGroupPolicy {
        private static Logger logger = Logger.getLogger(GroupPolicy.class.getName());
        private ArrayList<ServerPolicy> servers;
        public void onCreate(SapphireServerPolicy server) {
            try {
                servers = new ArrayList<ServerPolicy>();
                ArrayList<String> regions = sapphire_getRegions();
                // Register the first replica, which has already been created.
                ServerPolicy consensusServer = (ServerPolicy) server;
                servers.add(consensusServer);
                // Create additional replicas, one per region. TODO:  Create N-1 replicas on different servers in the same zone.
                for (int i = 1; i < regions.size(); i++) {
                    ServerPolicy replica = (ServerPolicy)consensusServer.onSapphireObjectReplicate();
                    replica.onSapphirePin(regions.get(i));
                    servers.add(replica);
                }
                consensusServer.onSapphirePin(regions.get(0));
                // Tell all the servers about one another
                ConcurrentHashMap<UUID, ServerPolicy> allServers = new ConcurrentHashMap<UUID, ServerPolicy>();
                // First get the self-assigned ID from each server
                for(ServerPolicy s: servers) {
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

        @Override
        public ArrayList<SapphireServerPolicy> getServers() {
            // In case our parent has servers too, add ours to theirs and return the union.
            ArrayList<SapphireServerPolicy> servers = super.getServers();
            if (servers==null) {
                servers = new ArrayList<SapphireServerPolicy>();
            }
            servers.addAll(this.servers);
            return servers;
        }
    }
}
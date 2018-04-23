package sapphire.policy.replication;


import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.util.consensus.raft.Server;
import sapphire.policy.util.consensus.raft.StateMachineApplier;

/**
 * Created by quinton on 1/31/18.
 * Single cluster replicated SO w/ atomic RPCs across at least f + 1 replicas, using RAFT algorithm.
 * **/

public class ConsensusRSMPolicy extends DefaultSapphirePolicy  {
    public static class RPC implements Serializable {
        String method;
        ArrayList<Object> params;
        public RPC(String method, ArrayList<Object> params) {
            this.method = method;
            this.params = params;
        }
    }

    /* Class to make raft rpc calls to remote servers */
    public static class RemoteRaftServerInvocation implements sapphire.policy.util.consensus.raft.RemoteRaftServerInvoker {
        @Override
        public int requestVote(Object remoteServer, int term, UUID candidate, int lastLogIndex, int lastLogTerm) throws Server.InvalidTermException, Server.AlreadyVotedException, Server.CandidateBehindException, java.lang.Exception {
            Integer retVal;
            ArrayList<Object> args = new ArrayList<Object>();
            args.add(new Integer(term));
            args.add(candidate);
            args.add(new Integer(lastLogIndex));
            args.add(new Integer(lastLogTerm));

            if (!(remoteServer instanceof ServerPolicy)) {
                return 0;
            }

            retVal = (Integer)((ServerPolicy)remoteServer).requestVote(args);
            if (null == retVal) {
                return 0;
            }

            return retVal.intValue();
        }

        @Override
        public Object applyToStateMachine(Object remoteServer, Object operation) throws java.lang.Exception {
            if (!(remoteServer instanceof ServerPolicy)) {
                return null;
            }

            return ((ServerPolicy)remoteServer).applyToStateMachine(operation);
        }

        @Override
        public int appendEntries(Object remoteServer, int term, UUID leader, int prevLogIndex, int prevLogTerm, List<Object> entries, int leaderCommit) throws java.lang.Exception {
            Integer retVal;
            ArrayList<Object> args = new ArrayList<Object>();
            args.add(new Integer(term));
            args.add(leader);
            args.add(new Integer(prevLogIndex));
            args.add(new Integer(prevLogTerm));
            args.add(new Integer(leaderCommit));
            args.addAll(entries);
            if (!(remoteServer instanceof ServerPolicy)) {
                return 0;
            }

            retVal = (Integer)((ServerPolicy)remoteServer).appendEntries(args);
            if (null == retVal) {
                return 0;
            }

            return retVal.intValue();
        }
    }

    public static class ClientPolicy extends DefaultSapphirePolicy.DefaultClientPolicy {}

    // TODO: ServerPolicy needs to be Serializable
    public static class ServerPolicy extends DefaultSapphirePolicy.DefaultServerPolicy  implements StateMachineApplier {
        static Logger logger = Logger.getLogger(ServerPolicy.class.getCanonicalName());
        // There are so many servers and clients in this code,
        // include full package name to make it clear to the reader.
        transient private RemoteRaftServerInvocation remoteRaftServerInvocation;
        transient private sapphire.policy.util.consensus.raft.Server raftServer;

        public sapphire.policy.util.consensus.raft.Server getRaftServer() {
            return raftServer;
        }

        @Override
        public void onCreate(SapphireGroupPolicy group) {
            super.onCreate(group);
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
         * Initialize the local RAFT Server instance.
         */
        public void initializeRaftServer() {
            remoteRaftServerInvocation = new RemoteRaftServerInvocation();
            raftServer = new sapphire.policy.util.consensus.raft.Server(this, remoteRaftServerInvocation);
        }

        /**
         * Initialize the RAFT protocol with the specified set of servers.
         * @param raftServers
         */
        public void initializeRaft(ConcurrentMap<UUID, ServerPolicy> raftServers){
            for(UUID id: raftServers.keySet()) {
                this.raftServer.addServer(id, raftServers.get(id));
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

        // Invoked by remote candidate server to gather vote from this server
        public Object requestVote(ArrayList<Object> params) throws java.lang.Exception {
            int term = ((Integer)params.get(0)).intValue();
            UUID candidate = (UUID)params.get(1);
            int lastLogIndex = ((Integer)params.get(2)).intValue();
            int lastLogTerm = ((Integer)params.get(3)).intValue();
            return raftServer.requestVote(term, candidate, lastLogIndex, lastLogTerm);
        }

        // Invoked by followers to redirect the client's operation invoked them to get applied on leader
        public Object applyToStateMachine(Object operation) throws java.lang.Exception {
           return raftServer.applyToStateMachine(operation);
        }

        // Invoked by leader server to replicate log entries to this server
        public Object appendEntries(ArrayList<Object> params) throws java.lang.Exception {
            int term = ((Integer)params.get(0)).intValue();
            UUID leader = (UUID)params.get(1);
            int prevLogIndex = ((Integer)params.get(2)).intValue();
            int prevLogTerm = ((Integer)params.get(3)).intValue();
            int leaderCommit = ((Integer)params.get(4)).intValue();
            List<Object> entries = params.subList(5, params.size());
            return raftServer.appendEntries(term, leader, prevLogIndex, prevLogTerm, entries, leaderCommit);
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
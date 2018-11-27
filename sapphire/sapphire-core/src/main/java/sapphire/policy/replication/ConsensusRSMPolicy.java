package sapphire.policy.replication;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.util.consensus.raft.AlreadyVotedException;
import sapphire.policy.util.consensus.raft.CandidateBehindException;
import sapphire.policy.util.consensus.raft.InvalidLogIndex;
import sapphire.policy.util.consensus.raft.InvalidTermException;
import sapphire.policy.util.consensus.raft.LeaderException;
import sapphire.policy.util.consensus.raft.LogEntry;
import sapphire.policy.util.consensus.raft.PrevLogTermMismatch;
import sapphire.policy.util.consensus.raft.RemoteRaftServer;
import sapphire.policy.util.consensus.raft.StateMachineApplier;

/**
 * Created by quinton on 1/31/18. Single cluster replicated SO w/ atomic RPCs across at least f + 1
 * replicas, using RAFT algorithm. *
 */
public class ConsensusRSMPolicy extends DefaultSapphirePolicy {
    public static class RPC implements Serializable {
        String method;
        ArrayList<Object> params;

        public RPC(String method, ArrayList<Object> params) {
            this.method = method;
            this.params = params;
        }
    }

    public static class ClientPolicy extends DefaultSapphirePolicy.DefaultClientPolicy {
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            Object ret = null;

            try {
                ret = getServer().onRPC(method, params);
            } catch (LeaderException e) {

                if (null == e.getLeader()) {
                    throw new RemoteException("Raft leader is not elected");
                }

                setServer((ServerPolicy) e.getLeader());
                ret = ((ServerPolicy) e.getLeader()).onRPC(method, params);
            } catch (InvocationTargetException e) {
                /*
                 * Added for handling Multi-DM scenarios where LeaderException can be nested inside InvocationTargetException.
                 * Check whether the received InvocationTargetException is LeaderException which needs to be handled.
                 */

                if (e.getTargetException() instanceof LeaderException) {
                    LeaderException le = (LeaderException) e.getTargetException();
                    if (null == le.getLeader()) {
                        throw new RemoteException("Raft leader is not elected");
                    }
                    setServer((ServerPolicy) le.getLeader());
                    ret = ((ServerPolicy) le.getLeader()).onRPC(method, params);
                }
            } catch (RemoteException e) {
                /* Get servers from the group and find a responding server */
                boolean serverFound = false;
                ArrayList<SapphireServerPolicy> servers = getGroup().getServers();
                for (SapphireServerPolicy server : servers) {
                    /* Excluding the server failed to respond in the above try block */
                    if (getServer().$__getKernelOID().equals(server.$__getKernelOID())) {
                        continue;
                    }

                    try {
                        ret = server.onRPC(method, params);
                        serverFound = true;
                        break;
                    } catch (RemoteException re) {
                        /* Try with next server */
                    } catch (sapphire.policy.util.consensus.raft.LeaderException le) {
                        /* Store this server as reachable and use it for the rpcs to follow */
                        if (null == le.getLeader()) {
                            setServer(server);
                            throw new RemoteException("Raft leader is not elected");
                        }

                        setServer(((ServerPolicy) le.getLeader()));
                        ret = ((ServerPolicy) le.getLeader()).onRPC(method, params);
                        serverFound = true;
                        break;
                    }
                }

                /* Responding server not found */
                if (true != serverFound) {
                    throw new RemoteException("Failed to connect atleast one server");
                }
            }

            return ret;
        }
    }

    // TODO: ServerPolicy needs to be Serializable
    public static class ServerPolicy extends DefaultSapphirePolicy.DefaultServerPolicy
            implements StateMachineApplier, RemoteRaftServer {
        static Logger logger = Logger.getLogger(ServerPolicy.class.getCanonicalName());
        // There are so many servers and clients in this code,
        // include full package name to make it clear to the reader.
        private transient sapphire.policy.util.consensus.raft.Server raftServer;

        public UUID getRaftServerId() {
            return raftServer.getMyServerID();
        }

        @Override
        public int appendEntries(
                int term,
                UUID leader,
                int prevLogIndex,
                int prevLogTerm,
                List<LogEntry> entries,
                int leaderCommit)
                throws InvalidTermException, PrevLogTermMismatch, InvalidLogIndex {
            return raftServer.appendEntries(
                    term, leader, prevLogIndex, prevLogTerm, entries, leaderCommit);
        }

        @Override
        public int requestVote(int term, UUID candidate, int lastLogIndex, int lastLogTerm)
                throws InvalidTermException, AlreadyVotedException, CandidateBehindException {
            return raftServer.requestVote(term, candidate, lastLogIndex, lastLogTerm);
        }

        @Override
        public Object applyToStateMachine(Object operation) throws Exception {
            return raftServer.applyToStateMachine(operation);
        }

        @Override
        public void onCreate(SapphireGroupPolicy group, SapphireObjectSpec spec) {
            super.onCreate(group, spec);
            raftServer = new sapphire.policy.util.consensus.raft.Server(this);
        }

        /**
         * TODO: Handle added and failed servers - i.e. quorum membership changes @Override public
         * void onMembershipChange() { super.onMembershipChange(); for(SapphireServerPolicy server:
         * this.getGroup().getServers()) { ServerPolicy consensusServer = (ServerPolicy)server;
         * this.raftServer.addServer(consensusServer.getRaftServer().getMyServerID(),
         * consensusServer.getRaftServer()); } }
         */

        /**
         * Initialize the RAFT protocol with the specified set of servers.
         *
         * @param servers
         */
        public void initializeRaft(ConcurrentMap<UUID, ServerPolicy> servers) {
            // Need to update each raft server about all the other raft servers.
            UUID raftId = null;
            for (ServerPolicy s : servers.values()) {
                raftId = s.getRaftServerId();
                for (UUID id : servers.keySet()) {
                    if ((!id.equals(raftId))
                            && ((id.equals(this.getRaftServerId()))
                                    || (raftId.equals(this.getRaftServerId())))) {
                        s.addRaftServer(id, servers.get(id));
                    }
                }

                // After updation of the raft servers, need to start the current raft Server,
                // which is the newly added raft server.
                if (this.getRaftServerId().equals(raftId)) {
                    this.raftServer.start();
                }
            }
        }

        /**
         * Add a remote raft server, to the current raft server's otherServers Map.
         *
         * @param id: Remote raft server's UUID
         * @param server: Remote raft server
         */
        public void addRaftServer(UUID id, RemoteRaftServer server) {
            this.raftServer.addServer(id, server);
        }

        // TODO: This method should be thread safe
        // We should not have multiple threads calling apply concurrently
        // If we implement log compaction in the future, we also need to
        // ensure snapshot operation and apply operation are synchronized.
        public Object apply(Object operation) throws Exception {
            RPC rpc = (RPC) operation;
            logger.fine(String.format("Applying %s(%s)", rpc.method, rpc.params));
            if (rpc.params == null) {
                rpc.params = new ArrayList<Object>();
            }

            return super.onRPC(rpc.method, rpc.params);
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            return raftServer.applyToStateMachine(
                    new RPC(
                            method,
                            params)); // first commit it to the logs of a consensus of replicas.
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (raftServer != null) {
                raftServer.stop();
            }
        }
    }

    // TODO: Group policy needs to be Serializable
    public static class GroupPolicy extends DefaultSapphirePolicy.DefaultGroupPolicy {
        private static Logger logger = Logger.getLogger(GroupPolicy.class.getName());

        @Override
        public void onCreate(String region, SapphireServerPolicy server, SapphireObjectSpec spec)
                throws RemoteException {

            super.onCreate(region, server, spec);
            List<InetSocketAddress> addressList = null;

            try {
                addressList = sapphire_getAddressList(spec.getNodeSelectorSpec(), region);

                // Register the first replica, which has already been created.
                ServerPolicy consensusServer = (ServerPolicy) server;
                // Create additional replicas as per the nodeSelector spec.
                for (int i = 1; i < addressList.size(); i++) {
                    addReplica(consensusServer, addressList.get(i), region);
                }

                // The first in the addressList is for primary policy chain.
                consensusServer.sapphire_pin_to_server(server, addressList.get(0));
            } catch (RemoteException e) {
                // TODO: Sapphire Group Policy Interface does not allow throwing exceptions, so in
                // the mean time convert to an Error.
                throw new Error(
                        "Could not create new group policy because the oms is not available.", e);
            } catch (SapphireObjectNotFoundException e) {
                throw new Error("Failed to find sapphire object.", e);
            } catch (SapphireObjectReplicaNotFoundException e) {
                throw new Error("Failed to find sapphire object replica.", e);
            }
        }

        @Override
        public void addServer(SapphireServerPolicy server) throws RemoteException {
            super.addServer(server);

            // Tell all the servers about one another
            ConcurrentHashMap<UUID, ServerPolicy> allServers =
                    new ConcurrentHashMap<UUID, ServerPolicy>();
            // First get the self-assigned ID from each server
            List<SapphireServerPolicy> servers = getServers();
            for (SapphireServerPolicy i : servers) {
                ServerPolicy s = (ServerPolicy) i;
                allServers.put(s.getRaftServerId(), s);
            }

            // Now tell each server about the location and ID of the new raft server,
            // and start the RAFT protocol on the current server.
            ServerPolicy s = (ServerPolicy) server;
            s.initializeRaft(allServers);
        }
    }
}

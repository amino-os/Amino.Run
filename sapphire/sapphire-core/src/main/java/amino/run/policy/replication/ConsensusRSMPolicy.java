package amino.run.policy.replication;

import amino.run.app.MicroServiceSpec;
import amino.run.common.SapphireObjectNotFoundException;
import amino.run.common.SapphireObjectReplicaNotFoundException;
import amino.run.policy.DefaultPolicy;
import amino.run.policy.Policy;
import amino.run.policy.util.consensus.raft.AlreadyVotedException;
import amino.run.policy.util.consensus.raft.CandidateBehindException;
import amino.run.policy.util.consensus.raft.InvalidLogIndex;
import amino.run.policy.util.consensus.raft.InvalidTermException;
import amino.run.policy.util.consensus.raft.LeaderException;
import amino.run.policy.util.consensus.raft.LogEntry;
import amino.run.policy.util.consensus.raft.PrevLogTermMismatch;
import amino.run.policy.util.consensus.raft.RemoteRaftServer;
import amino.run.policy.util.consensus.raft.Server;
import amino.run.policy.util.consensus.raft.StateMachineApplier;
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

/**
 * Created by quinton on 1/31/18. Single cluster replicated SO w/ atomic RPCs across at least f + 1
 * replicas, using RAFT algorithm. *
 */
public class ConsensusRSMPolicy extends DefaultPolicy {
    public static class RPC implements Serializable {
        String method;
        ArrayList<Object> params;

        public RPC(String method, ArrayList<Object> params) {
            this.method = method;
            this.params = params;
        }
    }

    public static class ClientPolicy extends DefaultPolicy.DefaultClientPolicy {
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            /* TODO: Note that this is currently the only DM that calls {@link setServer}.
             * setServer should only ever be called by the kernel.  Remove calls to setServer and keep
             * track of the master via a separate mechanism.
             */
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
                ArrayList<Policy.ServerPolicy> servers = getGroup().getServers();
                for (Policy.ServerPolicy server : servers) {
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
                    } catch (LeaderException le) {
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

    public static class ServerPolicy extends DefaultPolicy.DefaultServerPolicy
            implements StateMachineApplier, RemoteRaftServer {
        static Logger logger = Logger.getLogger(ServerPolicy.class.getCanonicalName());
        // There are so many servers and clients in this code,
        // include full package name to make it clear to the reader.
        private transient Server raftServer;

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
        public void onCreate(Policy.GroupPolicy group, MicroServiceSpec spec) {
            super.onCreate(group, spec);
            raftServer = new Server(this);
        }

        /**
         * TODO: Handle added and failed servers - i.e. quorum membership changes @Override public
         * void onMembershipChange() { super.onMembershipChange(); for(ServerPolicy server:
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
            for (UUID id : servers.keySet()) {
                if (!id.equals(raftServer.getMyServerID())) {
                    this.raftServer.addServer(id, servers.get(id));
                }
            }
            this.raftServer.start();
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

    public static class GroupPolicy extends DefaultPolicy.DefaultGroupPolicy {
        private static Logger logger = Logger.getLogger(GroupPolicy.class.getName());

        @Override
        public void onCreate(String region, Policy.ServerPolicy server, MicroServiceSpec spec)
                throws RemoteException {
            super.onCreate(region, server, spec);
            List<InetSocketAddress> addressList = new ArrayList<>();

            try {
                ServerPolicy consensusServer = (ServerPolicy) server;

                if (server.isLastPolicy()) {
                    // TODO: Make deployment kernel pin primary replica once node selection
                    // constraint is implemented.
                    addressList = sapphire_getAddressList(spec.getNodeSelectorSpec(), region);
                    // The first in the addressList is for primary policy chain.
                    // TODO: Improve node allocation so that other servers can be used instead of
                    // the first one in the region.
                    pin(consensusServer, addressList.get(0));
                }

                // Create additional replicas, one per region. TODO:  Create N-1 replicas on
                // different servers in the same zone.
                for (int i = 1; i < addressList.size(); i++) {
                    replicate(consensusServer, addressList.get(i), region);
                }

                // Tell all the servers about one another
                ConcurrentHashMap<UUID, ServerPolicy> allServers =
                        new ConcurrentHashMap<UUID, ServerPolicy>();
                // First get the self-assigned ID from each server
                List<Policy.ServerPolicy> servers = getServers();
                for (Policy.ServerPolicy i : servers) {
                    ServerPolicy s = (ServerPolicy) i;
                    allServers.put(s.getRaftServerId(), s);
                }
                // Now tell each server about the location and ID of all the servers, and start the
                // RAFT protocol on each server.
                for (ServerPolicy s : allServers.values()) {
                    s.initializeRaft(allServers);
                }
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
    }
}

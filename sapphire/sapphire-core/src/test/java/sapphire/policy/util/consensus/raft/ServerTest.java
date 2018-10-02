package sapphire.policy.util.consensus.raft;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static sapphire.common.UtilsTest.extractFieldValueOnInstance;
import static sapphire.policy.util.consensus.raft.Server.State.FOLLOWER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import sapphire.common.AppObject;
import sapphire.policy.SapphirePolicy;
import sapphire.policy.replication.ConsensusRSMPolicy;

/** Created by quinton on 3/16/18. */
public class ServerTest {
    final int SERVER_COUNT = 3;
    SapphirePolicy.SapphireGroupPolicy groupPolicy = mock(ConsensusRSMPolicy.GroupPolicy.class);
    SapphirePolicy.SapphireServerPolicy[] serverPolicy =
            new SapphirePolicy.SapphireServerPolicy[SERVER_COUNT];
    Server raftServer[] = new Server[SERVER_COUNT];
    private AppObject appObject;

    @Before
    public void setUp() throws Exception {
        appObject = mock(AppObject.class);

        for (int i = 0; i < SERVER_COUNT; i++) {

            serverPolicy[i] = spy(ConsensusRSMPolicy.ServerPolicy.class);
            serverPolicy[i].$__initialize(appObject);
            ((ConsensusRSMPolicy.ServerPolicy) serverPolicy[i])
                    .onCreate(groupPolicy, new HashMap<>());
            try {
                raftServer[i] =
                        (Server) (extractFieldValueOnInstance(this.serverPolicy[i], "raftServer"));

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        // Tell all the servers about one another
        ConcurrentHashMap<UUID, ConsensusRSMPolicy.ServerPolicy> allServers =
                new ConcurrentHashMap<UUID, ConsensusRSMPolicy.ServerPolicy>();
        int k = 0;
        for (SapphirePolicy.SapphireServerPolicy i : serverPolicy) {
            ConsensusRSMPolicy.ServerPolicy s = (ConsensusRSMPolicy.ServerPolicy) i;
            allServers.put(raftServer[k++].getMyServerID(), s);
        }

        for (int i = 0; i < SERVER_COUNT; i++) {
            ((ConsensusRSMPolicy.ServerPolicy) serverPolicy[i]).initializeRaft(allServers);
        }
    }

    @After
    public void tearDown() throws Exception {}

    @Test
    public void getMyServerID() throws Exception {
        raftServer[0].getMyServerID();
    }

    @Test
    public void getCurrentLeader() throws Exception {
        raftServer[0].getCurrentLeader();
    }

    @Test
    public void majorityQuorumSize() throws Exception {
        raftServer[0].majorityQuorumSize();
    }

    @Test
    public void fullQuorumSize() throws Exception {
        raftServer[0].fullQuorumSize();
    }

    @Test
    public void getServer() throws Exception {
        assert (raftServer[0].getServer(raftServer[1].getMyServerID()) == serverPolicy[1]);
    }

    @Test
    public void addServer() throws Exception {
        // Not necessary, tested in setUp() above.
    }

    @Test
    public void start() throws Exception {
        raftServer[0].start();
    }

    public static void makeLeader(Server server) throws java.lang.Exception {
        server.become(Server.State.CANDIDATE, Server.State.FOLLOWER);
        assert (server.getState() == Server.State.CANDIDATE);
        server.become(Server.State.LEADER, Server.State.CANDIDATE);
        assert (server.getState() == Server.State.LEADER);
    }

    public static void makeFollower(Server server) throws java.lang.Exception {
        server.become(Server.State.FOLLOWER, server.getState());
        assert (server.getState() == Server.State.FOLLOWER);
    }

    public static RemoteRaftServer getCurrentLeader(Server server) throws java.lang.Exception {
        return server.getCurrentLeader();
    }

    @Test
    public void become() throws Exception {
        raftServer[0].start();
        raftServer[0].become(Server.State.FOLLOWER, Server.State.FOLLOWER);
        assert (raftServer[0].getState() == Server.State.FOLLOWER);
        raftServer[0].become(Server.State.CANDIDATE, Server.State.FOLLOWER);
        assert (raftServer[0].getState() == Server.State.CANDIDATE);
        raftServer[0].become(Server.State.LEADER, Server.State.CANDIDATE);
        assert (raftServer[0].getState() == Server.State.LEADER);
    }

    @Test
    public void successfulLeaderElection() throws java.lang.InterruptedException {
        for (Server s : raftServer) {
            s.start();
        }
        raftServer[0].become(
                Server.State.CANDIDATE,
                Server.State
                        .FOLLOWER); // Immediately tell the first one to start an election, just to
        // speed things up in this unit test.

        int leaderCount = 0;
        UUID initialLeader = null, finalLeader = null;
        long startTime = System.currentTimeMillis();
        while (leaderCount == 0
                && System.currentTimeMillis() < startTime + Server.LEADER_HEARTBEAT_TIMEOUT) {
            // Use LEADER_HEARTBEAT_TIMEOUT not LEADER_ELECTION_TIMEOUT as the latter is randomized,
            // and we want consistent test results.
            // LEADER_HEARTBEAT_TIMEOUT is currently defined to always be > LEADER_ELECTION_TIMEOUT
            for (Server s : raftServer) {
                if (s.getState() == Server.State.LEADER) {
                    initialLeader = s.getMyServerID();
                    leaderCount++;
                }
            }
        }
        assertEquals(1, leaderCount); // So we got one leader, at least.
        Thread.sleep(
                100); // Wait a while and make sure that we still have exactly one leader, and that
        // it's the same.
        leaderCount = 0;
        for (Server s : raftServer) {
            if (s.getState() == Server.State.LEADER) {
                finalLeader = s.getMyServerID();
                leaderCount++;
            }
        }
        assertTrue(leaderCount == 1 && finalLeader.equals(initialLeader));
    }

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Test
    public void appendEntries() throws java.lang.Exception {
        for (Server s : raftServer) {
            s.start();
        }
        raftServer[0].become(Server.State.CANDIDATE, FOLLOWER);
        Thread.sleep(100);

        String methodName = "public java.lang.String java.lang.Object.toString()";
        ArrayList<Object> args = new ArrayList<Object>();
        Object obj = new ConsensusRSMPolicy.RPC(methodName, args);

        assertEquals(0, raftServer[0].pState.log().size());
        assertEquals(0, raftServer[1].pState.log().size());
        assertEquals(0, raftServer[2].pState.log().size());

        raftServer[0].applyToStateMachine(obj);
        Thread.sleep(100);

        assertEquals(1, raftServer[0].pState.log().size());
        assertEquals(1, raftServer[1].pState.log().size());
        assertEquals(1, raftServer[2].pState.log().size());
    }

    @Test
    public void alreadyVoted() throws java.lang.Exception {
        for (Server s : raftServer) {
            s.start();
        }
        raftServer[0].become(Server.State.CANDIDATE, FOLLOWER);
        Thread.sleep(100);

        thrown.expect(AlreadyVotedException.class);
        raftServer[2].requestVote(1, raftServer[1].getMyServerID(), -1, -1);
    }

    @Test
    public void candidateBehind() throws java.lang.Exception {
        int term;
        List<LogEntry> entries = new ArrayList<LogEntry>();

        for (Server s : raftServer) {
            String methodName = "public java.lang.String java.lang.Object.toString()";
            ArrayList<Object> args = new ArrayList<Object>();
            Object obj = new ConsensusRSMPolicy.RPC(methodName, args);
            term = 1;
            LogEntry one = new LogEntry(obj, term);
            entries.add(one);

            s.pState.setLog(entries);
            s.pState.setCurrentTerm(term, s.pState.getCurrentTerm());
        }

        thrown.expect(CandidateBehindException.class);
        raftServer[1].requestVote(2, raftServer[0].getMyServerID(), -1, -1);
    }

    @Test
    public void invalidPrevLogIndex() throws java.lang.Exception {
        int term;
        List<LogEntry> entries = new ArrayList<LogEntry>();

        for (Server s : raftServer) {
            String methodName = "public java.lang.String java.lang.Object.toString()";
            ArrayList<Object> args = new ArrayList<Object>();
            Object obj = new ConsensusRSMPolicy.RPC(methodName, args);
            term = 1;
            LogEntry one = new LogEntry(obj, term);
            entries.add(one);

            s.pState.setLog(entries);
            s.pState.setCurrentTerm(term, s.pState.getCurrentTerm());
        }

        thrown.expect(InvalidLogIndex.class);
        raftServer[1].appendEntries(2, raftServer[0].getMyServerID(), 3, 1, entries, -1);
    }

    @Test
    public void prevLogTermMismatch() throws java.lang.Exception {
        int term;
        List<LogEntry> entries = new ArrayList<LogEntry>();

        for (Server s : raftServer) {
            String methodName = "public java.lang.String java.lang.Object.toString()";
            ArrayList<Object> args = new ArrayList<Object>();
            Object obj = new ConsensusRSMPolicy.RPC(methodName, args);
            term = 1;
            LogEntry one = new LogEntry(obj, term);
            entries.add(one);

            s.pState.setLog(entries);
            s.pState.setCurrentTerm(term, s.pState.getCurrentTerm());
        }

        thrown.expect(PrevLogTermMismatch.class);
        raftServer[1].appendEntries(3, raftServer[0].getMyServerID(), 1, 2, entries, -1);
    }

    @Test
    public void invalidTerm() throws java.lang.Exception {
        int term;
        List<LogEntry> entries = new ArrayList<LogEntry>();

        for (Server s : raftServer) {
            String methodName = "public java.lang.String java.lang.Object.toString()";
            ArrayList<Object> args = new ArrayList<Object>();
            Object obj = new ConsensusRSMPolicy.RPC(methodName, args);
            term = 2;
            LogEntry one = new LogEntry(obj, term);
            entries.add(one);

            s.pState.setLog(entries);
            s.pState.setCurrentTerm(term, s.pState.getCurrentTerm());
        }

        thrown.expect(InvalidTermException.class);
        raftServer[2].requestVote(1, raftServer[1].getMyServerID(), -1, -1);
    }

    @Test
    public void requestVote() throws Exception {
        // Already tested by becoming a candidate above.
    }

    @Test
    public void applyCommitted() throws Exception {
        raftServer[0].applyCommitted();
    }

    @Test
    public void respondToRemoteTerm() throws Exception {
        for (Server s : raftServer) {
            s.start();
        }
        raftServer[0].become(Server.State.CANDIDATE, FOLLOWER);

        int leaderCount = 0;
        UUID initialLeader = null;
        long startTime = System.currentTimeMillis();
        while (leaderCount == 0
                && System.currentTimeMillis() < startTime + Server.LEADER_HEARTBEAT_TIMEOUT) {
            for (Server s : raftServer) {
                if (s.getState() == Server.State.LEADER) {
                    initialLeader = s.getMyServerID();
                    leaderCount++;
                }
            }
        }
        assertEquals(raftServer[0].getMyServerID(), initialLeader);

        raftServer[0].respondToRemoteTerm(2);
        assertEquals(FOLLOWER, raftServer[0].getState());
    }

    @Test
    public void doRPC() throws java.lang.Exception {
        successfulLeaderElection();
        String[] methods = {"fooMethod", "barMethod"};
        ArrayList<Object> args = new ArrayList<Object>();
        for (String method : methods) {
            // Apply to stateMachine is invoked only on leader
            raftServer[0].applyToStateMachine(new ConsensusRSMPolicy.RPC(method, args));
        }
    }
}

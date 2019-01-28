package amino.run.policy.util.consensus.raft;

import static amino.run.common.UtilsTest.extractFieldValueOnInstance;
import static amino.run.policy.util.consensus.raft.Server.State.CANDIDATE;
import static amino.run.policy.util.consensus.raft.Server.State.FOLLOWER;
import static amino.run.policy.util.consensus.raft.Server.State.LEADER;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import amino.run.common.AppObject;
import amino.run.policy.Policy;
import amino.run.policy.replication.ConsensusRSMPolicy;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/** Created by quinton on 3/16/18. */
public class ServerTest {
    final int SERVER_COUNT = 3;
    Policy.GroupPolicy groupPolicy = mock(ConsensusRSMPolicy.GroupPolicy.class);
    Policy.ServerPolicy[] serverPolicy = new Policy.ServerPolicy[SERVER_COUNT];
    Server raftServer[] = new Server[SERVER_COUNT];
    private AppObject appObject;

    /**
     * Below methods have been written for the purpose of unit tests only. The reason for creating
     * this is to use the methods provided in "amino.run.policy.util.consensus.raft.Server" which
     * has default access.
     */
    public static List getRaftlog(Server r) throws Exception {
        return r.pState.log();
    }

    public static Object getOperation(LogEntry l) throws Exception {
        return l.operation;
    }

    public static Integer getTerm(LogEntry l) throws Exception {
        return l.term;
    }

    public static int verifyLeaderElected(Server[] s) throws java.lang.Exception {
        int count = 0;
        int maxRetries = 10;
        while (count < maxRetries) {
            for (int i = 0; i < s.length; i++) {
                if (s[i].getState() == LEADER) {
                    verifyLeaderViewOnRafts(s, i);
                    return i;
                }
            }
            count++;
            sleep(500);
        }
        throw new Exception("Raft Leader not elected for long time.");
    }

    public static void verifyLeaderViewOnRafts(Server[] s, int leaderIndex)
            throws java.lang.Exception {
        int count = 0;
        int verifiedCount = 0;
        int maxRetries = 3;
        while (count < maxRetries) {
            for (int j = 0; j < s.length; j++) {
                if (j != leaderIndex) {
                    UUID leaderViewOfRaftServer = s[j].vState.currentLeader;
                    UUID leaderServer = s[leaderIndex].getMyServerID();
                    if (leaderViewOfRaftServer == leaderServer) {
                        verifiedCount++;
                    }
                }
            }
            if (verifiedCount == s.length - 1) {
                break;
            }
            count++;
            verifiedCount = 0;
            sleep(500);
        }
    }

    public static void verifyLastApplied(Server leader, Server follower)
            throws Exception, InterruptedException {
        int count = 0;
        int maxRetries = 5;
        while (count < maxRetries) {
            if (leader.pState.log().size() - 1 == follower.vState.getLastApplied()) {
                return;
            }
            count++;
            sleep(500);
        }
        throw new Exception("Applying log on server failed.");
    }

    @Before
    public void setUp() throws Exception {
        appObject = mock(AppObject.class);

        for (int i = 0; i < SERVER_COUNT; i++) {
            serverPolicy[i] = spy(ConsensusRSMPolicy.ServerPolicy.class);
            serverPolicy[i].$__initialize(appObject);
            ((ConsensusRSMPolicy.ServerPolicy) serverPolicy[i]).onCreate(groupPolicy, null);
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

        for (Policy.ServerPolicy i : serverPolicy) {
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

    public static void makeCandidate(Server server) throws java.lang.Exception {
        server.become(Server.State.FOLLOWER, server.getState());
        server.become(Server.State.CANDIDATE, Server.State.FOLLOWER);
        assert (server.getState() == Server.State.CANDIDATE);
    }

    public static void makeFollower(Server server) throws java.lang.Exception {
        server.become(FOLLOWER, server.getState());
        assert (server.getState() == FOLLOWER);
    }

    public static RemoteRaftServer getCurrentLeader(Server server) throws java.lang.Exception {
        return server.getCurrentLeader();
    }

    @Test
    public void become() throws Exception {
        raftServer[0].start();
        raftServer[0].become(FOLLOWER, FOLLOWER);
        assert (raftServer[0].getState() == FOLLOWER);
        raftServer[0].become(CANDIDATE, FOLLOWER);
        assert (raftServer[0].getState() == CANDIDATE);
        raftServer[0].become(LEADER, CANDIDATE);
        assert (raftServer[0].getState() == LEADER);
    }

    @Test
    public void successfulLeaderElection() throws java.lang.InterruptedException {
        for (Server s : raftServer) {
            s.start();
        }
        raftServer[0].become(
                CANDIDATE,
                FOLLOWER); // Immediately tell the first one to start an election, just to
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
                if (s.getState() == LEADER) {
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
            if (s.getState() == LEADER) {
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
        raftServer[0].become(CANDIDATE, FOLLOWER);
        assertTrue(raftServer[verifyLeaderElected(raftServer)] == raftServer[0]);

        String methodName = "public java.lang.String java.lang.Object.toString()";
        ArrayList<Object> args = new ArrayList<Object>();
        Object obj = new ConsensusRSMPolicy.RPC(methodName, args);

        assertEquals(0, raftServer[0].pState.log().size());
        assertEquals(0, raftServer[1].pState.log().size());
        assertEquals(0, raftServer[2].pState.log().size());

        raftServer[0].applyToStateMachine(obj);
        verifyLastApplied(raftServer[0], raftServer[1]);
        verifyLastApplied(raftServer[0], raftServer[2]);

        raftServer[0].applyToStateMachine(obj);
        verifyLastApplied(raftServer[0], raftServer[1]);
        verifyLastApplied(raftServer[0], raftServer[2]);

        // Verifying the entries on other raftServers, with that on the leader.
        for (int i = 0; i < raftServer[0].pState.log().size(); i++) {
            LogEntry leaderEntry = raftServer[0].pState.log().get(i);
            LogEntry entry1 = raftServer[1].pState.log().get(i);
            LogEntry entry2 = raftServer[2].pState.log().get(i);

            // Matching Operation
            assertEquals(leaderEntry.operation, entry1.operation);
            // Matching Term
            assertEquals(leaderEntry.term, entry1.term);

            // Matching Operation
            assertEquals(leaderEntry.operation, entry2.operation);
            // Matching Term
            assertEquals(leaderEntry.term, entry2.term);
        }

        // Verifying that post applying the logs, all clients are at the same LastApplied.
        assertEquals(raftServer[0].vState.getLastApplied(), raftServer[1].vState.getLastApplied());
        assertEquals(raftServer[0].vState.getLastApplied(), raftServer[2].vState.getLastApplied());
    }

    @Test
    public void alreadyVoted() throws java.lang.Exception {
        for (Server s : raftServer) {
            s.start();
        }
        raftServer[0].become(CANDIDATE, FOLLOWER);
        verifyLeaderElected(raftServer);
        assertTrue(raftServer[verifyLeaderElected(raftServer)] == raftServer[0]);

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
        raftServer[0].become(CANDIDATE, FOLLOWER);

        int leaderCount = 0;
        UUID initialLeader = null;
        long startTime = System.currentTimeMillis();
        while (leaderCount == 0
                && System.currentTimeMillis() < startTime + Server.LEADER_HEARTBEAT_TIMEOUT) {
            for (Server s : raftServer) {
                if (s.getState() == LEADER) {
                    initialLeader = s.getMyServerID();
                    leaderCount++;
                }
            }
        }
        assertEquals(raftServer[0].getMyServerID(), initialLeader);

        raftServer[0].respondToRemoteTerm(2);
        Assert.assertEquals(FOLLOWER, raftServer[0].getState());
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

    /**
     * Verifying that the currentTerm, votedFor, myServerID and log stored using memory map to the
     * persistent storage is in sync with the data on the raftServer.
     */
    @Test
    public void testPersistence() throws java.lang.Exception {
        byte[] arr;
        int i = 0;
        int position = 0;
        int UUIDsize = 36;
        List<LogEntry> myLog = new ArrayList<LogEntry>();
        MappedByteBuffer buffer;
        RandomAccessFile file;
        String persistFile = "/var/tmp/" + raftServer[0].pState.myServerID + ".txt";
        String value;
        UUID uid;

        // Ensure Leader Election
        for (Server s : raftServer) {
            s.start();
        }
        raftServer[0].become(CANDIDATE, FOLLOWER);
        assertTrue(raftServer[verifyLeaderElected(raftServer)] == raftServer[0]);

        String methodName = "public java.lang.String java.lang.Object.toString()";
        ArrayList<Object> args = new ArrayList<Object>();
        Object obj = new ConsensusRSMPolicy.RPC(methodName, args);
        raftServer[0].applyToStateMachine(obj);
        raftServer[0].applyToStateMachine(obj);

        // Gaining access to the file to which the logs were persisted
        file = new RandomAccessFile(new File(persistFile), "r");
        FileChannel fc = file.getChannel();

        // Extracting currentTerm from the persistent storage
        buffer = fc.map(FileChannel.MapMode.READ_ONLY, position, Integer.SIZE);
        arr = new byte[Integer.SIZE];
        buffer.get(arr);
        int currentTerm = Integer.parseInt(new String(arr).trim());
        // Verifying currentTerm is the same
        assertEquals(raftServer[0].pState.getCurrentTerm(), currentTerm);
        position += Integer.SIZE;

        // Extracting votedFor from the persistent storage
        buffer = fc.map(FileChannel.MapMode.READ_ONLY, position, UUIDsize);
        value = StandardCharsets.UTF_8.decode(buffer).toString();
        uid = UUID.fromString(value);
        // Verifying votedFor is same
        assertEquals(raftServer[0].pState.getVotedFor(), uid);
        position += UUIDsize;

        // Extracting myServerID from the persistent storage
        buffer = fc.map(FileChannel.MapMode.READ_ONLY, position, UUIDsize);
        value = StandardCharsets.UTF_8.decode(buffer).toString();
        uid = UUID.fromString(value);
        // Verifying myServerID is same
        assertEquals(raftServer[0].pState.myServerID, uid);
        position += UUIDsize;

        while (position < file.length()) {
            // Extracting the logEntry index from the persistent storage
            buffer = fc.map(FileChannel.MapMode.READ_ONLY, position, Integer.SIZE);
            arr = new byte[Integer.SIZE];
            buffer.get(arr);
            position += Integer.SIZE;

            // Extracting the logEntry length from the persistent storage
            buffer = fc.map(FileChannel.MapMode.READ_ONLY, position, Integer.SIZE);
            arr = new byte[Integer.SIZE];
            buffer.get(arr);
            int logEntry_length = Integer.parseInt(new String(arr).trim());
            position += Integer.SIZE;

            // Extracting the logEntry object from the persistent storage
            buffer = fc.map(FileChannel.MapMode.READ_ONLY, position, logEntry_length);
            arr = new byte[logEntry_length];
            buffer.get(arr);
            position += logEntry_length;

            ByteArrayInputStream bais = new ByteArrayInputStream(arr);
            ObjectInputStream objectIn = new ObjectInputStream(bais);
            try {
                Object objc = objectIn.readObject();
                myLog.add((LogEntry) objc);
                // Verifying the LogEntry term at each index
                assertEquals(raftServer[0].pState.log().get(i).term, ((LogEntry) objc).term);
                // Verifying the LogEntry operation at each index
                assertTrue(
                        (raftServer[0].pState.log().get(i).operation)
                                .equals(((LogEntry) objc).operation));
                i++;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            objectIn.close();
        }
    }
}

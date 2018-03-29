package sapphire.policy.util.consensus.raft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import sapphire.policy.replication.ConsensusRSMPolicy;
import sapphire.policy.util.ResettableTimer;

/**
 * Created by quinton on 1/31/18.
 * Implementation of the RAFT consensus algorithm, as per https://www.usenix.org/system/files/conference/atc14/atc14-paper-ongaro.pdf
 * Not fully tested or production-ready.
 * I considered using an existing Java Server library, but no obviously good candidates could be found,
 * so decided to just try to implement Server as per the paper.  May be replaced in future by alternative
 * Server implementation.
 */
public class Server { // This outer class contains everything common to leaders, followers and candidates.
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constants
    ////////////////////////////////////////////////////////////////////////////////////////////////
    final UUID NO_LEADER = new UUID(0L, 0L);
    final int INVALID_INDEX = -1;
    /**
     * How long we wait for a heartbeat from the leader before starting a new leader election.
     */
    public static final int LEADER_HEARTBEAT_TIMEOUT = 3000; // milliseconds

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Persistent state on all servers.  //TODO Make persistent, restore on start.
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private Integer currentTerm = -1;
    private UUID votedFor = NO_LEADER;
    private volatile ArrayList<LogEntry> log; // TODO: Garbage collection - log growth currently unbounded.
    private UUID myServerID;

    /**
     * All the other servers.
     */
    ConcurrentMap<UUID, Server> otherServers = new ConcurrentHashMap<UUID, Server>();

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Volatile state on all servers
    ////////////////////////////////////////////////////////////////////////////////////////////////
    int commitIndex = INVALID_INDEX;
    int lastApplied = INVALID_INDEX;

    /**
     * Who is the current leader?
     */
    UUID currentLeader = NO_LEADER;  // Initially no leader.

    /**
     * If we don't receive a heartbeat from the leader, start an election.
     */
    ResettableTimer leaderHeartbeatReceiveTimer;


    enum State { NONE, LEADER, FOLLOWER, CANDIDATE };
    private volatile State state = State.NONE;

    static Logger logger = Logger.getLogger("sapphire.policy.consensus.ConsensusRSMPolicy");

    // Avoid creating a new empty log segment for each heartbeat, to reduce garbage collection.
    final private ArrayList<LogEntry> NO_LOG_ENTRIES = new ArrayList<LogEntry>();

    Leader leader; // Delegate for leader operations.
    Follower follower; // Delegate for follower operations.
    Candidate candidate; // Delegate for candidate operations.

    StateMachineApplier applier; // Delegate to apply state changes.

    /**
     * Constructor
     *
     * Note that the constructor does not start the RAFT algorithm.
     * To do that, call addServer() for each server, and then call start()
     */
    public Server(StateMachineApplier applier) {
        /**
         * Delegate applier, leader, follower and candidate behaviour.
         */
        this.applier = applier;
        /**
         * Persistent state.
         */
        // TODO: Restore persistent state
        this.myServerID = UUID.randomUUID(); // TODO: Retrieve from persistent state.
        this.currentLeader = this.NO_LEADER;
        this.currentTerm = 0;
        this.votedFor = this.NO_LEADER;
        this.log = new ArrayList<LogEntry>();

        this.leader = new Leader();
        this.follower = new Follower();
        this.candidate = new Candidate();
    }

    public void start() {
        // TODO: Perform a pre-flight check.
        /**
         * Start off being a follower.
         */
        become(State.FOLLOWER);
    }

    void become(State newState) {
        switch(this.state) { // Exit the current state.
            case NONE: // do nothing
                break;
            case LEADER:
                leader.stop();
                break;
            case FOLLOWER:
                follower.stop();
                break;
            case CANDIDATE:
                candidate.stop();
        }
        switch(newState) { // Enter the new state.
            case NONE: // do nothing
                break;
            case LEADER:
                leader.start();
                break;
            case FOLLOWER:
                follower.start();
                break;
            case CANDIDATE:
                candidate.start();
        }
    }

    /**
     * appendEntries is invoked by leader to replicate log entries; also used as a heartbeat.
     * @param term leaders' term
     * @param leader so follower can redirect clients
     * @param prevLogIndex index of log entry immediately preceding new ones
     * @param prevLogTerm term of prevLogIndex entry
     * @param entries log entries to store (empty for heartbeat; may send more than one for efficiency)
     * @param leaderCommit leader’s commitIndex
     * @return currentTerm, for leader to update itself
     */
    int appendEntries(int term, UUID leader, int prevLogIndex, int prevLogTerm, List<LogEntry> entries, int leaderCommit) throws InvalidTermException, PrevLogTermMismatch, InvalidLogIndex {
        setCurrentLeader(leader);
        logger.info(String.format(
                "%s: received AppendEntries request from leader %s, term %d, prevLogIndex=%d, leaderCommit=%d, entries=%d",
                myServerID, leader, term, prevLogTerm, leaderCommit, entries.size()));
        if (this.state == Server.State.CANDIDATE) {
            become(State.FOLLOWER);
        }
        leaderHeartbeatReceiveTimer.reset(); // This is a heartbeat from the leader.
        /**
         * All servers convert to followers if their current term is behind (§5.1).
         */
        respondToRemoteTerm(term);
        /**
         *  1. Reply false if term < currentTerm (§5.1)
         **/
        if (term < this.currentTerm) {
            throw new InvalidTermException("Server: Attempt to append entries from prior leader term " + term + ", current term " + term, currentTerm);
        }

        /**
         *  2. Reply false if log doesn’t contain an entry at prevLogIndex
         *     whose term matches prevLogTerm (§5.3)
         **/
        if (entries.size() > 0) { // Not for empty heartbeats
            LogEntry prevLogEntry;
            if (prevLogIndex >= 0) {
                try {
                    prevLogEntry = this.log.get(prevLogIndex);
                } catch (IndexOutOfBoundsException e) {
                    throw new InvalidLogIndex("Attempt to append entry with invalid previous log index: " + prevLogIndex, prevLogIndex);
                }
                if (prevLogEntry.term != term) {
                    throw new PrevLogTermMismatch("Attempt to append entry with invalid previous log term.  Requested term " + prevLogTerm + ", actual term: " + prevLogEntry.term, prevLogIndex, prevLogEntry.term, prevLogTerm);
                }
            }
            /**
             *  3. If an existing entry conflicts with a new one (same index
             *     but different terms), delete the existing entry and all that
             *     follow it (§5.3)
             **/
            int logIndex = prevLogIndex;
            for (Iterator<LogEntry> i = entries.iterator(); i.hasNext(); ) {
                LogEntry newEntry = (LogEntry) i.next();
                if (this.log.size() - 1 >= ++logIndex) { // We already have a log entry with that index
                    if (this.log.get(logIndex).term != term) { // conflicts
                        logger.info(String.format("%s: Removing conflicting log entries, replcing log with server's log from index %d to %d", myServerID, 0, logIndex));
                        this.log = (ArrayList) log.subList(0, logIndex); // delete the existing entry and all that follow.
                    }
                } else {
                    log.add(newEntry); // Append any new entries not already in the log
                }
            }
            /**
             *  4. If leaderCommit > commitIndex, set commitIndex =
             *     min(leaderCommit, index of last new entry)
             **/
            if (leaderCommit > this.commitIndex) {
                this.commitIndex = Math.min(leaderCommit, log.size() - 1);
            }
            applyCommitted();
        }
        return this.currentTerm;
    }

    /**
     * Invoked by candidates to gather votes.
     * @param term candidate’s term
     * @param candidate candidate requesting vote
     * @param lastLogIndex index of candidate’s last log entry
     * @param lastLogTerm term of candidate’s last log entry
     * @return currentTerm, for candidate to update itself
     * @throws InvalidTermException
     * @throws AlreadyVotedException
     * @throws CandidateBehindException
     */
    int requestVote(int term, UUID candidate, int lastLogIndex, int lastLogTerm) throws Server.InvalidTermException, Server.AlreadyVotedException, Server.CandidateBehindException {
        logger.info(String.format("%s received vote request from %s (term=%d, lastLogIndex=%d, lastLogTerm=%d)", myServerID, candidate, term, lastLogIndex, lastLogTerm));
        if (!candidate.equals(getMyServerID())) { // We sometimes vote for ourselves, to that's not considered a request from a remote server.
            /**
             * All servers convert to followers if their current term is behind (§5.1).
             */
            this.respondToRemoteTerm(term);
        }
        /**
         *  1. Reply false if term < currentTerm (§5.1)
         **/
        if (term < this.currentTerm) {
            throw new Server.InvalidTermException("Server: Received voting request for old leader term " + term + ", current term " + this.currentTerm + ". No vote granted.", this.currentTerm);
        }

        /**
         *  2. If votedFor is null or candidateId, and candidate’s log is at least as up-to-date as
         *     receiver’s log, grant vote (§5.2, §5.4)
         */
        if (this.votedFor == null || this.votedFor.equals(NO_LEADER) || this.votedFor == candidate) {
            logger.info(String.format("%s: Before local log size calculation: log=%s", myServerID, this.log));
            int localLogSize = this.log.size();
            logger.info(String.format("%s deciding whether to vote for %s: local log size = %d", myServerID, candidate, localLogSize));
            if (lastLogIndex >= this.lastLogIndex() && (localLogSize==0 || lastLogTerm >= log.get(this.lastLogIndex()).term)) {
                logger.info(String.format("%s decided to vote for %s", myServerID, candidate));
                votedFor = candidate;
                return currentTerm; // Vote for her!
            }
            else {
                throw new Server.CandidateBehindException("Candidate is behind.  Candidate last log index, term  = (" + lastLogIndex + ", " + lastLogTerm + "), current last log index, term = (" + this.lastLogIndex() + ", " + this.lastLogTerm(), currentTerm);
            }
        }
        else {
           throw new Server.AlreadyVotedException("Request to vote for " + candidate + " but already voted for " + this.votedFor + "(current term = " + this.currentTerm, this.currentTerm);
        }
    }

    /**
     *  Apply log entries that have been committed but not yet applied.
     * • If commitIndex > lastApplied: increment lastApplied, apply
     *   log[lastApplied] to state machine (§5.3)
     */
    void applyCommitted() {
        while (this.commitIndex > this.lastApplied) {
            LogEntry entry = this.log.get(++this.lastApplied);
            logger.info(myServerID + ": Applying " + entry);
            try {
                applier.apply(entry.operation);
            }
            catch (java.lang.Exception e) {
                logger.warning(String.format("Exception thrown while applying %s: %s", entry, e));
            }
            // Note that is doesn't matter what the result of the call is.
            // As long as we execute them all in the same order on all servers.
            // We could try to return a failure if the method call throws an exception,
            // but chances are that it will throw the same exception on the master (as all replicas
            // run method calls in the same sequence).  So in the absence of external side effects that
            // succeed on some replicas and fail on others, this is safe.
            // If we want to deal with external side effects we need to layer a transaction protocol
            // on top of Server.  We can do that later.
        }
    }

    /**
     * • If RPC request or response contains term T > currentTerm:
     *   set currentTerm = T, convert to follower (§5.1)
     */
    void respondToRemoteTerm(int remoteTerm) {
        synchronized(this.currentTerm) {
            if (this.currentTerm < remoteTerm) {
                this.currentTerm = remoteTerm;
                if (this.state != State.FOLLOWER) {
                    this.become(State.FOLLOWER);
                }
            }
        }
    }

    public Object applyToStateMachine(Object operation) throws java.lang.Exception {
        logger.info(String.format("%s: applyToStateMachine(%s)", myServerID, operation));
        if (this.state == Server.State.LEADER) {
            return leader.applyToStateMachine(operation);
        }
        else {
            return getCurrentLeader().applyToStateMachine(operation); // Forward to the leader.
        }
    }


    private int lastLogIndex() {
        return this.log.size()-1;
    }

    private int prevLogIndex() {
        return Math.max(lastLogIndex()-1, INVALID_INDEX);
    }

    private int lastLogTerm() {
        if (this.log.isEmpty()) {
            return -1;
        }
        else {
            return this.log.get(this.log.size()-1).term;
        }
    }

    private int prevLogTerm() {
        int prev = prevLogIndex();
        if( prev < 0) {
            return -1;
        }
        else {
            return this.log.get(prev).term;
        }
    }

    State getState() { // For unit testing only.
        return state;
    }

    class Leader {
        // Volatile state on leaders (reinitialized after election)
        /**
         * Next index for each server.
         */
        Map<UUID, Integer> nextIndex = new HashMap<UUID, Integer>();
        /**
         * Match index for each server.
         */
        Map<UUID, Integer> matchIndex = new HashMap<UUID, Integer>();

        /**
         * How frequently we send out heartbeats when we're the leader.
         */
        final int LEADER_HEARTBEAT_PERIOD = LEADER_HEARTBEAT_TIMEOUT / 3;

        /**
         * Periodically send heartbeats when we're the leader.
         */
        ResettableTimer leaderHeartbeatSendTimer;

        /**
         * Thread pool used for sending appendEntries (incl heartbeats) to followers.
         */
        ThreadPoolExecutor appendEntriesThreadPool;

        Leader() {
        }
        /**
         * Start being a leader.
         */
        void start() {
            /**
             * • Upon election: send initial empty AppendEntries RPCs
             *   (heartbeat) to each server; repeat during idle periods to
             *    prevent election timeouts (§5.2)
             **/
            logger.info(myServerID + ": Start being a leader.");
            state = Server.State.LEADER;
            if(appendEntriesThreadPool == null ) {
                appendEntriesThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(otherServers.size() * 2);
            }
            leaderHeartbeatSendTimer = new ResettableTimer(new TimerTask() {
                public void run() {
                    sendHeartbeats();
                }
            }, (long) LEADER_HEARTBEAT_PERIOD);
            sendHeartbeats();

            /**
             * Reinitialize volatile leader state
             */
            this.nextIndex.clear();
            this.matchIndex.clear();
            for (UUID i : otherServers.keySet()) {
                this.nextIndex.put(i, lastLogIndex() + 1);
                this.matchIndex.put(i, 0);
            }
            /**
             * • If last log index ≥ nextIndex for a follower: send
             *   AppendEntries RPC with log entries starting at nextIndex
             * • If successful: update nextIndex and matchIndex for
             *   follower (§5.3)
             * • If AppendEntries fails because of log inconsistency:
             *   decrement nextIndex and retry (§5.3)
             * • If there exists an N such that N > commitIndex, a majority
             *   of matchIndex[i] ≥ N, and log[N].term == currentTerm:
             *   set commitIndex = N (§5.3, §5.4).
             **/
        }

        /**
         * Stop being a leader.
         */
        void stop() {
            logger.info(myServerID + ": Stop being a leader.");
            leaderHeartbeatSendTimer.cancel(); // Stop sending heartbeats.
            // appendEntriesThreadPool.shutdownNow();
            // appendEntriesThreadPool = null;
        }

        /**
         * Send a appendLog request to the specified server.
         *
         * @param otherServerID
         */
        void sendAppendEntries(UUID otherServerID) {
            boolean success = false;
            while (!success && state == State.LEADER) {
                final Integer otherServerNextIndex = leader.nextIndex.get(otherServerID);
                try {
                    logger.info(String.format("%s sending appendEntries to %s: otherServerNextIndex=%d, log.size=%d", myServerID, otherServerID, otherServerNextIndex, log.size()));
                    int nextIndex = otherServerNextIndex == null ? 0 : otherServerNextIndex;
                    int prevLogTerm;
                    if (nextIndex > 0 && log.size() > 0) {
                        prevLogTerm = log.get(otherServerNextIndex - 1).term;
                    }
                    else {
                        prevLogTerm = INVALID_INDEX;
                    }
                    List<LogEntry> entries = log.size() > 0 ? (List<LogEntry>)log.subList(nextIndex, lastLogIndex() + 1) : NO_LOG_ENTRIES;
                    int remoteTerm = getServer(otherServerID).appendEntries(currentTerm, myServerID,
                            nextIndex - 1, prevLogTerm, entries, commitIndex);
                    success = true;
                    respondToRemoteTerm(remoteTerm); // Might lose leadership.
                } catch (Server.InvalidTermException e) {
                    logger.warning(e.toString());
                    respondToRemoteTerm(e.currentTerm);
                } catch (Server.PrevLogTermMismatch e) {
                    logger.warning(e.toString());
                    leader.nextIndex.put(otherServerID, otherServerNextIndex - 1); // Decrement and try again.
                } catch (Server.InvalidLogIndex e) { // The remote server doesn't have that log entry at all.
                    logger.severe(e.toString());
                    // TODO: throw e; // Why does this not compile???
                }
            }
            if (state == State.LEADER) {
                leader.nextIndex.put(otherServerID, lastLogIndex() + 1);
                leader.matchIndex.put(otherServerID, lastLogIndex());
            }
        }


            /*
            boolean success = false;
            while(!success) {
                if (state == State.LEADER) {
                    logger.info(getMyServerID() + ": Sending appendEntries to " + server.getMyServerID());
                    try {
                        server.appendEntries(currentTerm, myServerID, prevLogIndex(),
                                prevLogTerm(), NO_LOG_ENTRIES, commitIndex); // TODO: Calc prevLog... for destination server, and send correct log entries.
                        success = true;
                    } catch (InvalidTermException e) {
                        /*
                         * If RPC request or response contains term T > currentTerm:
                         *     set currentTerm = T, convert to follower (§5.1)
                         *
                        currentTerm = e.currentTerm;
                        become(State.FOLLOWER);
                    } catch (PrevLogTermMismatch e) {
                        // TODO: Handle this
                    } catch (InvalidLogIndex e) {
                        // TODO: Handle this
                    }
                } else {
                    logger.severe("Fatal exception - code bug!!! - sendHeartbeat() called when we are not the leader.  Current state "
                            + state + " != State.LEADER (" + State.LEADER + ")");
                }
            }
            */
        // }

        /**
         * Send heartbeats to all servers.
         */
        void sendHeartbeats() { // We might want to keep a separate timer for each other server.  For now, we use one timer.
            if (state == State.LEADER) {
                final Iterator<UUID> i = otherServers.keySet().iterator();
                while (i.hasNext()) {
                    final UUID server = i.next();
                    appendEntriesThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            sendAppendEntries(server);
                        }
                    });
                }
                ;
                leaderHeartbeatSendTimer.reset();
            }
        }

        /**
         * Are a majority of matchIndex[i]>=logIndex, and log[logIndex].term = currentTerm
         *
         * @param logIndex
         * @return true if the above holds for logIndex, else false
         */
        boolean shouldBeCommitted(int logIndex) {
            if (log.get(logIndex).term != currentTerm) {
                return false;
            }
            int matches = 0;
            for (UUID otherServerID : otherServers.keySet()) {
                int match = leader.matchIndex.get(otherServerID);
                if (match >= logIndex) {
                    if (matches++ >= majorityQuorumSize() - 1) { // -1 because the leader implicitly matches
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * If there exists an N such that N>commitIndex, a majority of
         * matchIndex[i]>=N, and log[N].term = currentTerm, then set
         * commitIndex = N (see 5.3, 5.3)
         */
        void updateCommitIndex() {
            for (int i = lastLogIndex(); i > commitIndex; i--) {
                if (shouldBeCommitted(i)) {
                    commitIndex = i;
                    break;
                }
            }
        }

        /**
         * Applies an operation to the state machine, after successfully appending it to the logs of a majority quorum of replicas.
         *
         * @param operation to apply, i.e. a method invocation
         * @return result of the method invocation
         * @throws Exception either an exception thrown by the method invocation when applied locally,
         *                   or a RAFTException indicating why the operation could not be applied.
         */
        public Object applyToStateMachine(Object operation) throws java.lang.Exception {
            /**
             *  If command received from client: append entry to local log, respond after entry applied to state machine (§5.3)
             */
            logger.info(String.format("%s: applyToStateMachine(%s)", myServerID, operation));
            final int logIndex;
            synchronized (log) {
                log.add(new LogEntry(operation, currentTerm));
                logIndex = lastLogIndex();
            }
            final Semaphore replicationCounter = new Semaphore(0); // Initially we have zero replicas
            final Iterator<UUID> i = otherServers.keySet().iterator();
            while (i.hasNext()) {
                final UUID otherServerID = i.next();
                final Integer otherServerNextIndex = leader.nextIndex.get(otherServerID);
                appendEntriesThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (otherServerNextIndex == null || logIndex >= otherServerNextIndex) { // This is always true, but we put it here to be consistent with the formal algorithm.
                            sendAppendEntries(otherServerID);
                        } else {
                            logger.warning(String.format("%s: Whooah! logIndex %d is not greater than otherServerNextIndex %s", myServerID, logIndex, otherServerNextIndex));
                        }
                        replicationCounter.release();
                    }
                });
            }
            logger.info(String.format("%s: Waiting for logindex %d to be committed to %d majority quorum.", myServerID, logIndex, majorityQuorumSize()));
            boolean quorumAchieved = false;
            try {
                replicationCounter.acquire(majorityQuorumSize());
                quorumAchieved = true;
            } catch (InterruptedException e) {
                logger.warning(String.format("Failed to commit logindex %d to quorum of %d.  Reason: %s", logIndex, majorityQuorumSize(), e));
            }
            logger.info(String.format("%s: Logindex %d was committed to %d majority quorum.", myServerID, logIndex, majorityQuorumSize()));
            updateCommitIndex();
            applyCommitted();
            if (lastApplied >= logIndex) {
                logger.info("logIndex " + logIndex + " applied to state machine: " + operation);
            } else {
                logger.severe(String.format("Whooah! Internal error: logIndex %d was committed by a majority quorum, but not applied locally."));
            }
            return null; // TODO: Need to get the return value back from applyCommited(), and return it here.  Hmmm.
        }

        /**
         * Apply log entries that have been committed but not yet applied.
         * • If commitIndex > lastApplied: increment lastApplied, apply
         * log[lastApplied] to state machine (§5.3).
         * On the master, return the result of the last applied operation, for return
         * to the client.
         */
        Object applyCommitted() throws java.lang.Exception {
            Object lastReturnVal = null;
            while (commitIndex > lastApplied) {
                LogEntry entry = log.get(++lastApplied);
                logger.info(myServerID + ": Applying " + entry);
                lastReturnVal = applier.apply(entry.operation);
            }
            return lastReturnVal;
        }
    }

    public UUID getMyServerID() {
        return this.myServerID;
    }
    /**
     * Which Server is the current leader?
     * @return null if there is no leader, else the current leader.
     */
    Server getCurrentLeader() {
        synchronized(currentLeader) {
            return currentLeader.equals(NO_LEADER) ? null : getServer(currentLeader);
        }
    }

    void removeCurrentLeader() {
        synchronized(currentLeader) {
            currentLeader = NO_LEADER;
        }
    }

    synchronized void setCurrentLeader(UUID leader) {
        synchronized(currentLeader) {
            if (!this.currentLeader.equals(leader)) {
                this.currentLeader = leader; // We learned about a new leader.
            }
        }
    }

    int majorityQuorumSize() {
        // Quorum size is 2f+1.  f+1 votes are required for a majority.
        int f  = fullQuorumSize()/2+1; // Note integer arithmetic rounds down.
        return f;
    }

    int fullQuorumSize() {
        return otherServers.size()+1; // NOTE: This only makes sense if otherServers contains all the other servers at this time.
    }

    Server getServer(UUID serverId) {
        return otherServers.get(serverId); // TODO: Handle not found.
    }

    public void addServer(UUID id, Server server) {
        this.otherServers.put(id, server);
    }


    class Follower {
        Follower() {
            leaderHeartbeatReceiveTimer = new ResettableTimer(new TimerTask() { public void run() { become(State.CANDIDATE); }}, (long)LEADER_HEARTBEAT_TIMEOUT);
        }
        /**
         * Start being a follower.
         */
        void start() {
            /**
             * Followers (§5.2)
             * - Respond to RPCs from candidates and leaders (we already handle appendEntries() and requestVote() )
             * - If election timeout elapses without receiving AppendEntries RPC from current leader
             *   or granting vote to candidate:
             *      - convert to candidate
             */
            logger.info(myServerID + ": Start being a follower.");
            state = Server.State.FOLLOWER;
            leaderHeartbeatReceiveTimer.start(); // Expect to receive heartbeats from the leader.
        }

        /**
         * Stop being a follower.
         */
        void stop() {
            logger.info(myServerID + ": Stop being a follower.");
            leaderHeartbeatReceiveTimer.cancel(); // Don't expect to receive heartbeats from leader.
        }
    }

    class Candidate {
        /**
         * How long we wait after starting election, before giving up and starting again if no leader has been elected yet.
         * Note that a random variation between servers is introduced to reduce split votes.
         */
        public final int LEADER_ELECTION_TIMEOUT = (int)(LEADER_HEARTBEAT_TIMEOUT * Math.random());

        /**
         * If no leader is elected within the timeout, start another election.
         */
        ResettableTimer leaderElectionTimer = new ResettableTimer(new TimerTask() { public void run() { become(State.CANDIDATE); }}, (long)LEADER_ELECTION_TIMEOUT);

        /**
         * Thread pool used for sending out vote requests in parallel.
         */
        ThreadPoolExecutor voteRequestThreadPool;

        /**
         * Start being a candidate.
         */
        void start() {
            /**
             * Candidates (§5.2):
             * - On conversion to candidate, start election:
             *   - Increment currentTerm
             *   - Vote for self
             *   - Reset election timer
             *   - Send RequestVote RPCs to all other servers
             * - If votes received from majority of servers: become leader
             * - If AppendEntries RPC received from new leader: convert to
             *   follower
             * - If election timeout elapses: start new election
             */
            logger.info(myServerID + ": Start being a candidate.");
            state = Server.State.CANDIDATE;
            currentTerm++;
            try { // Vote for self
                requestVote(currentTerm, myServerID, lastLogIndex(), lastLogTerm());
            }
            catch (Server.AlreadyVotedException e) {
                logger.info("Failed to vote for self.  Already voted: " + e.toString());
                become(State.FOLLOWER);
                return;
            }
            catch (Server.VotingException e) {
                logger.warning("Unexpected error voting for self: " + e.toString());
                become(State.FOLLOWER);
                return;
            }
            voteRequestThreadPool = (ThreadPoolExecutor)Executors.newFixedThreadPool(otherServers.size() + 1);
            this.leaderElectionTimer.start();
            sendVoteRequests();
        }

        /**
         * Stop being a candidate.
         */
        void stop() {
            logger.info(myServerID + ": Stop being a candidate.");
            this.leaderElectionTimer.cancel();
            if (voteRequestThreadPool != null) {
                voteRequestThreadPool.shutdownNow();
                voteRequestThreadPool = null;
            }
        }
        /**
         *  Invoke requestVote() on server, and process result, including incrementing the Semaphore on success.
         */
        void sendVoteRequest(UUID serverID, Semaphore voteCounter) {
            Server server = getServer(serverID);
            boolean voteGranted = true;
            try {
                logger.info("Sending vote request to server " + server.getMyServerID());
                server.requestVote(currentTerm, myServerID, lastLogIndex(), lastLogTerm());
            }
            catch (Server.VotingException e) {
                voteGranted = false;
                logger.info("Leader election vote request denied by server: " + e.toString());
                respondToRemoteTerm(e.currentTerm);
            }
            if (voteGranted) {
                logger.info("Vote received from server " + server.getMyServerID());
                voteCounter.release(1); // Yay!  We got one vote.
            }
        }

        /**
         * Send vote requests to all other servers.
         * Returns once all vote requests have been queued for sending, and a thread has been created
         * to gather all responses and convert to leader or start another election, as appropriate.
         */
        void sendVoteRequests() {
            logger.info("Sending vote requests to " + otherServers.keySet().size() + " other servers");
            final Semaphore voteCounter = new Semaphore(0); // Initially we have zero votes.
            // Send vote requests in parallel
            final Iterator<UUID> i = otherServers.keySet().iterator();
            while(i.hasNext()) {
                final UUID server = i.next();
                voteRequestThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        sendVoteRequest(server, voteCounter);
                        logger.info("Sent vote request to server " + server);
                    }
                });
            }
            // Wait to receive a majority of votes.
            voteRequestThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    boolean votedInAsLeader = false;
                    try {
                        votedInAsLeader = voteCounter.tryAcquire(majorityQuorumSize(), LEADER_ELECTION_TIMEOUT, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        logger.info(myServerID + "Interrupted while waiting to receive a majority of votes in leader election.");
                    }
                    if (votedInAsLeader && state == State.CANDIDATE) {
                        become(State.LEADER);
                    } else if (state == State.CANDIDATE) {
                        become(State.CANDIDATE);
                    } else {
                        become(State.FOLLOWER);
                    }
                }
            });
        }
    }

    /**
     * Base class for all voting exceptions.
     * When candidates request a vote but are denied, they need to know the current term of the voter to update themselves.
     */
    public static class VotingException extends Exception {
        public int currentTerm;
        public VotingException(String s, int currentTerm) {
            super(s);
            this.currentTerm = currentTerm;
        }
    }
    /**
     * If term < currentTerm on appendEntries or requestVote RPC
     */
    public static class InvalidTermException extends VotingException {
        public InvalidTermException(String s, int currentTerm) {
            super(s, currentTerm);
        }
    }

    /**
     * If log doesn’t contain an entry at prevLogIndex whose term matches prevLogTerm on appendEntries RPC
     */
    public static class PrevLogTermMismatch extends Exception {
        int logIndex, remoteTerm, localTerm;
        public PrevLogTermMismatch(String s, int logIndex, int remoteTerm, int localTerm) {
            super(s);
            this.logIndex = logIndex;
            this.remoteTerm = remoteTerm;
            this.localTerm = localTerm;
        }
    }

    /**
     * If log doesn’t contain an entry at specified logIndex on appendEntries RPC
     */
    public static class InvalidLogIndex extends sapphire.policy.util.consensus.raft.Exception {
        int invalidIndex;
        public InvalidLogIndex(String s, int invalidIndex) {
            super(s);
            this.invalidIndex = invalidIndex;
        }
    }

    /**
     * If member has already voted for a different leader when receiving requestVote RPC
     */
    public static class AlreadyVotedException extends Server.VotingException {
        public AlreadyVotedException(String s, int currentTerm) {
            super(s, currentTerm);
        }
    }

    /**
     * If candidate’s log is not at least as up-to-date as receiver’s log on requestVote RPC
     */
    public static class CandidateBehindException extends Server.VotingException {
        public CandidateBehindException(String s, int currentTerm) {
            super(s, currentTerm);
        }
    }
}

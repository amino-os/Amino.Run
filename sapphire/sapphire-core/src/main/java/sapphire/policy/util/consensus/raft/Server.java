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

import static sapphire.policy.util.consensus.raft.PersistentState.INVALID_INDEX;
import static sapphire.policy.util.consensus.raft.PersistentState.NO_LEADER;

/**
 * Created by quinton on 1/31/18.
 * Implementation of the RAFT consensus algorithm, as per https://www.usenix.org/system/files/conference/atc14/atc14-paper-ongaro.pdf
 * Not fully tested or production-ready yet.
 * Please read the above paper, or at least p308 before trying to understand or working on this code.
 * I considered using an existing Java Server library, but no obviously good candidates could be found,
 * so decided to just try to implement Server as per the paper.  May be replaced in future by alternative
 * RAFT implementation.
 */
public class Server implements RemoteRaftServer { // This outer class contains everything common to leaders, followers and candidates.

    /**
     * How long we wait for a heartbeat from the leader before starting a new leader election.
     */
    public static final int LEADER_HEARTBEAT_TIMEOUT = 3000; // milliseconds


    /**
     * If we don't receive a heartbeat from the leader, start an election.
     */
    ResettableTimer leaderHeartbeatReceiveTimer;

    enum State { NONE, LEADER, FOLLOWER, CANDIDATE };

    static Logger logger = Logger.getLogger(Server.class.getCanonicalName());

    // Avoid creating a new empty log segment for each heartbeat, to reduce garbage collection.
    final private ArrayList<LogEntry> NO_LOG_ENTRIES = new ArrayList<LogEntry>();

    PersistentState pState;
    VolatileState vState;
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
        this.pState = new PersistentState();
        this.vState = new VolatileState();
        this.leader = new Leader();
        this.follower = new Follower();
        this.candidate = new Candidate();
    }

    public UUID getMyServerID() {
        return pState.myServerID;
    }

    public void start() {
        // TODO: Perform a pre-flight check.
        /**
         * Start off being a follower.
         */
        this.become(State.FOLLOWER, vState.getState());
    }

    /**
     * Transition to a new state if current state is preconditionState, i.e. optimistic concurrency.
     * @param newState
     * @param preconditionState
     * @return state after possible transition.
     */
    synchronized State become(State newState, State preconditionState) {
        if (vState.getState() == preconditionState) {
            switch (vState.getState()) { // Exit the current state.
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
            switch (newState) { // Enter the new state.
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
        return vState.getState();
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
    public int appendEntries(int term, UUID leader, int prevLogIndex, int prevLogTerm, List<LogEntry> entries, int leaderCommit) throws InvalidTermException, PrevLogTermMismatch, InvalidLogIndex {
        logger.info(String.format(
                "%s: received AppendEntries request from leader %s, term %d, prevLogIndex=%d, prevLogTerm=%d, leaderCommit=%d, entries=%d",
                pState.myServerID, leader, term, prevLogIndex, prevLogTerm, leaderCommit, entries.size()));

        /**
         * All servers convert to followers if their current term is behind (§5.1).
         */
        respondToRemoteTerm(term);

        /**
         *  1. Reply false if term < currentTerm (§5.1)
         **/
        if (term < pState.getCurrentTerm()) {
            throw new InvalidTermException("Server: Attempt to append entries from prior leader term " + term + ", current term " + pState.getCurrentTerm(), pState.getCurrentTerm());
        }

        vState.setCurrentLeader(leader);

        /* After checking the leader's term and deciding whether to become the follower(if current
        term is less than leader's term) or to reject rpc(throw exception and continue in same
        state(i.e., candidate or follower state), need to reset the leader heartbeat receive timer*/
        leaderHeartbeatReceiveTimer.reset(); // This is a heartbeat from the leader.

        /**
         *  2. Reply false if log doesn’t contain an entry at prevLogIndex
         *     whose term matches prevLogTerm (§5.3)
         **/
        LogEntry prevLogEntry;
        if (prevLogIndex >= 0) {
            try {
                prevLogEntry = pState.log().get(prevLogIndex);
            } catch (IndexOutOfBoundsException e) {
                throw new InvalidLogIndex("Attempt to append entry with invalid previous log index: " + prevLogIndex, prevLogIndex);
            }

            /* Need to check for the prev log term */
            if (prevLogEntry.term != prevLogTerm) {
                throw new PrevLogTermMismatch("Attempt to append entry with invalid previous log term.  Requested term " + prevLogTerm + ", actual term: " + prevLogEntry.term, prevLogIndex, prevLogEntry.term, prevLogTerm);
            }
        }

        /**
         *  3. If an existing entry conflicts with a new one (same index
         *     but different terms), delete the existing entry and all that
         *     follow it (§5.3)
         **/
        int logIndex = prevLogIndex;

        /* Delete the existing conflicting entries and append the new entries */
        if (pState.log().size() - 1 >= ++logIndex) {
            logger.info(String.format("%s: Removing conflicting log entries. Current log size=%d, Current commit index=%d. Replacing logs starting from index=%d", pState.myServerID, pState.log().size(), this.vState.getCommitIndex(), logIndex));
            pState.setLog(pState.log().subList(0, logIndex));
        }

        //entries is non null. Could be empty or with some entries in it
        pState.log().addAll(entries);

        /**
         *  4. If leaderCommit > commitIndex, set commitIndex =
         *     min(leaderCommit, index of last new entry)
         **/
        if (leaderCommit > vState.getCommitIndex()) {
            vState.setCommitIndex(Math.min(leaderCommit, pState.log().size() - 1), vState.getCommitIndex());
        }

        applyCommitted();

        return pState.getCurrentTerm();
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
    public int requestVote(int term, UUID candidate, int lastLogIndex, int lastLogTerm) throws InvalidTermException, AlreadyVotedException, CandidateBehindException {
        logger.info(String.format("%s received vote request from %s (term=%d, lastLogIndex=%d, lastLogTerm=%d)", pState.myServerID, candidate, term, lastLogIndex, lastLogTerm));
        if (!candidate.equals(pState.myServerID)) { // We sometimes vote for ourselves, to that's not considered a request from a remote server.
            /**
             * All servers convert to followers if their current term is behind (§5.1).
             */
            this.respondToRemoteTerm(term);
        }
        /**
         *  1. Reply false if term < currentTerm (§5.1)
         **/
        int currentTerm = pState.getCurrentTerm();
        if (term < currentTerm) {
            throw new InvalidTermException("Server: Received voting request for old leader term " + term + ", current term " + currentTerm + ". No vote granted.", currentTerm);
        }

        /**
         *  2. If votedFor is null or candidateId, and candidate’s log is at least as up-to-date as
         *     receiver’s log, grant vote (§5.2, §5.4)
         */
        UUID votedFor = pState.getVotedFor();
        if (votedFor.equals(NO_LEADER) || votedFor.equals(candidate)) {
            int localLogSize = pState.log().size();
            logger.info(String.format("%s deciding whether to vote for %s: local log size = %d", pState.myServerID, candidate, localLogSize));
            if (lastLogIndex >= this.lastLogIndex() && (localLogSize==0 || lastLogTerm >= pState.log().get(this.lastLogIndex()).term)) {
                logger.info(String.format("%s decided to vote for %s", pState.myServerID, candidate));
                pState.setVotedFor(candidate, pState.getVotedFor());
                return currentTerm; // Vote for her!
            }
            else {
                throw new CandidateBehindException(String.format("Candidate is behind.  Candidate last log index, term  = (%d, %d), current last log index, term = (%d, %d)",
                        lastLogIndex, lastLogTerm, this.lastLogIndex(), this.lastLogTerm()), pState.getCurrentTerm());
            }
        }
        else {
           throw new AlreadyVotedException(String.format("Request to vote for %s but already voted for %s (current term = %d)",
                   candidate, pState.getVotedFor(), pState.getCurrentTerm()), pState.getCurrentTerm());
        }
    }

    /**
     *  Apply log entries that have been committed but not yet applied.
     * • If commitIndex > lastApplied: increment lastApplied, apply
     *   log[lastApplied] to state machine (§5.3)
     */
    void applyCommitted() {
        int lastApplied;
        while (vState.getCommitIndex() > (lastApplied = vState.getLastApplied())) {
            LogEntry entry = pState.log().get(vState.incrementLastApplied(lastApplied));
            logger.info(pState.myServerID + ": Applying " + entry);
            try {
                applier.apply(entry.operation);
            }
            catch (java.lang.Exception e) {
                logger.warning(String.format("Operation %s generated exception %s on replica.  " +
                        "This should generally not be a problem, as the same exception should be " +
                        "generated on the master and all other replicas, and returned to the client " +
                        "for appropriate action (e.g. retry)", entry, e));
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
        int currentTerm = pState.getCurrentTerm();
        if (currentTerm < remoteTerm) {
            pState.setCurrentTerm(remoteTerm, currentTerm);
            State currentState = vState.getState();
            if (currentState != vState.getState().FOLLOWER) {
                become(State.FOLLOWER, currentState);
            }
        }
    }

    public Object applyToStateMachine(Object operation) throws java.lang.Exception {
        logger.info(String.format("%s: applyToStateMachine(%s)", pState.myServerID, operation));
        if (vState.getState() == Server.State.LEADER) {
            return leader.applyToStateMachine(operation);
        }
        else {
            throw new LeaderException(String.format("Current Leader is %s", vState.getCurrentLeader()), getCurrentLeader());
        }
    }


    private int lastLogIndex() {
        return pState.log().size()-1;
    }

    private int prevLogIndex() {
        return Math.max(lastLogIndex()-1, INVALID_INDEX);
    }

    private int lastLogTerm() {
        if (pState.log().isEmpty()) {
            return -1;
        }
        else {
            return  pState.log().get( pState.log().size()-1).term;
        }
    }

    private int prevLogTerm() {
        int prev = prevLogIndex();
        if( prev < 0) {
            return -1;
        }
        else {
            return  pState.log().get(prev).term;
        }
    }

    State getState() { // For unit testing only.
        return vState.getState();
    }

    class Leader {
        // Volatile state on leaders (reinitialized after election)
        /**
         * Next index for each server.
         */
        Map<UUID, Integer> nextIndex = new ConcurrentHashMap<UUID, Integer>();
        /**
         * Match index for each server.
         */
        Map<UUID, Integer> matchIndex = new ConcurrentHashMap<UUID, Integer>();

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
            logger.info(pState.myServerID + ": Start being a leader.");
            vState.setState(Server.State.LEADER, vState.getState()); // It doesn't matter what we were before.
            /**
             * Reinitialize volatile leader state
             */
            nextIndex.clear();
            matchIndex.clear();
            int lastLogIndex = lastLogIndex();
            for (UUID i : vState.otherServers.keySet()) {
                nextIndex.put(i, lastLogIndex + 1);
                matchIndex.put(i, 0);
            }

            appendEntriesThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(vState.otherServers.size() * 2 + 1);

            leaderHeartbeatSendTimer = new ResettableTimer(new TimerTask() {
                public void run() {
                    sendHeartbeats();
                }
            }, (long) LEADER_HEARTBEAT_PERIOD);
            sendHeartbeats();

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
            logger.info(pState.myServerID + ": Stop being a leader.");
            leaderHeartbeatSendTimer.cancel(); // Stop sending heartbeats.
            appendEntriesThreadPool.shutdownNow();
            appendEntriesThreadPool = null;
        }

        /**
         * Send a appendLog request to the specified server.
         *
         * @param otherServerID
         */
        void sendAppendEntries(UUID otherServerID) {
            boolean success = false;
            while (!success && vState.getState() == State.LEADER) {
                final Integer otherServerNextIndex = leader.nextIndex.get(otherServerID);
                try {
                    logger.info(String.format("%s sending appendEntries to %s: otherServerNextIndex=%d, log.size=%d",
                            pState.myServerID, otherServerID, otherServerNextIndex, pState.log().size()));
                    int nextIndex = otherServerNextIndex == null ? 0 : otherServerNextIndex;
                    int prevLogTerm;
                    // TODO: If nextIndex == 0 and log.size() > 0, prevLogTerm should be set to log.get(0).term
                    // But it is currently set to INVALID_INDEX (-1)
                    if (nextIndex > 0 && pState.log().size() > 0) {
                        prevLogTerm = pState.log().get(otherServerNextIndex - 1).term;
                    }
                    else {
                        prevLogTerm = INVALID_INDEX;
                    }
                    List<LogEntry> entries = pState.log().size() > 0 ? new ArrayList(pState.log().subList(nextIndex, lastLogIndex() + 1)) : NO_LOG_ENTRIES;
                    int remoteTerm = getServer(otherServerID).appendEntries(pState.getCurrentTerm(), pState.myServerID,
                            nextIndex - 1, prevLogTerm, entries, vState.getCommitIndex());
                    success = true;
                    respondToRemoteTerm(remoteTerm); // Might lose leadership.
                } catch (InvalidTermException e) {
                    logger.warning(e.toString());
                    respondToRemoteTerm(e.currentTerm);
                } catch (PrevLogTermMismatch e) {
                    logger.warning(e.toString());
                    this.nextIndex.put(otherServerID, otherServerNextIndex - 1); // Decrement and try again.
                } catch (InvalidLogIndex e) { // The remote server doesn't have that log entry at all.
                    logger.severe(e.toString());
                    this.nextIndex.put(otherServerID, otherServerNextIndex - 1); // Decrement and try again.
                }
            }
            if (vState.getState() == State.LEADER) {
                this.nextIndex.put(otherServerID, lastLogIndex() + 1);
                this.matchIndex.put(otherServerID, lastLogIndex());
            }
        }

        /**
         * Send heartbeats to all servers.
         */
        void sendHeartbeats() { // We might want to keep a separate timer for each other server.  For now, we use one timer.
            if (vState.getState() == State.LEADER) {
                final Iterator<UUID> i = vState.otherServers.keySet().iterator();
                while (i.hasNext()) {
                    final UUID server = i.next();
                    appendEntriesThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            sendAppendEntries(server);
                            updateCommitIndex();
                            try {
                                applyCommitted();
                            }
                            catch (java.lang.Exception e) {
                                logger.warning(String.format("%s: Applying operatation remotely on %s generated exception %s.  This can probably be ignored.",
                                        pState.myServerID, server, e));
                            }
                        }
                    });
                }
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
            synchronized(pState) {
                if (pState.log().get(logIndex).term != pState.getCurrentTerm()) {
                    return false;
                }
            }

            int matches = 0;
            if (0 == vState.otherServers.size()) {
                return true;
            }

            for (UUID otherServerID : vState.otherServers.keySet()) {
                int match = leader.matchIndex.get(otherServerID);
                if (match >= logIndex) {
                    if (++matches >= majorityQuorumSize() - 1) { // -1 because the leader implicitly matches
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
            for (int i = lastLogIndex(); i > vState.getCommitIndex(); i--) {
                if (shouldBeCommitted(i)) {
                    vState.setCommitIndex(i, vState.getCommitIndex());
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
        // TODO: This method should be thread safe
        // Currently this thread is not thread safe. Suppose we have multiple threads executing this
        // method in different speed. We further assume that the lastLogIndex observed by thread A is
        // 20, thread A sends append entry requests to other servers to replicate entries up to 20.
        // After thread A receives quorum replies, it will update the committed index to the latest
        // index. In this case the latest index may have been increased to 25 by other threads.
        public Object applyToStateMachine(Object operation) throws java.lang.Exception {
            /**
             *  If command received from client: append entry to local log, respond after entry applied to state machine (§5.3)
             */
            logger.info(String.format("%s: applyToStateMachine(%s)", pState.myServerID, operation));
            final int logIndex;
            synchronized (pState) {
                pState.log().add(new LogEntry(operation, pState.getCurrentTerm()));
                logIndex = lastLogIndex();
            }
            final Semaphore replicationCounter = new Semaphore(0); // Initially we have zero replicas
            final Iterator<UUID> i = vState.otherServers.keySet().iterator();
            while (i.hasNext()) {
                final UUID otherServerID = i.next();
                final Integer otherServerNextIndex = leader.nextIndex.get(otherServerID);

                // TODO: We should allocate one replication thread for each follower.
                // Blindly putting replication request into a thread pool has a few drawbacks:
                // 1) A dead follower will take all available threads and make the system hang
                // 2) We have to deal with race conditions between multiple threads which try
                // to update the status of the same follower
                appendEntriesThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (otherServerNextIndex == null || logIndex >= otherServerNextIndex) { // This is always true, but we put it here to be consistent with the formal algorithm.
                                sendAppendEntries(otherServerID);
                        } else {
                            logger.warning(String.format("%s: Whooah! logIndex %d is not greater than otherServerNextIndex %s", pState.myServerID, logIndex, otherServerNextIndex));
                        }
                        replicationCounter.release();
                    }
                });
            }
            logger.info(String.format("%s: Waiting for logindex %d to be committed to %d majority quorum.", pState.myServerID, logIndex, majorityQuorumSize()));
            try {
                replicationCounter.acquire(majorityQuorumSize() - 1);
            } catch (InterruptedException e) {
                logger.warning(String.format("Failed to commit logindex %d to quorum of %d.  Reason: %s", logIndex, majorityQuorumSize(), e));
            }
            logger.info(String.format("%s: Logindex %d was committed to %d majority quorum.", pState.myServerID, logIndex, majorityQuorumSize()));
            updateCommitIndex();
            Object returnVal = applyCommitted();
            if (vState.getLastApplied() >= logIndex) {
                logger.info("logIndex " + logIndex + " applied to state machine: " + operation);
            } else {
                logger.severe(String.format("Whooah! Internal error: logIndex %d was committed by a majority quorum, but not applied locally.", logIndex));
            }
            return returnVal;
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
            java.lang.Exception lastException = null;
            synchronized(vState) {
                while (vState.getCommitIndex() > vState.getLastApplied()) {
                    LogEntry entry = pState.log().get(vState.incrementLastApplied(vState.getLastApplied()));
                    logger.info(pState.myServerID + ": Applying " + entry);
                    try {
                        lastReturnVal = applier.apply(entry.operation);
                        lastException = null;
                    }
                    catch (java.lang.Exception e) {
                        logger.warning(String.format("Operation %s generated exception %s.  " +
                                "This should generally not be a problem, as the same exception should be " +
                                "generated on the master and all other replicas, and returned to the client " +
                                "for appropriate action (e.g. retry)", entry, e));
                        lastException = e;
                    }

                }
            }
            if (lastException != null) {
                throw lastException;
            }
            return lastReturnVal;
        }
    }

    int majorityQuorumSize() {
        // Quorum size is 2f+1.  f+1 votes are required for a majority.
        int f  = fullQuorumSize()/2+1; // Note integer arithmetic rounds down.
        return f;
    }

    int fullQuorumSize() {
        return vState.otherServers.size()+1; // NOTE: This only makes sense if otherServers contains all the other servers at this time.
    }

    RemoteRaftServer getServer(UUID serverId) {
        return vState.otherServers.get(serverId); // TODO: Handle not found - which would indicate a code bug.
    }

    public void addServer(UUID id, RemoteRaftServer server) {
        vState.otherServers.put(id, server);
    }

    RemoteRaftServer getCurrentLeader() {
        return getServer(vState.getCurrentLeader());
    }

    class Follower {
        Follower() {
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
            logger.info(pState.myServerID + ": Start being a follower.");
            vState.setState(State.FOLLOWER, vState.getState()); // Doesn't matter what we were before.

            /* On state transition to follower, should reset the votedFor. Consider the case, when
            requestVote is received from remote server with higher term(compared to our current term).
            It triggers state transition to follower inside respondToRemoteTerm. And continue further
            processing to decide whether to vote for that remote server. So if we don't reset votedFor
            here, requestVote would fail taking old votedFor into consideration. */
            pState.setVotedFor(NO_LEADER, pState.getVotedFor());

            vState.removeCurrentLeader();

            if (null == leaderHeartbeatReceiveTimer) {
                /* Create a leader heartbeat timer instance for the very first time follower.start
                method is called. Further call to follower.start due to FSM, doesn't create another
                instance. Just restart the existing leader heartbeat timer instance */
                leaderHeartbeatReceiveTimer = new ResettableTimer(new TimerTask() {
                    public void run() {
                        /**
                         * If we don't receive a heartbeat from the leader, start an election.
                         */
                        become(State.CANDIDATE, State.FOLLOWER);
                    }
                }, (long) LEADER_HEARTBEAT_TIMEOUT);
            }

            leaderHeartbeatReceiveTimer.start(); // Expect to receive heartbeats from the leader.
        }

        /**
         * Stop being a follower.
         */
        void stop() {
            logger.info(pState.myServerID + ": Stop being a follower.");
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
        ResettableTimer leaderElectionTimer;

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
            logger.info(pState.myServerID + ": Start being a candidate.");
            vState.setState(State.CANDIDATE, vState.getState()); // Doesn't matter what we were before.

            /* Need to reset the votedFor before requesting vote. Because, it could have happened
            that we had received a vote request from remote server and voted for it before the state
            transition happen from follower to candidate upon leaderHeartbeatReceiveTimer expiry. It
            avoids AlreadyVotedException while voting for self */
            pState.setVotedFor(NO_LEADER, pState.getVotedFor());

            if (null == leaderElectionTimer) {
                /* Create a leader election timer instance for the very first time candidate.start
                method is called. Further call to candidate.start due to FSM, doesn't create another
                instance. Just restart the existing leader election timer instance */
                leaderElectionTimer = new ResettableTimer(new TimerTask() {
                    public void run() {
                        /**
                         * If no leader is elected within the timeout, start another election.
                         */
                        become(State.CANDIDATE, vState.getState());
                    }
                }, (long) LEADER_ELECTION_TIMEOUT);
            }

            pState.incrementCurrentTerm(pState.getCurrentTerm());

            try { // Vote for self
                requestVote(pState.getCurrentTerm(), pState.myServerID, lastLogIndex(), lastLogTerm());
            }
            catch (VotingException e) {
                logger.warning("Unexpected error voting for self: " + e.toString());
                become(State.FOLLOWER, State.CANDIDATE);
                return;
            }

            voteRequestThreadPool = (ThreadPoolExecutor)Executors.newFixedThreadPool(vState.otherServers.size() + 1);

            this.leaderElectionTimer.start();
            sendVoteRequests();
        }

        /**
         * Stop being a candidate.
         */
        void stop() {
            logger.info(pState.myServerID + ": Stop being a candidate.");
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
            final Integer currentTerm = pState.getCurrentTerm();
            RemoteRaftServer server = getServer(serverID);
            boolean voteGranted = true;
            try {
                logger.info("Sending vote request to server " + serverID);
                server.requestVote(pState.getCurrentTerm(), pState.myServerID, lastLogIndex(), lastLogTerm());

                /* Condition to check if this grant is still valid/useful */
                /* If the state is follower OR if it is candidate or leader with term changed,
                then this grant is not valid anymore. Also, once majority quorum grant votes,
                state transition happens from candidate to leader and can safely consider
                the pending awaited vote grants as not useful. It can happen due to blocking call */
                if ((vState.getState() != State.CANDIDATE) || (!currentTerm.equals(pState.getCurrentTerm()))) {
                    //Not a valid/useful grant
                    voteGranted = false;
                    logger.info(String.format("%s While waiting in blocking call, state transitioned from CANDIDATE to %s, old term : %d and current term : %d", pState.myServerID, vState.getState(), currentTerm, pState.getCurrentTerm()));
                }
            }
            catch (VotingException e) {
                voteGranted = false;
                logger.info("Leader election vote request denied by server: " + e.toString());
                respondToRemoteTerm(e.currentTerm);
            }
            if (voteGranted) {
                logger.info("Vote received from server " + serverID);
                voteCounter.release(1); // Yay!  We got one vote.
            }
        }

        /**
         * Send vote requests to all other servers.
         * Returns once all vote requests have been queued for sending, and a thread has been created
         * to gather all responses and convert to leader or start another election, as appropriate.
         */
        void sendVoteRequests() {
            logger.info("Sending vote requests to " + vState.otherServers.keySet().size() + " other servers");
            final Semaphore voteCounter = new Semaphore(0); // Initially we have zero votes.
            // Send vote requests in parallel
            final Iterator<UUID> i = vState.otherServers.keySet().iterator();
            final Integer currentTerm = pState.getCurrentTerm();
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
                        votedInAsLeader = voteCounter.tryAcquire(majorityQuorumSize() - 1, LEADER_ELECTION_TIMEOUT, TimeUnit.MILLISECONDS);

                    } catch (InterruptedException e) {
                        logger.info(pState.myServerID + "Interrupted while waiting to receive a majority of votes in leader election.");
                    }

                    /* If the state is not candidate or term has changed during election process,
                    then this election is not valid anymore. Just return. It can happen due to
                    blocking call */
                    if ((vState.getState() != State.CANDIDATE) || (!currentTerm.equals(pState.getCurrentTerm()))) {
                        logger.info(String.format("%s While waiting in blocking call, state transitioned from CANDIDATE to %s, old term : %d and current term : %d", pState.myServerID, vState.getState(), currentTerm, pState.getCurrentTerm()));
                        return;
                    }

                    /* Control reaches here only when current state is CANDIDATE */
                    if (votedInAsLeader) {
                        become(State.LEADER, State.CANDIDATE);
                    } else {
                        become(State.CANDIDATE, State.CANDIDATE);
                    }
                }
            });
        }
    }
}

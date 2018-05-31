package sapphire.policy.util.consensus.raft;

import java.util.List;
import java.util.UUID;

/**
 * Remote methods exposed by all RAFT servers.
 * In Sapphire, these RMI method calls are received by ServerPolicy DM (from remote RAFT servers),
 * and delegated to the local RAFT Server instance.  See the RAFT paper for details:
 * https://www.usenix.org/system/files/conference/atc14/atc14-paper-ongaro.pdf
 * Created by quinton on 4/24/18.
 */

public interface RemoteRaftServer {
    /**
     * appendEntries is invoked by leader to replicate log entries; also used as a heartbeat.
     *
     * @param term         leaders' term
     * @param leader       so follower can redirect clients
     * @param prevLogIndex index of log entry immediately preceding new ones
     * @param prevLogTerm  term of prevLogIndex entry
     * @param entries      log entries to store (empty for heartbeat; may send more than one for efficiency)
     * @param leaderCommit leader’s commitIndex
     * @return currentTerm, for leader to update itself
     * @throws InvalidTermException
     * @throws PrevLogTermMismatch
     * @throws InvalidLogIndex
     */
    int appendEntries(int term, UUID leader, int prevLogIndex, int prevLogTerm, List<LogEntry> entries, int leaderCommit)
            throws InvalidTermException, PrevLogTermMismatch, InvalidLogIndex;

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
    int requestVote(int term, UUID candidate, int lastLogIndex, int lastLogTerm)
            throws InvalidTermException, AlreadyVotedException, CandidateBehindException;

    /**
     * applyToStateMachine applies an operation to the local state machine if the local node is
     * the master, or forwards to the master if not.
     * @param operation
     * @return
     * @throws java.lang.Exception
     */
    Object applyToStateMachine(Object operation) throws java.lang.Exception;
}
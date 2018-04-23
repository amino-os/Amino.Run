package sapphire.policy.util.consensus.raft;

import java.lang.*;
import java.util.List;
import java.util.UUID;

/**
 * Created by Venugopal Reddy K on 4/20/18.
 * Interface to execute calls on remote raft servers
 */

/*TODO: Currently policy stubs swallows all the exceptions and no exceptions are thrown to client.
When stub generation code is modified not to catch client exceptions, below methods can catch these
specific exceptions. And, need to remove the generic exception thrown from these methods.
 */
public interface RemoteRaftServerInvoker {
	/**
	 * Invoked by candidate to gather votes from remote servers.
	 * @param remoteServer Remote server on which this call will be invoked
	 * @param term candidate’s term
	 * @param candidate candidate requesting vote
	 * @param lastLogIndex index of candidate’s last log entry
	 * @param lastLogTerm term of candidate’s last log entry
	 * @return currentTerm, for candidate to update itself
	 * @throws sapphire.policy.util.consensus.raft.Server.InvalidTermException
	 * @throws sapphire.policy.util.consensus.raft.Server.AlreadyVotedException
	 * @throws sapphire.policy.util.consensus.raft.Server.CandidateBehindException
	 */
	int requestVote(Object remoteServer, int term, UUID candidate, int lastLogIndex, int lastLogTerm) throws Server.InvalidTermException, Server.AlreadyVotedException, Server.CandidateBehindException, java.lang.Exception;

	/**
	 * Invoked by leader to replicate log entries to remote servers
	 * @param remoteServer Remote server on which this call will be invoked
	 * @param term leaders' term
	 * @param leader so follower can redirect clients
	 * @param prevLogIndex index of log entry immediately preceding new ones
	 * @param prevLogTerm term of prevLogIndex entry
	 * @param entries log entries to store (empty for heartbeat; may send more than one for efficiency)
	 * @param leaderCommit leader’s commitIndex
	 * @return currentTerm, for leader to update itself
	 * @throws sapphire.policy.util.consensus.raft.Server.InvalidTermException
	 * @throws sapphire.policy.util.consensus.raft.Server.PrevLogTermMismatch
	 * @throws sapphire.policy.util.consensus.raft.Server.InvalidLogIndex
	 */
	int appendEntries(Object remoteServer, int term, UUID leader, int prevLogIndex, int prevLogTerm, List<Object> entries, int leaderCommit) throws Server.InvalidTermException, Server.PrevLogTermMismatch, Server.InvalidLogIndex, java.lang.Exception;

	/**
 	 * Invoked by followers to redirect the client's operation invoked them to get applied on leader
 	 * @param remoteServer Remote server on which this call will be invoked(i.e., leader)
	 * @param operation Client's operation
	 * @throws java.lang.Exception
 	 */
	Object applyToStateMachine(Object remoteServer, Object operation) throws java.lang.Exception;
}

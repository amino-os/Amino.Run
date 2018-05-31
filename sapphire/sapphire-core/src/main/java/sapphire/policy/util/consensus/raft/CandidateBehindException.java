package sapphire.policy.util.consensus.raft;

/**
 * Created by quinton on 5/25/18.
 */

/**
 * If candidate’s log is not at least as up-to-date as receiver’s log on requestVote RPC
 */
public class CandidateBehindException extends VotingException {
	public CandidateBehindException(String s, int currentTerm) {
		super(s, currentTerm);
	}
}

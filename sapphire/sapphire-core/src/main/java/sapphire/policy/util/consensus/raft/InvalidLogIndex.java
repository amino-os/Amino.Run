package sapphire.policy.util.consensus.raft;

/**
 * Created by quinton on 5/25/18.
 */

/**
 * If log doesn’t contain an entry at specified logIndex on appendEntries RPC
 */
public class InvalidLogIndex extends RaftRuntimeException {
	int invalidIndex;
	public InvalidLogIndex(String s, int invalidIndex) {
		super(s);
		this.invalidIndex = invalidIndex;
	}
}

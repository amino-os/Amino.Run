package sapphire.policy.util.consensus.raft;

/**
 * Created by Venugopal Reddy K on 5/25/18.
 */

/**
 * Leader exception is thrown when onRPC is invoked to raft follower servers
 */
public class LeaderException extends Exception {
	RemoteRaftServer leader;
	public LeaderException(String s, RemoteRaftServer leader) {
		super(s);
		this.leader = leader;
	}

	public RemoteRaftServer getLeader() { return leader;}
}

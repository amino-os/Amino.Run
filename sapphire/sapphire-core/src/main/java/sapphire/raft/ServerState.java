package sapphire.raft;

/**
 * @author terryz
 */

public class ServerState {
    public static enum State {
        LEADER, FOLLOWER, CANDIDATE
    }
}

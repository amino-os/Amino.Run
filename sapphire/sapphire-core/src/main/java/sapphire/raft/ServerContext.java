package sapphire.raft;

import java.time.Duration;

/**
 * Raft Configurations
 *
 * @author terryz
 */

public class ServerContext {
    private Duration electionTimeout = Duration.ofMillis(500);
    private Duration heartbeatInterval = Duration.ofMillis(150);

    public Duration getElectionTimeout() {
        return electionTimeout;
    }

    public void setElectionTimeout(Duration electionTimeout) {
        this.electionTimeout = electionTimeout;
    }

    public Duration getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(Duration heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }
}

package sapphire.policy.scalability;

import java.util.List;

/**
 * @author terryz
 */
public class ReplicationRequest {
    private List<LogEntry> entries;

    public ReplicationRequest(List<LogEntry> entries) {
        this.entries = entries;
    }
}

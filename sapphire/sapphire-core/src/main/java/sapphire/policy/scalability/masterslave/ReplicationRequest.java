package sapphire.policy.scalability.masterslave;

import java.io.Serializable;
import java.util.List;

/**
 * Request for entry replications from master to slave
 *
 * @author terryz
 */
public class ReplicationRequest implements Serializable {
    private final long indexOfLargestCommittedEntry;
    private final List<LogEntry> entries;

    public ReplicationRequest(long indexOfLargestCommittedEntry, List<LogEntry> entries) {
        this.indexOfLargestCommittedEntry = indexOfLargestCommittedEntry;
        this.entries = entries;
    }

    /** @return log entries */
    public List<LogEntry> getEntries() {
        return entries;
    }

    /** @return index of largest committed entry */
    public long getIndexOfLargestCommittedEntry() {
        return this.indexOfLargestCommittedEntry;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ReplicationRequest{");
        sb.append("indexOfLargestCommittedEntry=").append(indexOfLargestCommittedEntry);
        sb.append(", entries=").append(entries);
        sb.append('}');
        return sb.toString();
    }
}

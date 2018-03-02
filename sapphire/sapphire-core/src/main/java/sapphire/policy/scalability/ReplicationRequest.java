package sapphire.policy.scalability;

import java.util.List;

/**
 * @author terryz
 */
public class ReplicationRequest {
    private final long indexOfPreviousSyncedEntry;
    private final List<LogEntry> entries;

    private ReplicationRequest(Builder builder) {
        this.indexOfPreviousSyncedEntry = builder.indexOfPreviousSyncedEntry;
        this.entries = builder.entries;
    }

    public List<LogEntry> getEntries() {
        return entries;
    }

    public long getIndexOfPreviousSyncedEntry() {
        return this.indexOfPreviousSyncedEntry;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ReplicationRequest{" +
                "indexOfPreviousSyncedEntry=" + indexOfPreviousSyncedEntry +
                ", entries=" + entries +
                '}';
    }

    public static class Builder {
        private long indexOfPreviousSyncedEntry;
        private List<LogEntry> entries;

        public Builder indexOfPreviousSyncedEntry(long indexOfPreviousSyncedEntry) {
            this.indexOfPreviousSyncedEntry = indexOfPreviousSyncedEntry;
            return this;
        }

        public Builder entries(List<LogEntry> entries) {
            this.entries = entries;
            return this;
        }

        public ReplicationRequest build() {
            return new ReplicationRequest(this);
        }
    }
}

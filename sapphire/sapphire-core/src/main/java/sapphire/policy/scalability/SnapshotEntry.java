package sapphire.policy.scalability;

import java.util.Objects;

/**
 * @author terryz
 */
public class SnapshotEntry extends Entry {
    private final String logFilePath;
    private final String snapshotFilePath;
    private final long indexOfLargestCommittedEntry;
    private final long indexOfLargestReplicatedEntry;
    private final Object appObject;
    private final long lowestOffsetInLogFile;

    private SnapshotEntry(Builder builder) {
        super(builder.term, builder.index);
        this.logFilePath = builder.logFilePath;
        this.snapshotFilePath = builder.snapshotFilePath;
        this.appObject = builder.appObject;
        this.indexOfLargestCommittedEntry = builder.indexOfLargestCommittedEntry;
        this.indexOfLargestReplicatedEntry = builder.indexOfLargestReplicatedEntry;
        this.lowestOffsetInLogFile = builder.lowestOffsetInLogFile;
    }

    public final static Builder newBuilder() {
        return new Builder();
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    public String getSnapshotFilePath() {
        return snapshotFilePath;
    }

    public long getIndexOfLargestCommittedEntry() {
        return indexOfLargestCommittedEntry;
    }

    public long getIndexOfLargestReplicatedEntry() {
        return indexOfLargestReplicatedEntry;
    }

    public Object getAppObject() {
        return appObject;
    }

    public long getLowestOffsetInLogFile() {
        return this.lowestOffsetInLogFile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SnapshotEntry)) return false;
        SnapshotEntry that = (SnapshotEntry) o;
        return getIndexOfLargestCommittedEntry() == that.getIndexOfLargestCommittedEntry() &&
                getIndexOfLargestReplicatedEntry() == that.getIndexOfLargestReplicatedEntry() &&
                getLowestOffsetInLogFile() == that.getLowestOffsetInLogFile() &&
                Objects.equals(getLogFilePath(), that.getLogFilePath()) &&
                Objects.equals(getSnapshotFilePath(), that.getSnapshotFilePath()) &&
                Objects.equals(getAppObject(), that.getAppObject());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLogFilePath(), getSnapshotFilePath(),
                getIndexOfLargestCommittedEntry(), getIndexOfLargestReplicatedEntry(),
                getAppObject(), getLowestOffsetInLogFile());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SnapshotEntry{");
        sb.append("logFilePath='").append(logFilePath).append('\'');
        sb.append(", snapshotFilePath='").append(snapshotFilePath).append('\'');
        sb.append(", indexOfLargestCommittedEntry=").append(indexOfLargestCommittedEntry);
        sb.append(", indexOfLargestReplicatedEntry=").append(indexOfLargestReplicatedEntry);
        sb.append(", appObject=").append(appObject);
        sb.append(", lowestOffsetInLogFile=").append(lowestOffsetInLogFile);
        sb.append('}');
        return sb.toString();
    }

    public final static class Builder {
        private long term;
        private long index;
        private String logFilePath;
        private String snapshotFilePath;
        private Object appObject;
        private long indexOfLargestCommittedEntry;
        private long indexOfLargestReplicatedEntry;
        private long lowestOffsetInLogFile;

        public Builder term(long term) {
            this.term = term;
            return this;
        }

        public Builder index(long index) {
            this.index = index;
            return this;
        }

        public Builder logFilePath(String logFilePath) {
            this.logFilePath = logFilePath;
            return this;
        }

        public Builder snapshotFilePath(String snapshotFilePath) {
            this.snapshotFilePath = snapshotFilePath;
            return this;
        }

        public Builder indexOfLargestCommittedEntry(long indexOfLargestCommittedEntry) {
            this.indexOfLargestCommittedEntry = indexOfLargestCommittedEntry;
            return this;
        }

        public Builder indexOfLargestReplicatedEntry(long indexOfLargestReplicatedEntry) {
            this.indexOfLargestReplicatedEntry = indexOfLargestReplicatedEntry;
            return this;
        }

        public Builder lowestOffsetInLogFile(long lowestOffsetInLogFile) {
            this.lowestOffsetInLogFile = lowestOffsetInLogFile;
            return this;
        }

        public Builder appObject(Object appObject) {
            this.appObject = appObject;
            return this;
        }

        public SnapshotEntry build() {
            if (indexOfLargestReplicatedEntry > index) {
                throw new IllegalStateException(String.format("indexOfLargestReplicatedEntry(%s) should not greater than snapshot index(%s)", indexOfLargestReplicatedEntry, index));
            }

            if (lowestOffsetInLogFile < 0) {
                throw new IllegalArgumentException(String.format("invalid negative offset %s", lowestOffsetInLogFile));
            }

            if (logFilePath == null || logFilePath.isEmpty()) {
                throw new IllegalArgumentException("log file path is not specified");
            }

            if (snapshotFilePath == null || snapshotFilePath.isEmpty()) {
                throw new IllegalArgumentException("snapshot file path is not specified");
            }

            return new SnapshotEntry(this);
        }
    }
}

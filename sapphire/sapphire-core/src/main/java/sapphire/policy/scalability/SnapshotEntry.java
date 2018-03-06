package sapphire.policy.scalability;

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

        if (getIndexOfLargestCommittedEntry() != that.getIndexOfLargestCommittedEntry())
            return false;
        if (getIndexOfLargestReplicatedEntry() != that.getIndexOfLargestReplicatedEntry())
            return false;
        if (getLowestOffsetInLogFile() != that.getLowestOffsetInLogFile()) return false;
        if (!getLogFilePath().equals(that.getLogFilePath())) return false;
        if (!getSnapshotFilePath().equals(that.getSnapshotFilePath())) return false;
        return getAppObject().equals(that.getAppObject());
    }

    @Override
    public int hashCode() {
        int result = getLogFilePath().hashCode();
        result = 31 * result + getSnapshotFilePath().hashCode();
        result = 31 * result + (int) (getIndexOfLargestCommittedEntry() ^ (getIndexOfLargestCommittedEntry() >>> 32));
        result = 31 * result + (int) (getIndexOfLargestReplicatedEntry() ^ (getIndexOfLargestReplicatedEntry() >>> 32));
        result = 31 * result + getAppObject().hashCode();
        result = 31 * result + (int) (getLowestOffsetInLogFile() ^ (getLowestOffsetInLogFile() >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "SnapshotEntry{" +
                "logFilePath='" + logFilePath + '\'' +
                ", snapshotFilePath='" + snapshotFilePath + '\'' +
                ", indexOfLargestCommittedEntry=" + indexOfLargestCommittedEntry +
                ", indexOfLargestReplicatedEntry=" + indexOfLargestReplicatedEntry +
                ", appObject=" + appObject +
                ", lowestOffsetInLogFile=" + lowestOffsetInLogFile +
                '}';
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
            if (indexOfLargestCommittedEntry > index) {
                throw new IllegalStateException(String.format("indexOfLargestCommittedEntry(%s) should not greater than snapshot index(%s)", indexOfLargestCommittedEntry, index));
            }

            if (indexOfLargestReplicatedEntry > index) {
                throw new IllegalStateException(String.format("indexOfLargestReplicatedEntry(%s) should not greater than snapshot index(%s)", indexOfLargestReplicatedEntry, index));
            }

            if (lowestOffsetInLogFile < 0) {
                throw new IllegalArgumentException(String.format("invalid negative offset %s", lowestOffsetInLogFile));
            }

            if (appObject == null) {
                throw new IllegalStateException("appObject in snapshot is null");
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

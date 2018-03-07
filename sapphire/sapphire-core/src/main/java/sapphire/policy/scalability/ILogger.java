package sapphire.policy.scalability;

import java.io.Closeable;
import java.util.List;

/**
 * @author terryz
 */
public interface ILogger<T> extends Closeable {
    /**
     * Appends log entry into log
     *
     * @param entry log entry
     * @return offset of the log entry
     * @throws Exception if append fails
     */
    long append(T entry) throws Exception;

    /**
     * Returns the log entry at the specified offset
     *
     * @param offset offset in log file
     * @return the {@link LogEntry} at the specified offset, or <code>null</code> if reaches the
     * end of file
     * @throws Exception
     */
    LogEntry read(long offset) throws Exception;

    /**
     * Load all log entries from log file
     *
     * @param offset starting offset of entries to be loaded
     * @throws Exception
     */
    void load(long offset) throws Exception;

    /**
     * Marks the given log entry as replicated.
     * <p>
     * On master, an entry is replicated means the entry has been replicated
     * to slave. On slave, an entry is replicated means the entry is received
     * on slave.
     * @param entry
     */
    void markReplicated(T entry);

    void markReplicated(long largestReplicatedIndex);

    /**
     * Takes snapshot
     *
     * @return snapshot entry
     * @throws Exception
     */
    SnapshotEntry takeSnapshot() throws Exception;

    /**
     * Returns index of the largest replicated entry. A log entry is replicated
     * iff its request has been invoked on <b>master</b> and the log
     * entry has been replicated to slave.
     *
     * This field is only used on master. It is meaningless on slaves.
     *
     * @return index of the largest replicated entry
     */
    long getIndexOfLargestReplicatedEntry();

    /**
     * Returns index of the largest received entry.
     *
     * This field is only used on slaves. It is meaningless on master.
     *
     * @return index of the largest received entry
     */
    long getIndexOfLargestReceivedEntry();

    /**
     * Returns the index of the largest committed entry.
     *
     * On master, the largest committed entry is the largest entry;
     * on slave, the index of the largest committed entry is always
     * less than or equal to the largest received entry.
     *
     * @return index of the largest committed entry
     */
    long getIndexOfLargestCommittedEntry();

    /**
     * @return a list of unreplicated log entries
     */
    List<LogEntry> getUnreplicatedEntries();

    /**
     * @return a list of uncommitted log entries
     */
    List<LogEntry> getUncomittedEntries();

    /**
     *
     * @param index
     * @return
     */
    boolean indexExists(long index);
}
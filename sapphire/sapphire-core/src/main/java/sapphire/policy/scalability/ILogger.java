package sapphire.policy.scalability;

import java.util.List;

/**
 * @author terryz
 */
public interface ILogger<T> {
    /**
     * Appends log entry into log
     *
     * @param entry log entry
     * @return offset of the log entry
     * @throws Exception if append fails
     */
    long append(T entry) throws Exception;

    /**
     * @param offset
     * @return the {@link LogEntry} at the specified offset
     * @throws Exception
     */
    LogEntry read(long offset) throws Exception;
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
         * Marks the given log entry as applied. A log entry is applied iff
         * its request has been invoked on slave.
         * <p>
         *
         * On mater, an entry is committed means the request in the entry has
         * been invoked but the entry may not be replicated to slave. On slave,
         * an entry is committed means the request has been invoked on slave.
         *
         * @param entry
         */
    void markCommitted(T entry);

    /**
     * Takes snapshot
     *
     * @param appObject snapshot of appObject
     * @return offset of the snapshot entry
     * @throws Exception
     */
    long takeSnapshot(Object appObject) throws Exception;

    /**
     * @return index of the largest replicated entry
     */
    long getIndexOfLargestReplicatedEntry();

    /**
     * @return a list of unreplicated log entries
     */
    List<LogEntry> getUnreplicatedEntries();

    /**
     *
     * @param index
     * @return
     */
    boolean indexExists(long index);
}
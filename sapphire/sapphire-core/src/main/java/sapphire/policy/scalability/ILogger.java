package sapphire.policy.scalability;

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
     * Marks the given log entry as replicated.
     * <p>
     * <b>This method will only be caused on slave.</b>
     *
     * @param entry
     */
    void markReplicated(T entry);

    /**
     * Marks the given log entry as applied. A log entry is applied iff
     * its request has been invoked on slave.
     * <p>
     * <b>This method will only be caused on slave.</b>
     *
     * @param entry
     */
    void markApplied(T entry);

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
     *
     * @param index
     * @return
     */
    boolean indexExists(long index);
}
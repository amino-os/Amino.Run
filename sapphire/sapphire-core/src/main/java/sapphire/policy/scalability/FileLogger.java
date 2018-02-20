package sapphire.policy.scalability;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *         R : Committed and Replicated Log Entries (on master)
 *         C : Committed but not Replicated Log Entries (on master)
 *         . : Received but not Applied Log Entries (on slave)
 *         X : Crash
 *  index(R) : Index of log entry R
 * offset(R) : offset of log entry R in file
 *
 * <p>
 * <b>Master:</b>
 *
 * Invariance:
 *     index(LargestReplicatedEntryOnSnapshot) <= index(LargestCommittedEntryOnSnapshot)
 *
 * Time T0 (snapshot time)
 *        R    R    R    R    C    C    C    C
 *                       ^                   ^
 *                       |                   |
 *   LargestReplicatedEntryOnSnapshot   SnapshotPoint(LargestCommittedEntryOnSnapshot)
 *
 * Time T1 (crash time)
 *
 *        R    R    R    R    R    R    R    R    R    R    R    R    R    C    C    C    C    C    X
 *                       ^                   ^                                                      ^
 *                       |                   |                                                      |
 *   LargestReplicatedEntryOnSnapshot   SnapshotPoint                                             Crash
 *
 * Case 1: Time T2 (come back as master again)
 *
 *                            <---------------     Send Replication Requests     -------------->
 *                                                <----------    Replay Log Entries    -------->
 *        R    R    R    R    R    R    R    R    R    R    R    R    R    C    C    C    C    C
 *                       ^                   ^
 *                       |                   |
 *   LargestReplicatedEntryOnSnapshot   SnapshotPoint
 *
 * Case 2: Time T2
 * Come back as slave. This is a temporary state. Since these are some commits that have not been
 * replicated to the other slave node, the other slave node will not be able to grab the lock and
 * therefore won't become the master. This server will periodically try to obtain the lock and
 * eventually it will become the master.
 *
 * <p>
 * <b>Slave:</b>
 *
 * Invariance:
 *     index(LargestCommittedEntryOnSnapshot) <= index(LargestReceivedEntryOnSnapshot)
 *
 * Time T0
 *       C    C    .    .    .    .    .
 *            ^                        ^
 *            |                        |
 *    LargestCommittedEntryOnSnapshot SnapshotPoint(LargestReceivedEntryOnSnapshot)
 *
 * Time T1
 *
 *       C    C    C    C    C    C    C    C    C    C    C    .    .    .    X
 *            ^                        ^                                       ^
 *            |                        |                                       |
 *    LargestCommittedEntryOnSnapshot  SnapshotPoint                         Crash
 *
 * Time T2 (come back as master or slave)
 *
 *                 <------------    Reapply Log Entries     -------------->
 *       C    C    C    C    C    C    C    C    C    C    C    .    .    .
 *            ^                        ^
 *            |                        |
 *    LargestCommittedEntryOnSnapshot  SnapshotPoint
 *
 *
 * <pre>
 * SnapshotEntry := {
 *     Snapshot:                                 Serialized App Object upon the Moment when the Snapshot was Taken
 *     LargestCommittedEntryOnSnapshot:          index(LargestCommittedEntryOnSnapshot) upon the Moment when the Snapshot was Taken,
 *     IndexOfLargestReplicatedEntryOnSnapshot:  index(LargestReplicatedEntry) upon the Moment when the Snapshot was Taken
 *     LowestOffsetInLogFile:                    Math.min(offset(LargestCommittedEntryOnSnapshot), offset(LargestReplicatedEntryOnSnapshot)),
 * }
 *
 * // Recovery Procedure
 *
 * SnapshotEntry := locate the last snapshot entry from snapshot file
 * appObject = load(SnapshotEntry.Snapshot);
 * indexOfLargestCommittedEntry = SnapshotEntry.LargestCommittedEntryOnSnapshot
 * indexOfLargestReplicatedEntry = SnapshotEntry.IndexOfLargestReplicatedEntryOnSnapshot
 *
 * for each LogEntry e where offset(e) >= SnapshotEntry.LowestOffsetInLogFile :
 *     do loadLogEntry(e)
 *     if index(e) > SnapshotEntry.LargestCommittedEntryOnSnapshot :
 *         do apply(e)
 * </pre>
 *
 * @author terryz
 */
public class FileLogger implements ILogger<Entry> {
    private final long TERM = 0;
    private Logger logger = Logger.getLogger(FileLogger.class.getName());

    /**
     * Log Entry File Path
     */
    private String logFilePath;

    /**
     * Snapshot File Path
     */
    private String snapshotFilePath;

    /**
     * Index of the largest committed log entry. A log entry is
     * committed iff its request has been invoked on <b>master</b>.
     */
    private long indexOfLargestCommittedEntry;

    /**
     * Index of the largest replicated entry. A log entry is replicated
     * iff its request has been invoked on <b>master</b> and the log
     * entry has been replicated to slave.
     */
    private long indexOfLargestReplicatedEntry;

    /**
     * Index of the largest entry.
     */
    private long indexOfLargestEntry;

    /**
     * Map that tracks the mapping from log index to its offset in the file
     */
    private Map<Long, Long> indexOffsetMap;

    // TODO (Terry): truncate the list and the log file periodically
    // A normal log entry with 5 Date objects as params takes about 800 bytes.
    // Suppose we serve 1000 operations per second, and we checkpoint every 15 minutes,
    // we expect the size of the List is around 1GB.
    private List<Entry> logEntries;

    /**
     * File for storing log entries
     * TODO (Terry): We need to define disk pages
     */
    private FileChannel logChannel;

    /**
     * File for storing snapshots
     */
    private FileChannel snapshotChannel;

    public FileLogger(String logFilePath, String snapshotFilePath) throws Exception {
        if (logFilePath == null || logFilePath.isEmpty()) {
            throw new IllegalArgumentException("log file path not specified");
        }

        if (snapshotFilePath == null || snapshotFilePath.isEmpty()) {
            throw new IllegalArgumentException("snapshot file path not specified");
        }

        this.logFilePath = logFilePath;
        this.snapshotFilePath = snapshotFilePath;
        this.indexOfLargestEntry = 0L;
        this.logEntries = new ArrayList<Entry>();
        this.indexOffsetMap = new HashMap<Long, Long>();

        FileOutputStream logOS = new FileOutputStream(logFilePath);
        this.logChannel = logOS.getChannel();

        FileOutputStream snapOS = new FileOutputStream(snapshotFilePath);
        this.snapshotChannel = snapOS.getChannel();
    }

    @Override
    public long append(Entry entry) throws Exception {
        entry.setTerm(TERM).setIndex(getNextIndex());

        long entryOffset = 0L;
        try {
            entryOffset = write(this.logChannel, Util.toBytes(entry));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "failed to write log entry {0} into file: {1}", new Object[]{entry, e});
            throw e;
        }

        indexOffsetMap.put(entry.getIndex(), entryOffset);
        logEntries.add(entry);
        return entryOffset;
    }

    @Override
    public void markReplicated(Entry logEntry) {
        if (! indexOffsetMap.containsKey(logEntry.getIndex())) {
            throw new IllegalStateException(String.format("cannot find log entry %s in indexOffsetMap", logEntry));
        }

        final long index = logEntry.getIndex();
        if (index <= indexOfLargestReplicatedEntry) {
            throw new IllegalStateException(String.format("index of log entry %s is %s which is less than indexOfLargestReplicatedEntry %s", logEntry, index, indexOfLargestReplicatedEntry));
        }

        this.indexOfLargestReplicatedEntry = index;
    }

    @Override
    public void markApplied(Entry logEntry) {
        if (! indexOffsetMap.containsKey(logEntry.getIndex())) {
            throw new IllegalStateException(String.format("cannot find log entry %s in indexOffsetMap", logEntry));
        }

        final long index = logEntry.getIndex();
        if (index <= indexOfLargestCommittedEntry) {
            throw new IllegalStateException(String.format("index of log entry %s is %s which is less than indexOfLargestCommittedEntry %s", logEntry, index, indexOfLargestCommittedEntry));
        }

        this.indexOfLargestCommittedEntry = index;
    }

    @Override
    public long takeSnapshot(Object appObject) throws Exception {
        final long lowestOffset = Math.min(indexOffsetMap.get(indexOfLargestCommittedEntry), indexOffsetMap.get(indexOfLargestReplicatedEntry));
        SnapshotEntry entry = SnapshotEntry.newBuilder().term(TERM).index(getNextIndex())
                .logFilePath(this.logFilePath)
                .snapshotFilePath(this.snapshotFilePath)
                .appObject(appObject)
                .lowestOffsetInLogFile(lowestOffset)
                .indexOfLargestCommittedEntry(this.indexOfLargestCommittedEntry)
                .indexOfLargestReplicatedEntry(this.indexOfLargestReplicatedEntry)
                .build();

        long snapshotOffset = 0L;
        try {
            snapshotOffset = write(this.snapshotChannel, Util.toBytes(entry));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "failed to write snapshot entry {0} into file: {1}", new Object[]{entry, e});
            throw e;
        }

        indexOffsetMap.put(entry.getIndex(), snapshotOffset);
        return snapshotOffset;
    }

    public static FileLogger restoreFromSnapshot() throws Exception {
        // TODO (Terry): retrieve latest snapshot entry from file
        SnapshotEntry entry = null;
        FileLogger fileLogger = new FileLogger(entry.getLogFilePath(), entry.getSnapshotFilePath());
        fileLogger.indexOfLargestCommittedEntry = entry.getIndexOfLargestCommittedEntry();
        fileLogger.indexOfLargestReplicatedEntry = entry.getIndexOfLargestReplicatedEntry();
        long lowestOffset = entry.getLowestOffsetInLogFile();

        FileChannel logChannel = fileLogger.logChannel;
        logChannel.position(lowestOffset);

        return fileLogger;
    }

    public long getIndexOfLargestReplicatedEntry() {
        return this.indexOfLargestReplicatedEntry;
    }

    // TODO (Terry): Replace logEntries with Map
    @Override
    public boolean indexExists(long index) {
        return this.indexOffsetMap.containsKey(index);
    }

    private long getNextIndex() {
        return ++indexOfLargestEntry;
    }

    /**
     * Writes byte array into the given file channel.
     * Each record is in the format of <record_length> followed by <record_content>.
     *
     * @param dest destination file channel
     * @param bytes
     * @return the offset of the record
     * @throws Exception
     */
    private long write(FileChannel dest, byte[] bytes) throws Exception {
        final long offset = dest.position();

        ByteBuffer bytebuffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE + bytes.length);
        // size of content
        bytebuffer.putInt(bytes.length);
        // actual content
        bytebuffer.put(bytes);

        dest.write(bytebuffer);
        dest.force(false);

        return offset;
    }

    private int readInt(FileChannel src) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        src.read(byteBuffer);
        return byteBuffer.getInt();
    }

    private byte[] readBytes(FileChannel src, long length) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        src.read(byteBuffer);
        return byteBuffer.array();
    }

    /**
     * @returns a copy of {@link #indexOffsetMap}
     */
    public Map<Long, Long> getIndexOffsetMap() {
        return new HashMap<Long, Long>(indexOffsetMap);
    }

    public void finalize() {
        if (logChannel != null) {
            try {
                logChannel.close();
            } catch (Exception e) {
                logger.log(Level.FINE, "failed to close log file: {0}", e);
            }
        }

        if (snapshotChannel != null) {
            try {
                snapshotChannel.close();
            } catch (Exception e) {
                logger.log(Level.FINE, "failed to close snapshot file: {0}", e);
            }
        }
    }
}
package sapphire.policy.scalability;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;
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
public class FileLogger implements ILogger<LogEntry> {
    private final long TERM = 0;
    private Logger logger = Logger.getLogger(FileLogger.class.getName());

    /**
     * Log Entry File Path
     */
    private File entryLog;

    /**
     * Snapshot File Path
     */
    private File snapshotLog;

    /**
     * Entry log output channel
     */
    private FileChannel entryLogOutChannel;

    /**
     * Entry log input channel
     */
    private FileChannel entryLogInChannel;

    /**
     * Snapshot log output channel
     */
    private FileChannel snapshotLogOutChannel;

    /**
     * Snapshot log in channel
     */
    private FileChannel snapshotLogInChannel;

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
    private AtomicLong indexOfLargestEntry;

    /**
     * Map that tracks the mapping from log index to its offset in the file
     */
    private Map<Long, Long> indexOffsetMap;

    // TODO (Terry): truncate the list and the log file periodically
    // A normal log entry with 5 Date objects as params takes about 800 bytes.
    // Suppose we serve 1000 operations per second, and we checkpoint every 15 minutes,
    // we expect the size of the List is around 1GB.
    private List<LogEntry> logEntries;

    /**
     * Constructor
     *
     * @param logFilePath
     * @param snapshotFilePath
     * @throws Exception
     */
    public FileLogger(String logFilePath, String snapshotFilePath) throws Exception {
        if (logFilePath == null || logFilePath.isEmpty()) {
            throw new IllegalArgumentException("log file path not specified");
        }

        if (snapshotFilePath == null || snapshotFilePath.isEmpty()) {
            throw new IllegalArgumentException("snapshot file path not specified");
        }

        this.entryLog = new File(logFilePath);
        this.snapshotLog = new File(snapshotFilePath);
        this.indexOfLargestCommittedEntry = 0L;
        this.indexOfLargestReplicatedEntry = 0L;
        this.indexOfLargestEntry = new AtomicLong();

        this.snapshotLogOutChannel = new FileOutputStream(snapshotLog).getChannel();
        this.snapshotLogInChannel = new FileInputStream(snapshotLog).getChannel();

        this.entryLogOutChannel = new FileOutputStream(entryLog).getChannel();
        this.entryLogInChannel = new FileInputStream(entryLog).getChannel();

        this.logEntries = new ArrayList<LogEntry>();
        this.indexOffsetMap = new HashMap<Long, Long>();

        // TODO (Terry): Fix errors and clean up codes
        if (snapshotLog.exists()) {
            SnapshotEntry snapshotEntry = getLatestSnapshotEntry(snapshotLogOutChannel);
            if (snapshotEntry != null) {
                this.indexOfLargestReplicatedEntry = snapshotEntry.getIndexOfLargestReplicatedEntry();
                this.indexOfLargestCommittedEntry = snapshotEntry.getIndexOfLargestCommittedEntry();
                long offset = snapshotEntry.getLowestOffsetInLogFile();
                entryLogInChannel.position(offset);

                int bytesRead = readInt(entryLogInChannel);
                while (bytesRead > 0) {
                    long position = entryLogInChannel.position();
                    byte[] bytes = readBytes(entryLogInChannel, bytesRead);

                    LogEntry entry = (LogEntry)Util.toObject(bytes);
                    indexOffsetMap.put(entry.getIndex(), position);
                    logEntries.add(entry);
                    indexOfLargestEntry.set(Math.max(indexOfLargestEntry.get(), entry.getIndex()));

                    bytesRead = readInt(entryLogInChannel);
                }
            }
        }
    }

    @Override
    public synchronized long append(LogEntry entry) throws Exception {
        entry.setTerm(TERM).setIndex(getNextIndex());

        try {
            long entryOffset = writeLog(Util.toBytes(entry));
            indexOffsetMap.put(entry.getIndex(), entryOffset);
            logEntries.add(entry);
            return entryOffset;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "failed to writeLog log entry {0} into file: {1}", new Object[]{entry, e});
            throw e;
        }
    }

    @Override
    public synchronized LogEntry read(long offset) throws Exception {
        entryLogInChannel.position(offset);
        int len = readInt(entryLogInChannel);
        byte[] bytes = readBytes(entryLogInChannel, len);
        return (LogEntry)Util.toObject(bytes);
    }

    @Override
    public synchronized void markReplicated(LogEntry logEntry) {
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
    public synchronized void markReplicated(long largestReplicatedIndex) {
        if (largestReplicatedIndex <= indexOfLargestReplicatedEntry) {
            throw new IllegalStateException(String.format("largestReplicatedIndex %s is less than indexOfLargestReplicatedEntry %s", largestReplicatedIndex, indexOfLargestReplicatedEntry));
        }

        this.indexOfLargestReplicatedEntry = largestReplicatedIndex;
    }


    @Override
    public synchronized void markCommitted(LogEntry logEntry) {
        if (! indexOffsetMap.containsKey(logEntry.getIndex())) {
            throw new IllegalStateException(String.format("cannot find log entry %s in indexOffsetMap", logEntry));
        }

        final long index = logEntry.getIndex();
        if (index <= indexOfLargestCommittedEntry) {
            throw new IllegalStateException(String.format("index of log entry %s is %s which is less than indexOfLargestCommittedEntry %s", logEntry, index, indexOfLargestCommittedEntry));
        }

        this.indexOfLargestCommittedEntry = index;
    }

    // TODO (Terry): caller should lock down AppObject during snapshot
    @Override
    public synchronized long takeSnapshot(Object appObject) throws Exception {
        final long lowestOffset = Math.min(indexOffsetMap.get(indexOfLargestCommittedEntry), indexOffsetMap.get(indexOfLargestReplicatedEntry));
        SnapshotEntry entry = SnapshotEntry.newBuilder().term(TERM).index(getNextIndex())
                .logFilePath(this.entryLog.getAbsolutePath())
                .snapshotFilePath(this.snapshotLog.getAbsolutePath())
                .appObject(appObject)
                .lowestOffsetInLogFile(lowestOffset)
                .indexOfLargestCommittedEntry(this.indexOfLargestCommittedEntry)
                .indexOfLargestReplicatedEntry(this.indexOfLargestReplicatedEntry)
                .build();

        try {
            return writeSnaphot(Util.toBytes(entry));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "failed to writeLog snapshot entry {0} into file: {1}", new Object[]{entry, e});
            throw e;
        }
    }

    @Override
    public boolean indexExists(long index) {
        return this.indexOffsetMap.containsKey(index);
    }

    private SnapshotEntry getLatestSnapshotEntry(FileChannel snapshotChannel) {
        // TODO (Terry): implement getLatestSnapshotEntry
        return null;
    }

    @Override
    public synchronized long getIndexOfLargestReplicatedEntry() {
        return this.indexOfLargestReplicatedEntry;
    }

    public synchronized List<LogEntry> getUnreplicatedEntries() {
        long indexOfLargestReplicatedEntry = getIndexOfLargestReplicatedEntry();

        Stack<LogEntry> stack = new Stack<LogEntry>();
        for (int i=logEntries.size()-1; i>=0; i--) {
            if (logEntries.get(i).getIndex() > indexOfLargestReplicatedEntry) {
                stack.push(logEntries.get(i));
            }
        }

        List<LogEntry> result = new ArrayList<LogEntry>();
        while (!stack.empty()) {
            result.add(stack.pop());
        }

        return result;
    }

    private long getNextIndex() {
        return indexOfLargestEntry.getAndIncrement();
    }

    /**
     * Writes byte array into the given file channel.
     * Each record is in the format of <record_length> followed by <record_content>.
     *
     * @param bytes
     * @return the offset of the record
     * @throws Exception
     */
    private long writeLog(byte[] bytes) throws Exception {
        final long offset = entryLogOutChannel.position();

        ByteBuffer bytebuffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE + bytes.length);
        // size of content
        bytebuffer.putInt(bytes.length);
        // actual content
        bytebuffer.put(bytes);
        bytebuffer.flip();

        entryLogOutChannel.write(bytebuffer);
        entryLogOutChannel.force(false);

        return offset;
    }

    // TODO (Terry): format <record><record_length><record_offset>
    private long writeSnaphot(byte[] bytes) throws Exception {
        final long offset = snapshotLogOutChannel.position();

        ByteBuffer bytebuffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE + bytes.length);
        // size of content
        bytebuffer.putInt(bytes.length);
        // actual content
        bytebuffer.put(bytes);
        bytebuffer.flip();

        snapshotLogOutChannel.write(bytebuffer);
        snapshotLogOutChannel.force(false);

        return offset;
    }

    private int readInt(FileChannel src) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        src.read(byteBuffer);
        byteBuffer.flip();
        return byteBuffer.getInt();
    }

    private byte[] readBytes(FileChannel src, int length) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate(length);
        src.read(byteBuffer);
        byteBuffer.flip();
        return byteBuffer.array();
    }

    public void finalize() {
        if (entryLogOutChannel != null) {
            try {
                entryLogOutChannel.close();
            } catch (Exception e) {
                logger.log(Level.FINE, "failed to close log file: {0}", e);
            }
        }

        if (entryLogInChannel != null) {
            try {
                entryLogInChannel.close();
            } catch (Exception e) {
                logger.log(Level.FINE, "failed to close log file: {0}", e);
            }
        }

        if (snapshotLogOutChannel != null) {
            try {
                snapshotLogOutChannel.close();
            } catch (Exception e) {
                logger.log(Level.FINE, "failed to close snapshot file: {0}", e);
            }
        }

        if (snapshotLogInChannel != null) {
            try {
                snapshotLogInChannel.close();
            } catch (Exception e) {
                logger.log(Level.FINE, "failed to close snapshot file: {0}", e);
            }
        }
    }
}
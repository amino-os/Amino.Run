package sapphire.policy.scalability;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import sapphire.common.Utils;
import sapphire.runtime.MethodInvocationRequest;
import sun.rmi.runtime.Log;

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
    private final Logger logger = Logger.getLogger(FileLogger.class.getName());

    /**
     * Log Entry File Path
     */
    private final File entryLog;

    /**
     * Snapshot File Path
     */
    private final File snapshotLog;

    /**
     * Entry log output channel
     */
    private final FileChannel entryLogOutChannel;

    /**
     * Entry log input channel
     */
    private final FileChannel entryLogInChannel;

    /**
     * Snapshot log output channel
     */
    private final FileChannel snapshotLogOutChannel;

    /**
     * Snapshot log in channel
     */
    private final FileChannel snapshotLogInChannel;

    /**
     * A thread used to apply commits
     */
    private final CommitExecutor commitExecutor;

    /**
     * Index of the largest replicated entry. On master, a log entry is
     * replicated iff its request has been replicated to slave.
     * On slave, a log entry is replicated iff the request has been
     * received and added into log file.
     */
    private long indexOfLargestReplicatedEntry;

    /**
     * Index of the largest entry.
     */
    private final AtomicLong indexOfLargestEntry;

    /**
     * Map that tracks the mapping from log index to its offset in the file
     */
    private final Map<Long, Long> indexOffsetMap;

    // A normal log entry with 5 Date objects as params takes about 800 bytes.
    // Suppose we serve 1000 operations per second, and we checkpoint every 15 minutes,
    // we expect the size of the List is around 1GB.
    private final List<LogEntry> logEntries;

    private final SequenceGenerator sequenceGenerator;

    private final ScheduledExecutorService cleanExecutor;

    /**
     * Constructor
     *
     * @param config
     * @param loadSnapshot whether or not to load existing snapshot file
     * @throws Exception
     */
    public FileLogger(Configuration config, CommitExecutor commitExecutor, boolean loadSnapshot) throws Exception {
        String logFilePath = config.getLogFilePath();
        String snapshotFilePath = config.getSnapshotFilePath();

        if (logFilePath == null || logFilePath.isEmpty()) {
            throw new IllegalArgumentException("log file path not specified");
        }

        if (snapshotFilePath == null || snapshotFilePath.isEmpty()) {
            throw new IllegalArgumentException("snapshot file path not specified");
        }

        this.entryLog = new File(logFilePath);
        this.snapshotLog = new File(snapshotFilePath);
        this.commitExecutor = commitExecutor;

        this.indexOfLargestReplicatedEntry = 0L;
        this.indexOfLargestEntry = new AtomicLong(-1);
        this.sequenceGenerator = SequenceGenerator.newBuilder().build();

        this.entryLogOutChannel = new FileOutputStream(entryLog, true).getChannel();
        this.entryLogInChannel = new FileInputStream(entryLog).getChannel();

        this.logEntries = new ArrayList<LogEntry>();
        this.indexOffsetMap = new ConcurrentHashMap<Long, Long>();

        if (loadSnapshot && snapshotLog.exists()) {
            this.snapshotLogOutChannel = new FileOutputStream(snapshotLog, true).getChannel();
            this.snapshotLogInChannel = new FileInputStream(snapshotLog).getChannel();

            SnapshotEntry snapshotEntry = readSnapshot();
            if (snapshotEntry != null) {
                this.indexOfLargestReplicatedEntry = snapshotEntry.getIndexOfLargestReplicatedEntry();
                this.commitExecutor.setIndexOfLargestCommittedEntry(snapshotEntry.getIndexOfLargestCommittedEntry());
                long offset = snapshotEntry.getLowestOffsetInLogFile();

                load(offset);
            }
        } else {
            this.snapshotLogOutChannel = new FileOutputStream(snapshotLog, true).getChannel();
            this.snapshotLogInChannel = new FileInputStream(snapshotLog).getChannel();

            takeSnapshot();
        }

        this.cleanExecutor = Executors.newSingleThreadScheduledExecutor();
        cleanExecutor.schedule(clean(), config.getSnapshotIntervalInMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized long append(LogEntry entry) throws Exception {
        LogEntry seenEntry = isRequestHandled(entry.getRequest(), this.logEntries);
        if (seenEntry != null) {
            return indexOffsetMap.get(seenEntry.getIndex());
        }

        entry.setTerm(TERM).setIndex(getNextIndex());

        try {
            long entryOffset = writeLog(Utils.toBytes(entry));
            updateInMemoryStructures(entry, entryOffset);

            logger.log(Level.FINE, "successfully appended entry {0} in log file", entry);
            return entryOffset;
        } catch (Exception e) {
            logger.log(Level.SEVERE, String.format("failed to append entry %s in log file: %s",entry, e), e);
            throw e;
        }
    }

    @Override
    public synchronized LogEntry read(long offset) throws Exception {
        entryLogInChannel.position(offset);
        int len = readInt(entryLogInChannel);
        if (len == -1) {
            return null;
        }

        byte[] bytes = readBytes(entryLogInChannel, len);
        return (LogEntry)Utils.toObject(bytes);
    }

    @Override
    public synchronized void load(long offset) throws Exception {
        entryLogInChannel.position(offset);
        int bytesToRead = readInt(entryLogInChannel);
        while (bytesToRead > 0) {
            long position = entryLogInChannel.position();
            byte[] bytes = readBytes(entryLogInChannel, bytesToRead);

            LogEntry entry = (LogEntry)Utils.toObject(bytes);
            updateInMemoryStructures(entry, position);

            bytesToRead = readInt(entryLogInChannel);
        }
    }

    @Override
    public synchronized void markReplicated(long largestReplicatedIndex) {
        if (largestReplicatedIndex <= indexOfLargestReplicatedEntry) {
            throw new IllegalStateException(String.format("largestReplicatedIndex %s is less than indexOfLargestReplicatedEntry %s",
                    largestReplicatedIndex, indexOfLargestReplicatedEntry));
        }

        this.indexOfLargestReplicatedEntry = largestReplicatedIndex;
    }

    @Override
    public synchronized SnapshotEntry takeSnapshot() {
        // find the lowest offset between largest committed entry and largest replicated entry
        long indexOfLargestCommittedEntry = getIndexOfLargestCommittedEntry();
        long lowestOffset = Math.min(
                indexOffsetMap.get(indexOfLargestCommittedEntry)  != null ? indexOffsetMap.get(indexOfLargestCommittedEntry)  : 0,
                indexOffsetMap.get(indexOfLargestReplicatedEntry) != null ? indexOffsetMap.get(indexOfLargestReplicatedEntry) : 0);

        try {
            SnapshotEntry entry = commitExecutor.updateSnapshot(
                SnapshotEntry.newBuilder()
                    .term(TERM).index(getNextIndex())
                    .logFilePath(this.entryLog.getAbsolutePath())
                    .snapshotFilePath(this.snapshotLog.getAbsolutePath())
                    .lowestOffsetInLogFile(lowestOffset)
                    .indexOfLargestReplicatedEntry(this.indexOfLargestReplicatedEntry)
                    .indexOfLargestCommittedEntry(indexOfLargestCommittedEntry)
                    .build());

            writeSnaphot(Utils.toBytes(entry));

            return entry;
        } catch (Exception e) {
            logger.log(Level.SEVERE, String.format("failed to write snapshot entry into log file: %s", e), e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean indexExists(long index) {
        return this.indexOffsetMap.containsKey(index);
    }

    @Override
    public synchronized long getIndexOfLargestReplicatedEntry() {
        return this.indexOfLargestReplicatedEntry;
    }

    @Override
    public synchronized long getIndexOfLargestCommittedEntry() {
        return this.commitExecutor.getIndexOfLargestCommittedEntry();
    }

    /**
     * Index of the largest entry received on a slave.
     *
     * Only slave node has largest received entry.
     *
     * @return the index of the largest received entry
     */
    @Override
    public synchronized long getIndexOfLargestReceivedEntry() {
        return indexOfLargestEntry.get();
    }

    @Override
    public synchronized List<LogEntry> getUnreplicatedEntries() {
        return filterEntriesByIndex(logEntries, getIndexOfLargestReplicatedEntry());
    }

    @Override
    public synchronized List<LogEntry> getUncomittedEntries() {
        return filterEntriesByIndex(logEntries, getIndexOfLargestCommittedEntry());
    }

    // TODO (Terry): need to make check faster
    /**
     * Checks if the request exists in the given {@code logEntries}.
     * @param request
     * @param logEntries
     * return {@link LogEntry} if the request exists in the given {@code logEntries};
     * {@code false} otherwise.
     */
    private LogEntry isRequestHandled(MethodInvocationRequest request, List<LogEntry> logEntries) {
        for (LogEntry en : logEntries) {
            if (en.getRequest().equals(request)) {
                return en;
            }
        }
        return null;
    }

    private Runnable clean() {
        return new Runnable() {
            @Override
            public void run() {
                SnapshotEntry entry = takeSnapshot();
                long indexOfLowerEntry = Math.min(entry.getIndexOfLargestCommittedEntry(),
                                                  entry.getIndexOfLargestReplicatedEntry());

                for (Iterator<LogEntry> iter = logEntries.listIterator(); iter.hasNext(); ) {
                    LogEntry en = iter.next();
                    if (en.getIndex() < indexOfLowerEntry) {
                        iter.remove();
                        indexOffsetMap.remove(en.getIndex());
                    }
                }
            }
        };
    }
    /**
     * Returns log entries whose indices are greater than the specified index
     * @param logEntries a list of log entries
     * @param index
     * @return log entries whose indices are greater than the specified index
     */
    private List<LogEntry> filterEntriesByIndex(List<LogEntry> logEntries, long index) {
        Stack<LogEntry> stack = new Stack<LogEntry>();
        for (int i=logEntries.size()-1; i>=0; i--) {
            if (logEntries.get(i).getIndex() <= index) {
                break;
            }
            stack.push(logEntries.get(i));
        }

        List<LogEntry> list = new LinkedList<LogEntry>();
        while (! stack.empty()) {
            list.add(stack.pop());
        }
        return list;
    }

    private long getNextIndex() {
        return sequenceGenerator.getNextSequence();
    }

    private synchronized void updateInMemoryStructures(LogEntry entry, long entryOffset) {
        if (entry.getIndex() <= indexOfLargestEntry.get()) {
            return;
        }

        Long offset = indexOffsetMap.putIfAbsent(entry.getIndex(), entryOffset);
        if (offset != null && offset != entryOffset) {
            throw new AssertionError(String.format("log entry %s already exists in indexOffsetMap with a different offset %s", entry, offset));
        }

        logEntries.add(entry);
        indexOfLargestEntry.set(entry.getIndex());

        if (indexOffsetMap.size() != logEntries.size()) {
            throw new AssertionError(String.format("the size of indexOffsetMap %s is different from the size of logEntries %s", indexOffsetMap.size(), logEntries.size()));
        }
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
        bytebuffer.putInt(bytes.length);
        bytebuffer.put(bytes);
        bytebuffer.flip();

        entryLogOutChannel.write(bytebuffer);
        entryLogOutChannel.force(false);

        return offset;
    }

    /**
     * Writes the specified byte array in snapshot log file
     *
     * @param bytes
     * @return the offset
     * @throws Exception
     */
    private long writeSnaphot(byte[] bytes) throws Exception {
        final long offset = snapshotLogOutChannel.position();

        ByteBuffer bytebuffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE + Long.SIZE/Byte.SIZE + bytes.length);
        bytebuffer.put(bytes);
        bytebuffer.putInt(bytes.length);
        bytebuffer.putLong(offset);
        bytebuffer.flip();

        snapshotLogOutChannel.write(bytebuffer);
        snapshotLogOutChannel.force(false);

        return offset;
    }

    public SnapshotEntry readSnapshot() throws IOException, ClassNotFoundException {
        long fileSize = snapshotLogInChannel.size();
        snapshotLogInChannel.position(fileSize - Integer.SIZE/Byte.SIZE - Long.SIZE/Byte.SIZE);
        int recordSize = readInt(snapshotLogInChannel);
        long offset = readInt(snapshotLogInChannel);
        snapshotLogInChannel.position(offset);
        byte[] bytes = readBytes(snapshotLogInChannel, recordSize);
        return (SnapshotEntry) Utils.toObject(bytes);
    }

    /**
     * Reads an integer from the source file channel and returns it.
     *
     *
     * @param src the file channel from which to read
     * @return the integer or -1 if the channel has reached end-of-stream
     * @throws Exception
     */
    private int readInt(FileChannel src) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        int byteCnt = src.read(byteBuffer);
        if (byteCnt == -1) {
            return -1;
        }

        byteBuffer.flip();
        return byteBuffer.getInt();
    }

    private long readLong(FileChannel src) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE);
        int byteCnt = src.read(byteBuffer);
        if (byteCnt == -1) {
            return -1;
        }

        byteBuffer.flip();
        return byteBuffer.getLong();
    }

    /**
     * Reads a sequence of bytes from this channel
     * @param src the file chanel from which to read
     * @param length the number of bytes to read
     * @return a byte array or <code>null</code> if the channel has reached end-of-stream
     * @throws Exception
     */
    private byte[] readBytes(FileChannel src, int length) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(length);
        int byteCnt = src.read(byteBuffer);
        if (byteCnt == -1) {
            return null;
        }

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

    @Override
    public void close() throws IOException {
        finalize();
    }
}
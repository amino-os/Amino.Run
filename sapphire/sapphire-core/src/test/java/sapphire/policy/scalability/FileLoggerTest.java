package sapphire.policy.scalability;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sapphire.common.AppObject;
import sapphire.runtime.MethodInvocationRequest;

/**
 * @author terryz
 */
public class FileLoggerTest {
    private File entryLogFile;
    private File snapshotLogFile;
    private CommitExecutor commitExecutor = new CommitExecutor(new AppObject("object"));

    @Before
    public void setup() throws Exception {
        entryLogFile = File.createTempFile("file", "");
        snapshotLogFile = File.createTempFile("snapshot", "");
    }

    @Test
    public void testEntryLogReadWrite() throws Exception {
        FileLogger entryLogger =  new FileLogger(entryLogFile.getPath(), snapshotLogFile.getPath(), commitExecutor, false);

        // write message to entry log
        String message = "hello world";
        invokePrivateMethod(entryLogger, "writeLog", new Object[]{Util.toBytes(message)});

        // read message out of entry log
        FileChannel fc = (FileChannel)getField(entryLogger, "entryLogInChannel");
        int length = (Integer)invokePrivateMethod(entryLogger, "readInt", new Object[]{fc});
        byte[] content = (byte[])invokePrivateMethod(entryLogger, "readBytes", new Object[]{fc, length});
        Assert.assertEquals(message, Util.toObject(content));
    }

    @Test
    public void testSingleAppendAndRead() throws Exception {
        FileLogger entryLogger =  new FileLogger(entryLogFile.getPath(), snapshotLogFile.getPath(), commitExecutor, false);

        LogEntry expected = createLogEntry(0, "m1");
        long offset = entryLogger.append(expected);
        LogEntry actual = entryLogger.read(offset);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testMultipleAppendAndRead() throws Exception {
        FileLogger entryLogger =  new FileLogger(entryLogFile.getPath(), snapshotLogFile.getPath(), commitExecutor, false);

        Map<Long, LogEntry> map = new HashMap<Long, LogEntry>();
        for (int i=0; i<10; i++) {
            LogEntry entry = createLogEntry(i, "m"+i);
            long offset = entryLogger.append(entry);
            map.put(offset, entry);
        }

        for (Map.Entry<Long, LogEntry> en : map.entrySet()) {
            long offset = en.getKey();
            LogEntry expected = en.getValue();
            Assert.assertEquals(expected, entryLogger.read(offset));
        }
    }

    @Test
    public void testLoad() throws Exception {
        File entryLogFile = File.createTempFile("file", "");
        File snapshotLogFile = File.createTempFile("snapshot", "");
        FileLogger logger1 = new FileLogger(entryLogFile.getPath(), snapshotLogFile.getPath(), commitExecutor, false);

        Map<Long, LogEntry> map = new HashMap<Long, LogEntry>();
        for (int i=0; i<10; i++) {
            LogEntry entry = createLogEntry(i, "m"+i);
            long offset = logger1.append(entry);
            map.put(offset, entry);
        }
        logger1.close();

        FileLogger logger2 = new FileLogger(entryLogFile.getPath(), snapshotLogFile.getPath(), commitExecutor, false);
        logger2.load(0L);
        List<LogEntry> logEntries = (List<LogEntry >)getField(logger2, "logEntries");
        Map<Long, Long> indexOffsetMap = (Map<Long, Long>)getField(logger2, "indexOffsetMap");
        Assert.assertEquals(logEntries.size(), indexOffsetMap.size());
    }

    @Test
    public void testWriteReadSnapshot() throws Exception {
        FileLogger entryLogger =  new FileLogger(entryLogFile.getPath(), snapshotLogFile.getPath(), commitExecutor, false);

        Map<Long, LogEntry> map = new HashMap<Long, LogEntry>();
        for (int i=0; i<5; i++) {
            LogEntry entry = createLogEntry(i, "m"+i);
            long offset = entryLogger.append(entry);
            map.put(offset, entry);
        }

        SnapshotEntry expected = entryLogger.takeSnapshot();
        SnapshotEntry actual = entryLogger.readSnapshot();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFilterEntriesByIndex() throws Exception {
        FileLogger entryLogger =  new FileLogger(entryLogFile.getPath(), snapshotLogFile.getPath(), commitExecutor, false);
        final int indexOfLargestCommittedEntry = 5;

        List<LogEntry> entries = createLogEntries(10);
        List<LogEntry> actual = (List<LogEntry>)invokePrivateMethod(entryLogger,
                "filterEntriesByIndex",
                new Object[] {entries, indexOfLargestCommittedEntry});

        Assert.assertEquals(entries.size()-1-indexOfLargestCommittedEntry, actual.size());

        for (int i=actual.size()-1, j=entries.size()-1; i>=0; i--, j--) {
            Assert.assertEquals(entries.get(j), actual.get(i));
        }
    }

    private FileLogger createFileLogger(File entryLogFile, File snapshotLogFile) throws Exception {
        return new FileLogger(entryLogFile.getPath(), snapshotLogFile.getPath(), commitExecutor, false);
    }

    private List<LogEntry> createLogEntries(int entryCnt) {
        List<LogEntry> entries = new ArrayList<LogEntry>();
        for (int i=0; i<entryCnt; i++) {
            entries.add(createLogEntry(i, "m"+i));
        }
        return entries;
    }

    private LogEntry createLogEntry(long index, String methodName) {
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(new Date(System.currentTimeMillis()));

        MethodInvocationRequest request = MethodInvocationRequest.newBuilder()
                .methodType(MethodInvocationRequest.MethodType.WRITE)
                .methodName(methodName)
                .params(params)
                .build();
        return LogEntry.newBuilder()
                .term(0)
                .index(index)
                .request(request)
                .build();
    }

    private Object invokePrivateMethod(Object callee, String methodName, Object[] params) throws Exception {
        Class[] argTyps = new Class[params.length];
        for (int i=0; i< params.length; i++) {
            argTyps[i] = params.getClass();
        }

        Method[] methods = callee.getClass().getDeclaredMethods();
        Method method = null;
        for (Method m : methods) {
            if (m.getName().equals(methodName)) {
                method = m;
                break;
            }
        }

        method.setAccessible(true);
        return method.invoke(callee, params);
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field f = obj.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(obj);
    }
}

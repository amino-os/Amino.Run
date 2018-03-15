package sapphire.policy.scalability;

import org.junit.After;
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
import sapphire.common.Utils;
import sapphire.runtime.MethodInvocationRequest;

/**
 * @author terryz
 */
public class FileLoggerTest {
    private File entryLogFile;
    private File snapshotLogFile;
    private String clientId;
    private Configuration config;
    private CommitExecutor commitExecutor;

    @Before
    public void setup() throws Exception {
        entryLogFile = File.createTempFile("file", "");
        entryLogFile.deleteOnExit();
        snapshotLogFile = File.createTempFile("snapshot", "");
        snapshotLogFile.deleteOnExit();
        config = Configuration.newBuilder()
                .logFilePath(entryLogFile.getPath())
                .snapshotFilePath(snapshotLogFile.getPath())
                .build();
        clientId = "clientId";
        commitExecutor = CommitExecutor.getInstance(new AppObject("object"), 0L, config);
    }

    @After
    public void tearDown() throws Exception {
        entryLogFile.delete();
        snapshotLogFile.delete();
    }

    @Test
    public void testEntryLogReadWrite() throws Exception {
        FileLogger entryLogger =  new FileLogger(config, commitExecutor, false);

        // write message to entry log
        String message = "hello world";
        invokePrivateMethod(entryLogger, "writeLog", new Object[]{Utils.toBytes(message)});

        // read message out of entry log
        FileChannel fc = (FileChannel)getField(entryLogger, "entryLogInChannel");
        int length = (Integer)invokePrivateMethod(entryLogger, "readInt", new Object[]{fc});
        byte[] content = (byte[])invokePrivateMethod(entryLogger, "readBytes", new Object[]{fc, length});
        Assert.assertEquals(message, Utils.toObject(content));
    }

    @Test
    public void testSingleAppendAndRead() throws Exception {
        FileLogger entryLogger =  new FileLogger(config, commitExecutor, false);

        LogEntry expected = createLogEntry(0, "m1");
        long offset = entryLogger.append(expected);
        LogEntry actual = entryLogger.read(offset);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testMultipleAppendAndRead() throws Exception {
        FileLogger entryLogger =  new FileLogger(config, commitExecutor, false);

        Map<Long, LogEntry> map = new HashMap<Long, LogEntry>();
        for (int i=0; i<10; i++) {
            LogEntry entry = createLogEntry(i, "m"+i);
            long offset = entryLogger.append(entry);
            map.put(offset, entry);
        }

        for (Map.Entry<Long, LogEntry> en : map.entrySet()) {
            long offset = en.getKey();
            LogEntry expected = en.getValue();
            LogEntry actual = entryLogger.read(offset);
            Assert.assertEquals(expected, actual);
        }
    }

    @Test
    public void testLoad() throws Exception {
        FileLogger logger1 = new FileLogger(config, commitExecutor, false);

        Map<Long, LogEntry> map = new HashMap<Long, LogEntry>();
        for (int i=0; i<10; i++) {
            LogEntry entry = createLogEntry(i, "m"+i);
            long offset = logger1.append(entry);
            map.put(offset, entry);
        }
        logger1.close();

        FileLogger logger2 = new FileLogger(config, commitExecutor, false);
        logger2.load(0L);
        List<LogEntry> logEntries = (List<LogEntry >)getField(logger2, "logEntries");
        Map<Long, Long> indexOffsetMap = (Map<Long, Long>)getField(logger2, "indexOffsetMap");
        Assert.assertEquals(logEntries.size(), indexOffsetMap.size());
    }

    @Test
    public void testWriteReadSnapshot() throws Exception {
        FileLogger entryLogger =  new FileLogger(config, commitExecutor, false);

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
        FileLogger entryLogger =  new FileLogger(config, commitExecutor, false);
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

    @Test
    public void testIsRequestHandled() throws Exception {
        FileLogger entryLogger =  new FileLogger(config, commitExecutor, false);
        List<LogEntry> entries = createLogEntries(5);

        LogEntry actual = (LogEntry)invokePrivateMethod(entryLogger, "isRequestHandled", new Object[]{entries.get(0).getRequest(), entries});
        Assert.assertEquals(entries.get(0), actual);
        for (int i=1; i<entries.size(); i++) {
            Assert.assertNotEquals(entries.get(1), actual);
        }
    }

    @Test
    public void testRequestNotHandled() throws Exception {
        FileLogger entryLogger =  new FileLogger(config, commitExecutor, false);
        List<LogEntry> entries = createLogEntries(5);

        MethodInvocationRequest requestNotExists = MethodInvocationRequest.newBuilder()
                .clientId("client_not_exist")
                .requestId(100L)
                .methodType(MethodInvocationRequest.MethodType.MUTABLE)
                .methodName("methodName")
                .params(null)
                .build();

        LogEntry actual = (LogEntry)invokePrivateMethod(entryLogger, "isRequestHandled", new Object[]{requestNotExists, entries});
        Assert.assertNull(actual);
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
                .clientId(clientId)
                .requestId(index)
                .methodType(MethodInvocationRequest.MethodType.MUTABLE)
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

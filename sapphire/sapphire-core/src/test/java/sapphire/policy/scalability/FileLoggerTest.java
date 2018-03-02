package sapphire.policy.scalability;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import java.util.ArrayList;
import java.util.Date;

import sapphire.runtime.MethodInvocationRequest;

/**
 * @author terryz
 */
public class FileLoggerTest {
    @Test
    public void testObjectSize() throws Exception {
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(new Date(System.currentTimeMillis()));
        params.add(new Date(System.currentTimeMillis()));
        params.add(new Date(System.currentTimeMillis()));
        params.add(new Date(System.currentTimeMillis()));
        params.add(new Date(System.currentTimeMillis()));

        MethodInvocationRequest request = MethodInvocationRequest.newBuilder().methodName("method").params(params).methodType(MethodInvocationRequest.MethodType.READ).build();
        Object o = LogEntry.newBuilder().index(0).request(request).term(0).build();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(o);
        oos.close();
        byte[] content = os.toByteArray();
        System.out.println("size: " + content.length);

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(content));
        Object o2 = ois.readObject();
        System.out.println(o2);
    }

    @Test
    public void testEntryLogReadWrite() throws Exception {
        File entryLogFile = File.createTempFile("filelog", "");
        File snapshotLogFile = File.createTempFile("snapshot", "");

        FileLogger entryLogger = new FileLogger(entryLogFile.getPath(), snapshotLogFile.getPath());
        // write message to entry log
        String message = "hello world";
        invokePrivateMethod(entryLogger, "writeLog", new Object[]{Util.toBytes(message)});

        // read message out of entry log
        FileChannel fc = getChannel(entryLogger, "entryLogInChannel");
        int length = (Integer)invokePrivateMethod(entryLogger, "readInt", new Object[]{fc});
        byte[] content = (byte[])invokePrivateMethod(entryLogger, "readBytes", new Object[]{fc, length});
        Assert.assertEquals(message, Util.toObject(content));

        entryLogFile.delete();
        snapshotLogFile.delete();
    }

    @Test
    public void testAppendAndRead() throws Exception {
        File entryLogFile = File.createTempFile("filelog", "");
        File snapshotLogFile = File.createTempFile("snapshot", "");
        FileLogger entryLogger = new FileLogger(entryLogFile.getPath(), snapshotLogFile.getPath());

        ArrayList<Object> params = new ArrayList<Object>();
        params.add(new Date(System.currentTimeMillis()));

        MethodInvocationRequest request = MethodInvocationRequest.newBuilder()
                .methodType(MethodInvocationRequest.MethodType.WRITE)
                .methodName("invoke")
                .params(params)
                .build();
        LogEntry expected = LogEntry.newBuilder()
                .term(0)
                .index(0)
                .request(request)
                .build();

        long offset = entryLogger.append(expected);

        LogEntry actual = entryLogger.read(offset);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testInputFileChannelOutputFileChannel() throws Exception {
        String[] strs = new String[]{"hello", "world", "YMCA"};
        File fileLog = File.createTempFile("filelog", "");
        FileChannel fo = new FileOutputStream(fileLog).getChannel();

        for (String s : strs) {
            byte[] content = Util.toBytes(s);
            write(fo, content);
        }

        FileChannel fi = new FileInputStream(fileLog).getChannel();
        for (int i=0; i<strs.length; i++) {
            int len = readInt(fi);
            byte[] bytes = readBytes(fi, len);
            Assert.assertEquals(strs[i], Util.toObject(bytes));
        }

        fo.close();
        fi.close();
    }

    @Test
    public void testRandomAccessFileChannel() throws Exception {
        File fileLog = File.createTempFile("filelog", "");
        FileChannel fc = new RandomAccessFile(fileLog.getAbsolutePath(), "rw").getChannel();

        String[] strs = new String[]{"hello", "world", "YMCA"};

        for (String s : strs) {
            byte[] content = Util.toBytes(s);
            write(fc, content);
        }

        fc.position(0);
        for (int i=0; i<strs.length; i++) {
            int len = readInt(fc);
            byte[] bytes = readBytes(fc, len);
            Assert.assertEquals(strs[i], Util.toObject(bytes));
        }
    }

    private long write(FileChannel fo, byte[] bytes) throws Exception {
        long offset = fo.position();

        ByteBuffer bytebuffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE + bytes.length);
        // size of content
        bytebuffer.putInt(bytes.length);
        // actual content
        bytebuffer.put(bytes);

        bytebuffer.flip();

        fo.write(bytebuffer);
        fo.force(true);

        return offset;

    }

    private int readInt(FileChannel src) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        long bytesRead = src.read(byteBuffer);
        byteBuffer.flip();
        return byteBuffer.getInt();
    }

    private byte[] readBytes(FileChannel src, long length) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate((int)length);
        src.read(byteBuffer);
        byteBuffer.flip();
        return byteBuffer.array();
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

    private FileChannel getChannel(Object callee, String channelName) throws Exception {
        Field f = callee.getClass().getDeclaredField(channelName);
        f.setAccessible(true);
        return (FileChannel)f.get(callee);
    }
}

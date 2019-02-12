package amino.run.policy.util.consensus.raft;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;

/** Created by Vishwajeet on 18/1/19. */

/**
 * The PersistentArrayList class persists the data from the pstate log, as and when a new entry gets
 * added to the log. Currently the data is stored to a file on the local disk using memory map.
 */
public class PersistentArrayList<E> extends ArrayList {
    /**
     * Position starts at 104 for LogEntries as the file stores currentTerm, votedFor and myServerID
     * till this point. So to avoid overwriting these fields in the file, log entries are written
     * beyond 104.
     */
    public int position = 104;

    ByteBuffer bb;
    File file;
    FileChannel fc;

    PersistentArrayList(String fileName) {
        file = new File("/var/tmp/" + fileName + ".txt");
        try {
            fc = new RandomAccessFile(file, "rw").getChannel();
            bb = fc.map(FileChannel.MapMode.READ_WRITE, position, position + 4000);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean add(Object o) {
        boolean addStatus = super.add(o);
        if (addStatus) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream objectOut = new ObjectOutputStream(bos);
                objectOut.writeObject(o);

                Integer index = indexOf(o);
                byte[] data = bos.toByteArray();
                Integer size = data.length;
                int length = index.toString().length() + size.toString().getBytes().length + size;
                if (bb.remaining() < length) {
                    bb = fc.map(FileChannel.MapMode.READ_WRITE, bb.position(), bb.position() + 4000);
                }
                bb.put(index.toString().getBytes());
                bb.put(size.toString().getBytes());
                bb.put(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return addStatus;
    }

    @Override
    public boolean addAll(Collection c) {
        boolean addAllStatus = super.addAll(c);
        if (addAllStatus) {
            Object[] a = c.toArray();
            for (int i = 0; i < a.length; i++) {
                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream objectOut = new ObjectOutputStream(bos);
                    objectOut.writeObject(a[i]);

                    Integer index = indexOf(a[i]);
                    byte[] data = bos.toByteArray();
                    Integer size = data.length;
                    int length =
                            index.toString().length() + size.toString().getBytes().length + size;
                    if (bb.remaining() < length) {
                        bb = fc.map(FileChannel.MapMode.READ_WRITE, bb.position(), bb.position() + 4000);
                    }
                    bb.put(index.toString().getBytes());
                    bb.put(size.toString().getBytes());
                    bb.put(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return addAllStatus;
    }

    public void closeChannel() throws IOException {
        // Closing the FileChannel
        if (fc != null) {
            fc.close();
        }
    }
}

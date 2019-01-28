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
import java.util.UUID;

/** Created by Vishwajeet on 18/1/19. */

/**
 * The PersistentArrayList class persists the data from the pstate log, as and when a new entry gets
 * added to the log. Currently the data is stored to a file on the local disk using memory map.
 */
public class PersistentArrayList<E> extends ArrayList {
    public int position = 104;
    public UUID myServerID = null;
    File file;
    FileChannel fc;
    ByteArrayOutputStream bos;
    ObjectOutputStream objectOut;
    ByteBuffer bb;

    PersistentArrayList(UUID myServerID) {
        this.myServerID = myServerID;
        file = new File("/var/tmp/" + myServerID + ".txt");
    }

    @Override
    public boolean add(Object o) {
        boolean addStatus = super.add(o);
        if (addStatus) {
            try {
                fc = new RandomAccessFile(file, "rw").getChannel();
                bos = new ByteArrayOutputStream();
                objectOut = new ObjectOutputStream(bos);
                objectOut.writeObject(o);

                Integer index = indexOf(o);
                if (file.length() < position) {
                    bb = fc.map(FileChannel.MapMode.READ_WRITE, position, Integer.SIZE);
                    bb.put(index.toString().getBytes());
                } else {
                    bb = fc.map(FileChannel.MapMode.READ_WRITE, file.length(), Integer.SIZE);
                    bb.put(index.toString().getBytes());
                }
                byte[] data = bos.toByteArray();
                Integer size = data.length;
                bb = fc.map(FileChannel.MapMode.READ_WRITE, file.length(), Integer.SIZE);
                bb.put(size.toString().getBytes());
                bb = fc.map(FileChannel.MapMode.READ_WRITE, file.length(), data.length);
                bb.put(data);

                objectOut.close();
                fc.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
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
                    fc = new RandomAccessFile(file, "rw").getChannel();
                    bos = new ByteArrayOutputStream();
                    objectOut = new ObjectOutputStream(bos);
                    objectOut.writeObject(a[i]);

                    Integer index = indexOf(a[i]);
                    if (file.length() < position) {
                        bb = fc.map(FileChannel.MapMode.READ_WRITE, position, Integer.SIZE);
                        bb.put(index.toString().getBytes());
                    } else {
                        bb = fc.map(FileChannel.MapMode.READ_WRITE, file.length(), Integer.SIZE);
                        bb.put(index.toString().getBytes());
                    }
                    byte[] data = bos.toByteArray();
                    Integer size = data.length;
                    bb = fc.map(FileChannel.MapMode.READ_WRITE, file.length(), Integer.SIZE);
                    bb.put(size.toString().getBytes());
                    bb = fc.map(FileChannel.MapMode.READ_WRITE, file.length(), data.length);
                    bb.put(data);

                    objectOut.close();
                    fc.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return addAllStatus;
    }
}

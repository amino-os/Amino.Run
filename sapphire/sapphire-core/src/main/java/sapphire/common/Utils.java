package sapphire.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import sapphire.kernel.common.KernelRPC;

/**
 * Created by quinton on 1/23/18.
 */

public class Utils {
    private static final Logger logger = Logger.getLogger(Utils.class.getName());

    private Utils() {} // // so that nobody can accidentally create a Utils object
    public static class ObjectCloner { // see https://www.javaworld.com/article/2077578/learn-java/java-tip-76--an-alternative-to-the-deep-copy-technique.html
        // returns a deep copy of an object
        private ObjectCloner() {} // // so that nobody can accidentally creates an ObjectCloner object
        static public Serializable deepCopy(Serializable oldObj) throws Exception
        {
            ObjectOutputStream oos = null;
            ObjectInputStream ois = null;
            try
            {
                ByteArrayOutputStream bos =
                        new ByteArrayOutputStream();
                oos = new ObjectOutputStream(bos);
                // serialize and pass the object
                oos.writeObject(oldObj);
                oos.flush();
                ByteArrayInputStream bin =
                        new ByteArrayInputStream(bos.toByteArray());
                ois = new ObjectInputStream(bin);
                // return the new object
                return (Serializable)ois.readObject();
            }
            catch(Exception e)
            {
                System.out.println("Exception in ObjectCloner = " + e);
                throw(e);
            }
            finally
            {
                oos.close();
                ois.close();
            }
        }
    }

    /**
     * Wraps the given {@code Runnable} with a try-catch block.
     * We suggest wrap {@code Runnable}s with this wrapper before passing it into
     * {@link java.util.concurrent.ExecutorService ExecutorService}.
     *
     * @param runnable
     * @return a runnable with try catch block
     */
    public static final Runnable RunnerWrapper(final Runnable runnable) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Throwable e) {
                    logger.log(Level.WARNING, "got exception when execute runnable {0}: {1}", new Object[]{runnable, e});
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * Serialize object into bytes.
     *
     * @param object the object to be serialized. The object must implement {@link java.io.Serializable}.
     * @return a byte array
     * @throws Exception
     */
    public static final byte[] toBytes(Object object) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(object);
            out.flush();
            return bos.toByteArray();
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                // ignore close exception
            }

            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                // ignore close exception
            }
        }
    }

    public static final byte[] toBytes(KernelRPC object) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(object);
            out.flush();
            return bos.toByteArray();
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                // ignore close exception
            }

            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                // ignore close exception
            }
        }
    }

    public static Object toObject(byte[] bytes) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            Object object = in.readObject();
            return object;
        } finally {
            if (bis != null) {
                bis.close();;
            }
            if (in != null) {
                in.close();
            }
        }
    }
}

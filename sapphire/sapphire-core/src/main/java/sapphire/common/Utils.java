package sapphire.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import sapphire.kernel.common.KernelRPC;
import sapphire.runtime.annotations.Immutable;
import sapphire.runtime.annotations.Runtime;

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

    public static Object toObject(byte[] bytes) throws IOException, ClassNotFoundException {
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

    /**
     * Determines if the given method is annotated as immutable.
     *
     * @param clazz
     * @param methodName
     * @param params
     * @return <code>true</code> if the method is annotated as immutable; <code>false</code> otherwise.
     */
    public static boolean isImmutableMethod(Class<?> clazz, String methodName, ArrayList<Object> params) {
        if (clazz == null) {
            throw new NullPointerException("Class not specified");
        }

        if (methodName == null || methodName.trim().isEmpty()) {
            throw new IllegalArgumentException("method name not specified");
        }

        Class[] paramTypes = new Class[params.size()];
        for (int i=0; i<params.size(); i++) {
            paramTypes[i] = params.get(i).getClass();
        }

        try {
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            return method.getDeclaredAnnotation(Immutable.class) != null;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(String.format("unable to find method %s in class %s with params %s", methodName, clazz, params), e);
        }
    }

    /**
     * Returns the {@link Runtime} spec specified on the given class.
     *
     * @param clazz
     * @return
     */
    public static Runtime getRuntimeSpec(Class<?> clazz) {
        return clazz.getAnnotation(Runtime.class);
    }
}

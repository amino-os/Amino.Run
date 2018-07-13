package sapphire.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import sapphire.runtime.annotations.Immutable;
import sapphire.runtime.annotations.RuntimeSpec;

/** Created by quinton on 1/23/18. */
public class Utils {
    private static final Logger logger = Logger.getLogger(Utils.class.getName());

    private Utils() {} // // so that nobody can accidentally create a Utils object

    public static class ObjectCloner { // see
        // https://www.javaworld.com/article/2077578/learn-java/java-tip-76--an-alternative-to-the-deep-copy-technique.html
        // returns a deep copy of an object
        private
        ObjectCloner() {} // // so that nobody can accidentally creates an ObjectCloner object

        public static Serializable deepCopy(Serializable oldObj) throws Exception {
            ObjectOutputStream oos = null;
            ObjectInputStream ois = null;
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                oos = new ObjectOutputStream(bos);
                // serialize and pass the object
                oos.writeObject(oldObj);
                oos.flush();
                ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
                ois = new ObjectInputStream(bin);
                // return the new object
                return (Serializable) ois.readObject();
            } catch (Exception e) {
                System.out.println("Exception in ObjectCloner = " + e);
                throw (e);
            } finally {
                oos.close();
                ois.close();
            }
        }
    }

    /**
     * Wraps the given {@code Runnable} with a try-catch block. We suggest wrap {@code Runnable}s
     * with this wrapper before passing it into {@link java.util.concurrent.ExecutorService
     * ExecutorService}.
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
                    logger.log(
                            Level.WARNING,
                            String.format(
                                    "got exception when execute runnable %s: %s", runnable, e),
                            e);
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * Serialize object into bytes.
     *
     * @param object the object to be serialized. The object must implement {@link
     *     java.io.Serializable}.
     * @return a byte array
     * @throws Exception
     */
    public static final byte[] toBytes(Object object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(object);
            out.flush();
            return bos.toByteArray();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "failed to close stream bos", e);
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "failed to close stream bos", e);
                }
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
                try {
                    bis.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "failed to close stream bis", e);
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "failed to close stream in", e);
                }
            }
        }
    }

    /**
     * Determines if the given method is annotated as immutable.
     *
     * @param clazz
     * @param methodName
     * @param params
     * @return <code>true</code> if the method is annotated as immutable; <code>false</code>
     *     otherwise.
     */
    public static boolean isImmutableMethod(
            Class<?> clazz, String methodName, ArrayList<Object> params) {
        if (clazz == null) {
            throw new NullPointerException("Class not specified");
        }

        if (methodName == null || methodName.trim().isEmpty()) {
            throw new IllegalArgumentException("method name not specified");
        }

        Class[] paramTypes = new Class[params.size()];
        for (int i = 0; i < params.size(); i++) {
            paramTypes[i] = params.get(i).getClass();
        }

        try {
            Method method = clazz.getMethod(methodName, paramTypes);
            return method.getDeclaredAnnotation(Immutable.class) != null;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    String.format(
                            "unable to find method %s in %s with param types %s",
                            methodName, clazz, paramTypes),
                    e);
        }
    }

    public static boolean isImmutableMethod(Method method) {
        return method.getDeclaredAnnotation(Immutable.class) != null;
    }

    /**
     * Returns the {@link RuntimeSpec} spec specified on the given class.
     *
     * @param clazz
     * @return
     */
    public static RuntimeSpec getRuntimeSpec(Class<?> clazz) {
        return clazz.getAnnotation(RuntimeSpec.class);
    }

    /**
     * Returns the annotation of specified type from the given annotations
     *
     * @param annotations
     * @param annotationType
     * @return
     */
    public static <A extends Annotation> A getAnnotation(
            Annotation[] annotations, Class<A> annotationType) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == annotationType) {
                return (A) annotation;
            }
        }

        return null;
    }
}

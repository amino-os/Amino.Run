package sapphire.common;

import static org.junit.Assert.assertNotEquals;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Test;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelRPC;
import sapphire.policy.scalability.masterslave.LogEntry;
import sapphire.policy.scalability.masterslave.MethodInvocationRequest;
import sapphire.runtime.annotations.Immutable;
import sapphire.runtime.annotations.RuntimeSpec;

/** Created by quinton on 1/23/18. */
public class UtilsTest {

    static class TestOuter implements Serializable {
        int o;
        TestInner innerObj;

        TestOuter() {
            innerObj = new TestInner();
        }
    }

    static class TestInner implements Serializable {
        int i;
    }

    @Test
    public void clonesAreDisjoint() throws Exception {
        UtilsTest.TestOuter testObj = new TestOuter();
        testObj.o = 1;
        testObj.innerObj.i = 1;
        TestOuter cloneObj = (TestOuter) Utils.ObjectCloner.deepCopy(testObj);
        cloneObj.o = 2;
        cloneObj.innerObj.i = 2;
        assertNotEquals(testObj.o, cloneObj.o);
        assertNotEquals(testObj.innerObj.i, cloneObj.innerObj.i);
    }

    /** Added by Vishwajeet on 4/4/18. */
    public static Field getField(Class<?> clz, String name) throws IllegalAccessException {
        Field field = null;
        Class<?> cls = clz;
        while (cls != null && field == null) {
            try {
                Field fields[] = cls.getDeclaredFields();
                for (Field fld : fields) {
                    if (fld.getName().equals(name)) {
                        fld.setAccessible(true);
                        field = fld;
                        break;
                    }
                }
            } catch (Exception e) {
            }
            cls = cls.getSuperclass();
        }
        return field;
    }

    public static Method getMethod(Object instance, String name) throws IllegalAccessException {
        Method method = null;
        Class<?> cls = instance.getClass();
        while (cls != null && method == null) {
            try {
                Method methods[] = cls.getDeclaredMethods();
                for (Method mtd : methods) {
                    if (mtd.getName().equals(name)) {
                        mtd.setAccessible(true);
                        method = mtd;
                        break;
                    }
                }
            } catch (Exception e) {
            }
            cls = cls.getSuperclass();
        }
        return method;
    }

    public static Object extractFieldValueOnInstance(Object instance, String name)
            throws IllegalAccessException {
        Field field;
        field = getField(instance.getClass(), name);
        if (null != field) {
            return field.get(instance);
        }
        return null;
    }

    public static void setFieldValue(Class<?> clz, String name, Object value)
            throws IllegalAccessException {
        Field field;
        field = getField(clz, name);
        field.set(null, value);
    }

    public static void setFieldValueOnInstance(Object instance, String name, Object value)
            throws IllegalAccessException {
        Field field;
        field = getField(instance.getClass(), name);
        if (null != field) {
            field.set(instance, value);
        }
    }

    @Test
    public void testLogEntrySerialization() throws Exception {
        ArrayList<Object> params = new ArrayList<Object>();
        params.add("hello");
        MethodInvocationRequest request =
                new MethodInvocationRequest(
                        "clientId",
                        0L,
                        "invoke",
                        params,
                        MethodInvocationRequest.MethodType.MUTABLE);
        LogEntry expected = LogEntry.newBuilder().term(0).index(0).request(request).build();

        byte[] bytes = Utils.toBytes(expected);
        LogEntry actual = (LogEntry) Utils.toObject(bytes);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testMethodInvocationRequestSerialization() throws Exception {
        ArrayList<Object> params = new ArrayList<Object>();
        params.add("hello");

        MethodInvocationRequest request =
                new MethodInvocationRequest(
                        "clientId",
                        0L,
                        "invoke",
                        params,
                        MethodInvocationRequest.MethodType.MUTABLE);

        byte[] bytes = Utils.toBytes(request);
        MethodInvocationRequest actual = (MethodInvocationRequest) Utils.toObject(bytes);
        Assert.assertEquals(request, actual);
    }

    @Test
    public void testKernelRPCSerialization() throws Exception {
        KernelOID oid = new KernelOID(10);
        ArrayList<Object> params = new ArrayList<Object>();
        params.add("hello");

        KernelRPC expected = new KernelRPC(oid, "method", params);

        byte[] bytes = Utils.toBytes(expected);
        KernelRPC actual = (KernelRPC) Utils.toObject(bytes);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testIsImmutable() throws Exception {
        ArrayList<Object> params = new ArrayList<Object>();
        params.add("hello");

        Clazz clazz = new Clazz();
        Assert.assertTrue(Utils.isImmutableMethod(clazz.getClass(), "immutable", params));
    }

    @Test
    public void testIsMutable() throws Exception {
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(5);

        Clazz clazz = new Clazz();
        Assert.assertFalse(Utils.isImmutableMethod(clazz.getClass(), "mutable", params));
    }

    @Test
    public void testGetRuntimeSpec() throws Exception {
        Clazz clazz = new Clazz();
        Assert.assertEquals(3, Utils.getRuntimeSpec(clazz.getClass()).replicas());
    }

    @RuntimeSpec(replicas = 3)
    private static class Clazz {
        @Immutable
        public void immutable(String arg) {}

        public void mutable(Integer arg) {}
    }
}

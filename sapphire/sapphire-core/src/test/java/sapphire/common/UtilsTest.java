package sapphire.common;

import org.junit.Test;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Created by quinton on 1/23/18.
 */
public class UtilsTest {

    static class TestOuter implements Serializable {
        int o;
        TestInner innerObj;
        TestOuter() {
            innerObj=new TestInner();
        }
    }
    static class TestInner implements Serializable {
        int i;
    }

    @Test
    public void clonesAreDisjoint() throws Exception {
        UtilsTest.TestOuter testObj = new TestOuter();
        testObj.o = 1;
        testObj.innerObj.i=1;
        TestOuter cloneObj = (TestOuter)Utils.ObjectCloner.deepCopy(testObj);
        cloneObj.o = 2;
        cloneObj.innerObj.i = 2;
        assertNotEquals(testObj.o, cloneObj.o);
        assertNotEquals(testObj.innerObj.i, cloneObj.innerObj.i);
    }

    /**
     * Added by Vishwajeet on 4/4/18.
     */
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

    public static Object extractFieldValueOnInstance(Object instance, String name) throws IllegalAccessException {
        Field field;
        field = getField(instance.getClass(), name);
        if (null != field) {
            return field.get(instance);
        }
        return null;
    }

    public static void setFieldValue(Class<?> clz, String name, Object value) throws IllegalAccessException {
        Field field;
        field = getField(clz, name);
        field.set(null, value);
    }

    public static void setFieldValueOnInstance(Object instance, String name, Object value) throws IllegalAccessException {
        Field field;
        field = getField(instance.getClass(), name);
        if (null != field) {
            field.set(instance, value);
        }
    }
}
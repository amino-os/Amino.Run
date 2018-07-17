package sapphire.common;

import java.lang.reflect.Field;

/** utilities for unit test purpose */
public class ReflectionTestUtil {
    /**
     * sets value of (usually private) field of an object
     *
     * @param object the target object
     * @param fieldName the name of field
     * @param value the value compatible to the field
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    public static void setField(Object object, String fieldName, Object value)
            throws IllegalAccessException, NoSuchFieldException {
        Field field;
        Class<?> classInScope = object.getClass();
        while (classInScope != Object.class) {
            try {
                field = classInScope.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(object, value);
                return;
            } catch (NoSuchFieldException e) {
                classInScope = classInScope.getSuperclass();
            }
        }

        throw new NoSuchFieldError("field not found: " + fieldName);
    }
}

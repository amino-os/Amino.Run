package sapphire.runtime.annotations;

import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.Test;

/** Created by terryz on 3/15/18. */
public class ImmutableTest {
    @Test
    public void testImmutableAnnotation() throws Exception {
        Clazz clazz = new Clazz();
        Method method = clazz.getClass().getDeclaredMethod("immutableOperation");
        Assert.assertNotNull(method.getDeclaredAnnotation(Immutable.class));

        method = clazz.getClass().getDeclaredMethod("mutableOperation");
        Assert.assertNull(method.getDeclaredAnnotation(Immutable.class));
    }

    public static class Clazz {
        @Immutable
        public void immutableOperation() {}

        public void mutableOperation() {}
    }
}

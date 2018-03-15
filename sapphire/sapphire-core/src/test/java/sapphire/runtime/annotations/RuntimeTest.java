package sapphire.runtime.annotations;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by terryz on 3/15/18.
 */
public class RuntimeTest {
    private static final int numOfReplicas = 3;
    private static final String[] labels = new String[]{"cloud", "gpu"};

    @Test
    public void testRuntimeAnnotation() throws Exception {
        TestClass clazz = new TestClass();
        Runtime runtime = clazz.getClass().getAnnotation(Runtime.class);
        Assert.assertEquals(numOfReplicas, runtime.replicas());
        assertEquals(labels, runtime.hostLabels());
    }

    @Test
    public void testRuntimeAnnotationInheritance() throws Exception {
        SubClass clazz = new SubClass();
        Runtime runtime = clazz.getClass().getAnnotation(Runtime.class);
        Assert.assertEquals(numOfReplicas, runtime.replicas());
        assertEquals(labels, runtime.hostLabels());
    }

    @Test
    public void testNoRuntimeAnnotation() throws Exception {
        NoAnnotationClass clazz = new NoAnnotationClass();
        Runtime runtime = clazz.getClass().getAnnotation(Runtime.class);
        Assert.assertNull(runtime);
    }

    @Test
    public void testRuntimeAnnotationDefault() throws Exception {
        AnnotationDefaultClass clazz = new AnnotationDefaultClass();
        Runtime runtime = clazz.getClass().getAnnotation(Runtime.class);
        System.out.println(runtime);
        Assert.assertEquals(1, runtime.replicas());
        assertEquals(new String[]{}, runtime.hostLabels());
    }

    private void assertEquals(String[] expected, String[] actual) {
        Assert.assertEquals(expected.length, actual.length);
        for (int i=0; i<expected.length; i++) {
            Assert.assertEquals(expected[i], actual[i]);
        }
    }

    @Runtime(replicas = numOfReplicas, hostLabels = {"cloud", "gpu"})
    public static class TestClass {
    }

    public static class SubClass extends TestClass {
    }

    @Runtime
    public static class AnnotationDefaultClass {
    }

    public static class NoAnnotationClass {
    }
}

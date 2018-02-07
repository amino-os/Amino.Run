package sapphire.runtime.annotations;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by terryz on 3/15/18.
 */
public class RuntimeSpecTest {
    private static final int numOfReplicas = 3;
    private static final String[] labels = new String[]{"cloud", "gpu"};

    @Test
    public void testRuntimeAnnotation() throws Exception {
        TestClass clazz = new TestClass();
        RuntimeSpec runtimeSpec = clazz.getClass().getAnnotation(RuntimeSpec.class);
        Assert.assertEquals(numOfReplicas, runtimeSpec.replicas());
        assertEquals(labels, runtimeSpec.hostLabels());
    }

    @Test
    public void testRuntimeAnnotationInheritance() throws Exception {
        SubClass clazz = new SubClass();
        RuntimeSpec runtimeSpec = clazz.getClass().getAnnotation(RuntimeSpec.class);
        Assert.assertEquals(numOfReplicas, runtimeSpec.replicas());
        assertEquals(labels, runtimeSpec.hostLabels());
    }

    @Test
    public void testNoRuntimeAnnotation() throws Exception {
        NoAnnotationClass clazz = new NoAnnotationClass();
        RuntimeSpec runtimeSpec = clazz.getClass().getAnnotation(RuntimeSpec.class);
        Assert.assertNull(runtimeSpec);
    }

    @Test
    public void testRuntimeAnnotationDefault() throws Exception {
        AnnotationDefaultClass clazz = new AnnotationDefaultClass();
        RuntimeSpec runtimeSpec = clazz.getClass().getAnnotation(RuntimeSpec.class);
        System.out.println(runtimeSpec);
        Assert.assertEquals(1, runtimeSpec.replicas());
        assertEquals(new String[]{}, runtimeSpec.hostLabels());
    }

    private void assertEquals(String[] expected, String[] actual) {
        Assert.assertEquals(expected.length, actual.length);
        for (int i=0; i<expected.length; i++) {
            Assert.assertEquals(expected[i], actual[i]);
        }
    }

    @RuntimeSpec(replicas = numOfReplicas, hostLabels = {"cloud", "gpu"})
    public static class TestClass {
    }

    public static class SubClass extends TestClass {
    }

    @RuntimeSpec
    public static class AnnotationDefaultClass {
    }

    public static class NoAnnotationClass {
    }
}

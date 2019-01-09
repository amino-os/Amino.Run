package amino.run.kernel.server;

import java.lang.reflect.Method;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class KernelServerImplTest {

    @Test
    public void testParseLabels1() throws Exception {
        Set<String> labels = parseLabels("");
        Assert.assertTrue(labels.isEmpty());
    }

    @Test
    public void testParseLabels2() throws Exception {
        Set<String> labels = parseLabels("--labels");
        Assert.assertTrue(labels.isEmpty());
    }

    @Test
    public void testParseLabels3() throws Exception {
        Set<String> labels = parseLabels("--labels=");
        Assert.assertTrue(labels.isEmpty());
    }

    @Test
    public void testParseLabels4() throws Exception {
        Set<String> labels = parseLabels("--labels=a");
        Assert.assertEquals(1, labels.size());
        Assert.assertEquals("[a]", labels.toString());
    }

    @Test
    public void testParseLabels5() throws Exception {
        Set<String> labels = parseLabels("--labels=a,b");
        Assert.assertEquals(2, labels.size());
        Assert.assertEquals("[a, b]", labels.toString());
    }

    @Test
    public void testParseLabels6() throws Exception {
        Set<String> labels = parseLabels("--labels=a,b,");
        Assert.assertEquals(2, labels.size());
        Assert.assertEquals("[a, b]", labels.toString());
    }

    private Set<String> parseLabels(String labelStr) throws Exception {
        Method m = KernelServerImpl.class.getDeclaredMethod("parseLabel", String.class);
        m.setAccessible(true);
        return (Set) m.invoke(KernelServerImpl.class, labelStr);
    }
}

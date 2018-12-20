package amino.run.kernel.server;

import java.lang.reflect.Method;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class KernelServerImplTest {

    @Test
    public void testParseLabels1() throws Exception {
        Map labels = parseLabels("");
        Assert.assertTrue(labels.isEmpty());
    }

    @Test
    public void testParseLabels2() throws Exception {
        Map labels = parseLabels("--labels");
        Assert.assertTrue(labels.isEmpty());
    }

    @Test
    public void testParseLabels3() throws Exception {
        Map labels = parseLabels("--labels:");
        Assert.assertTrue(labels.isEmpty());
    }

    @Test
    public void testParseLabels4() throws Exception {
        Map labels = parseLabels("--labels a=b");
        Assert.assertEquals(1, labels.size());
        Assert.assertTrue(labels.containsKey("a"));
        Assert.assertTrue(labels.containsValue("b"));
    }

    @Test
    public void testParseLabels5() throws Exception {
        Map labels = parseLabels("--labels a=b,c=d,e=f");
        Assert.assertEquals(3, labels.size());
        Assert.assertTrue(labels.containsKey("a"));
        Assert.assertTrue(labels.containsValue("b"));
        Assert.assertTrue(labels.containsKey("c"));
        Assert.assertTrue(labels.containsValue("d"));
        Assert.assertTrue(labels.containsKey("e"));
        Assert.assertTrue(labels.containsValue("f"));
    }

    @Test
    public void testParseLabels6() throws Exception {
        Map labels = parseLabels("--labels a=b,");
        Assert.assertEquals(1, labels.size());
        Assert.assertTrue(labels.containsKey("a"));
        Assert.assertTrue(labels.containsValue("b"));
    }

    private Map parseLabels(String labelStr) throws Exception {
        Method m = KernelServerImpl.class.getDeclaredMethod("parseLabel", String.class);
        m.setAccessible(true);
        return (Map) m.invoke(KernelServerImpl.class, labelStr);
    }
}

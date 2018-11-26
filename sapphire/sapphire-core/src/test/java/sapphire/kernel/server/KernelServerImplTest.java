package sapphire.kernel.server;

import java.lang.reflect.Method;
import java.util.HashMap;
import org.junit.Assert;
import org.junit.Test;

public class KernelServerImplTest {

    @Test
    public void testParseLabels1() throws Exception {
        HashMap labels = parseLabels("");
        Assert.assertTrue(labels.isEmpty());
    }

    @Test
    public void testParseLabels2() throws Exception {
        HashMap labels = parseLabels("--labels");
        Assert.assertTrue(labels.isEmpty());
    }

    @Test
    public void testParseLabels3() throws Exception {
        HashMap labels = parseLabels("--labels:");
        Assert.assertTrue(labels.isEmpty());
    }

    @Test
    public void testParseLabels4() throws Exception {
        HashMap labels = parseLabels("--labels:a=b");
        Assert.assertEquals(1, labels.size());
        Assert.assertTrue(labels.containsKey("a"));
        Assert.assertTrue(labels.containsValue("b"));
    }

    @Test
    public void testParseLabels5() throws Exception {
        HashMap labels = parseLabels("--labels:a=b,c=d");
        Assert.assertEquals(2, labels.size());
        Assert.assertTrue(labels.containsKey("a"));
        Assert.assertTrue(labels.containsValue("b"));
        Assert.assertTrue(labels.containsKey("c"));
        Assert.assertTrue(labels.containsValue("d"));
    }

    @Test
    public void testParseLabels6() throws Exception {
        HashMap labels = parseLabels("--labels:a=b,");
        Assert.assertEquals(1, labels.size());
        Assert.assertTrue(labels.containsKey("a"));
        Assert.assertTrue(labels.containsValue("b"));
    }

    private HashMap parseLabels(String labelStr) throws Exception {
        Method m = KernelServerImpl.class.getDeclaredMethod("parseLabel", String.class);
        m.setAccessible(true);
        return (HashMap) m.invoke(KernelServerImpl.class, labelStr);
    }
}

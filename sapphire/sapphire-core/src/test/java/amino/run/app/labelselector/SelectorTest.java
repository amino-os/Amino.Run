package amino.run.app.labelselector;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SelectorTest {
    private Labels labels;
    private Requirement req1, req2, req3, req4, req5;
    private Selector selector;

    @Before
    public void setUp() throws Exception {
        selector = new Selector();
        labels = new Labels();
        labels.put("key1", "value1");
        labels.put("key2", "value2");
        labels.put("key3", "value3");
        req1 =
                new Requirement(
                        "key1", Operator.Equal, new ArrayList<String>(Arrays.asList("value1")));
        req2 = new Requirement("key2", Operator.Exists, new ArrayList<String>());
        req3 =
                new Requirement(
                        "key2", Operator.NotIn, new ArrayList<String>(Arrays.asList("value1")));
        req4 =
                new Requirement(
                        "key1",
                        Operator.In,
                        new ArrayList<String>(Arrays.asList("value1", "value5")));
        req5 =
                new Requirement(
                        "key1",
                        Operator.In,
                        new ArrayList<String>(Arrays.asList("value2", "value5")));
    }

    // when the requirements in the selector matches with the labels
    @Test
    public void testMatches() {
        Selector selector1 = selector.add(req1, req2, req3, req4);
        Assert.assertTrue(selector1.matches(labels));
    }

    // when the requirement in the selector doesn't matches with the labels
    @Test
    public void testNotMatches() {
        Selector selector1 = selector.add(req1, req3, req5);
        Assert.assertFalse(selector1.matches(labels));
    }

    // to test a function which coonverts selector in the form of requirement into string format
    @Test
    public void testToString() {
        Selector selector1 = selector.add(req1, req2, req3, req4);
        String expected =
                "[key1 = [value1]"
                        + ", "
                        + "key2 exists"
                        + ", "
                        + "key2 notin [value1]"
                        + ", "
                        + "key1 in [value1, value5]]";
        Assert.assertEquals(expected, selector1.toString());
    }
}

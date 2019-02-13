package amino.run.app.labelselector;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LabelsTest {

    private Labels labels;

    @Before
    public void setUp() {
        labels = new Labels();
        labels.put("key1", "value1");
        labels.put("key2", "value2");
        labels.put("key3", "value3");
    }

    // to test a function which tells us whether a given key is present in Labels and the
    // expectation is true
    @Test
    public void testHasKey() {
        Assert.assertTrue(labels.containsKey("key1"));
    }

    // to test a function which tells us whether a given key is present in Labels and the
    // expectation is false
    @Test
    public void testNotHasKey() {
        Assert.assertFalse(labels.containsKey("key4"));
    }

    // to test a function which returns the value of the given key present in the label and it
    // matches with the expectation
    @Test
    public void testGetValue() {
        Assert.assertEquals("value1", labels.get("key1"));
    }

    // to test a function which returns the labels and it matches with the expectation
    @Test
    public void testGetLabels() {
        Map<String, String> exp = new HashMap<String, String>();
        exp.put("key1", "value1");
        exp.put("key2", "value2");
        exp.put("key3", "value3");
        Assert.assertEquals(exp, labels);
    }

    // to test a function which converts labels into requirements and add that requirement into
    // selector and return that selector
    @Test
    public void testAsSelector() throws Exception {
        Selector selector = labels.asSelector();
        String expected = "[key1 = [value1]" + ", " + "key2 = [value2]" + ", " + "key3 = [value3]]";
        Assert.assertEquals(expected, selector.toString());
    }

    // to test a function which compares two objects of labels using equals and hashcode and the
    // expected value is true
    @Test
    public void testEqualsAndHash() {
        Labels labels1 = labels;
        Labels labels2 = labels;
        Assert.assertTrue(labels1.equals(labels2));

        Assert.assertEquals(labels1.hashCode(), labels2.hashCode());
    }

    // to test a function which compares two objects of labels using equals and the expected value
    // is false
    @Test
    public void testNotEquals() {
        Labels labels1 = labels;
        Labels labels2 = new Labels();
        labels2.put("key4", "value4");
        labels2.putAll(labels);
        Assert.assertFalse(labels1.equals(labels2));
    }
}

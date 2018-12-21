package amino.run.app;

import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RequirementTest {
    HashMap<String, String> labels;

    @Before
    public void setup() {
        labels = new HashMap<String, String>();
        labels.put("key1", "value1");
        labels.put("key2", "value2");
        labels.put("key3", "value3");
    }

    // scenario for EQUAL Operator
    @Test
    public void testEQUAL() throws Exception {
        // when label matches
        Requirement req =
                new Requirement(
                        "key1", Operator.Equal, new ArrayList<String>(Arrays.asList("value1")));
        Assert.assertTrue(req.matches(labels));

        // when label do not matches
        req =
                new Requirement(
                        "key1", Operator.Equal, new ArrayList<String>(Arrays.asList("value2")));
        Assert.assertFalse(req.matches(labels));
    }

    // scenario for IN Operator
    @Test
    public void testIN() throws Exception {
        // when label matches
        Requirement req =
                new Requirement(
                        "key1", Operator.Equal, new ArrayList<String>(Arrays.asList("value1")));
        Assert.assertTrue(req.matches(labels));

        // when label do not matches
        req =
                new Requirement(
                        "key1", Operator.Equal, new ArrayList<String>(Arrays.asList("value2")));
        Assert.assertFalse(req.matches(labels));
    }

    // scenario for NOTIN Operator
    @Test
    public void testNOTIN() throws Exception {
        // when label matches
        Requirement req =
                new Requirement(
                        "key1", Operator.Equal, new ArrayList<String>(Arrays.asList("value1")));
        Assert.assertTrue(req.matches(labels));

        // when label do not matches
        req =
                new Requirement(
                        "key1", Operator.Equal, new ArrayList<String>(Arrays.asList("value2")));
        Assert.assertFalse(req.matches(labels));
    }

    // Positive scenario for EXISTS Operator
    @Test
    public void testEXISTS() throws Exception {
        // when label matches
        Requirement req = new Requirement("key1", Operator.Exists, new ArrayList<String>());
        Assert.assertTrue(req.matches(labels));

        // when label do not matches
        req = new Requirement("key6", Operator.Exists, new ArrayList<String>());
        Assert.assertFalse(req.matches(labels));
    }

    // tests for tostring() function with EQUAL,IN,NOTIN,EXISTS operator
    @Test
    public void testToString() throws Exception {
        // check
        Requirement req =
                new Requirement(
                        "key1", Operator.Equal, new ArrayList<String>(Arrays.asList("value1")));
        Assert.assertEquals(req.toString(), "key1 = [value1]");

        req =
                new Requirement(
                        "key1",
                        Operator.In,
                        new ArrayList<String>(Arrays.asList("value1", "value2")));
        Assert.assertEquals(req.toString(), "key1 in [value1, value2]");

        req =
                new Requirement(
                        "key1",
                        Operator.NotIn,
                        new ArrayList<String>(Arrays.asList("value1", "value2")));
        Assert.assertEquals(req.toString(), "key1 notin [value1, value2]");

        req = new Requirement("key1", Operator.Exists, new ArrayList<String>());
        Assert.assertEquals(req.toString(), "key1 exists");
    }
}

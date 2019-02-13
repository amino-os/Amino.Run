package amino.run.app.labelselector;

import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RequirementTest {
    Labels labels;

    @Before
    public void setup() {
        labels = new Labels();
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
                        "key1", Requirement.Equal, new ArrayList<>(Arrays.asList("value1")));
        Assert.assertTrue(req.matches(labels));

        // when label do not matches
        req = new Requirement("key1", Requirement.Equal, new ArrayList<>(Arrays.asList("value2")));
        Assert.assertFalse(req.matches(labels));
    }

    // scenario for IN Operator
    @Test
    public void testIN() throws Exception {
        // when label matches
        Requirement req =
                new Requirement(
                        "key1", Requirement.Equal, new ArrayList<>(Arrays.asList("value1")));
        Assert.assertTrue(req.matches(labels));

        // when label do not matches
        req = new Requirement("key1", Requirement.Equal, new ArrayList<>(Arrays.asList("value2")));
        Assert.assertFalse(req.matches(labels));
    }

    // scenario for NOTIN Operator
    @Test
    public void testNOTIN() throws Exception {
        // when label matches
        Requirement req =
                new Requirement(
                        "key1", Requirement.Equal, new ArrayList<>(Arrays.asList("value1")));
        Assert.assertTrue(req.matches(labels));

        // when label do not matches
        req = new Requirement("key1", Requirement.Equal, new ArrayList<>(Arrays.asList("value2")));
        Assert.assertFalse(req.matches(labels));
    }

    // Positive scenario for EXISTS Operator
    @Test
    public void testEXISTS() throws Exception {
        // when label matches
        Requirement req = new Requirement("key1", Requirement.Exists, new ArrayList<>());
        Assert.assertTrue(req.matches(labels));

        // when label do not matches
        req = new Requirement("key6", Requirement.Exists, new ArrayList<>());
        Assert.assertFalse(req.matches(labels));
    }

    // tests for tostring() function with EQUAL,IN,NOTIN,EXISTS operator
    @Test
    public void testtostring() throws Exception {
        // check
        Requirement req =
                new Requirement(
                        "key1", Requirement.Equal, new ArrayList<>(Arrays.asList("value1")));
        Assert.assertEquals(req.toString(), "key1=value1");

        req =
                new Requirement(
                        "key1", Requirement.In, new ArrayList<>(Arrays.asList("value1", "value2")));
        Assert.assertEquals(req.toString(), "key1 in {value1,value2}");

        req =
                new Requirement(
                        "key1",
                        Requirement.NotIn,
                        new ArrayList<>(Arrays.asList("value1", "value2")));
        Assert.assertEquals(req.toString(), "key1 notin {value1,value2}");

        req = new Requirement("key1", Requirement.Exists, new ArrayList<>());
        Assert.assertEquals(req.toString(), "key1 exists");
    }

    @Test
    public void testBuilding() throws Exception {
        Requirement req =
                new Requirement(
                        "key1", Requirement.Equal, new ArrayList<>(Arrays.asList("value1")));
        Assert.assertTrue(req.matches(labels));

        req =
                new Requirement(
                        "key1", Requirement.In, new ArrayList<>(Arrays.asList("value1", "value2")));
        Assert.assertTrue(req.matches(labels));

        req =
                new Requirement(
                        "key1",
                        Requirement.NotIn,
                        new ArrayList<>(Arrays.asList("value2", "value3")));
        Assert.assertTrue(req.matches(labels));

        req = new Requirement("key1", Requirement.Exists, new ArrayList<>());
        Assert.assertTrue(req.matches(labels));
    }
}

package amino.run.app.labelselector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RequirementTest {
    Labels label;

    @Before
    public void setup() {
        label =
                Labels.newBuilder()
                        .add("key1", "value1")
                        .add("key2", "value2")
                        .add("key3", "value3")
                        .add("key4", "value4")
                        .create();
    }

    // scenario for EQUAL Operator
    @Test
    public void testEQUAL() {
        // when label matches
        Requirement req = Requirement.newBuilder().key("key1").equal().value("value1").create();
        Assert.assertTrue(req.matches(label));

        // when label do not matches
        req = Requirement.newBuilder().key("key1").equal().value("value2").create();
        Assert.assertFalse(req.matches(label));
    }

    // scenario for IN Operator
    @Test
    public void testIN() {
        // when label matches
        Requirement req =
                Requirement.newBuilder().key("key1").in().values("value1", "value2").create();
        Assert.assertTrue(req.matches(label));

        // when label do not matches
        req = Requirement.newBuilder().key("key1").in().values("value2", "value3").create();
        Assert.assertFalse(req.matches(label));
    }

    // scenario for NOTIN Operator
    @Test
    public void testNOTIN() {
        // when label matches
        Requirement req =
                Requirement.newBuilder().key("key1").notIn().values("value2", "value3").create();
        Assert.assertTrue(req.matches(label));

        // when label do not matches
        req = Requirement.newBuilder().key("key1").notIn().values("value1", "value2").create();
        Assert.assertFalse(req.matches(label));
    }

    // Positive scenario for EXISTS Operator
    @Test
    public void testEXISTS() {
        // when label matches
        Requirement req = Requirement.newBuilder().key("key1").exists().create();
        Assert.assertTrue(req.matches(label));

        // when label do not matches
        req = Requirement.newBuilder().key("key6").exists().create();
        Assert.assertFalse(req.matches(label));
    }

    // tests for tostring() function with EQUAL,IN,NOTIN,EXISTS operator
    @Test
    public void testtostring() {
        // check
        Requirement req = Requirement.newBuilder().key("key1").equal().value("value1").create();
        Assert.assertEquals(req.toString(), "key1=value1");

        req = Requirement.newBuilder().key("key1").in().values("value1", "value2").create();
        Assert.assertEquals(req.toString(), "key1 in {value1,value2}");

        req = Requirement.newBuilder().key("key1").notIn().values("value1", "value2").create();
        Assert.assertEquals(req.toString(), "key1 notin {value1,value2}");

        req = Requirement.newBuilder().key("key1").exists().create();
        Assert.assertEquals(req.toString(), "key1 exists");
    }

    @Test
    public void testvalues() {
        Requirement req =
                Requirement.newBuilder().key("key1").in().values("value1", "value2").create();
        List<String> values = new ArrayList<>();
        values.add("value1");
        values.add("value2");
        Set<String> expected = new HashSet<>(values);
        Assert.assertEquals(req.values(), expected);
    }

    @Test
    public void testKey() {
        Requirement req =
                Requirement.newBuilder().key("key1").in().values("value1", "value2").create();
        Assert.assertEquals(req.key(), "key1");
    }

    @Test
    public void testoperator() {
        Requirement req =
                Requirement.newBuilder().key("key1").in().values("value1", "value2").create();
        Assert.assertEquals(req.operator(), Requirement.In);
    }

    @Test
    public void testbuilder() {
        Requirement req = Requirement.newBuilder().key("key1").equal().value("value1").create();
        Assert.assertTrue(req.matches(label));

        req = Requirement.newBuilder().key("key1").in().values("value1", "value2").create();
        Assert.assertTrue(req.matches(label));

        req = Requirement.newBuilder().key("key1").notIn().values("value2", "value3").create();
        Assert.assertTrue(req.matches(label));

        req = Requirement.newBuilder().key("key1").exists().create();
        Assert.assertTrue(req.matches(label));
    }
}

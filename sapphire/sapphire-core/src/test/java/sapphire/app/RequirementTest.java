package sapphire.app;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import sapphire.app.labelselector.Labels;
import sapphire.app.labelselector.Requirement;

public class RequirementTest {

    private Labels createLabels() {
        Labels label =
                Labels.newBuilder()
                        .add("key1", "value1")
                        .add("key2", "value2")
                        .add("key3", "value3")
                        .add("key4", "value4")
                        .create();
        return label;
    }

    // Positive scenario for EQUAL Operator
    @Test
    public void testpositiveEQUAL() {
        Labels l = createLabels();
        List<String> values = new ArrayList<>();
        values.add("value1");
        Requirement req = new Requirement("key1", "=", values);
        boolean actual = req.matches(l);
        boolean expected = true;
        Assert.assertEquals(expected, actual);
    }

    // Negative scenario for EQUAL Operator
    @Test
    public void testnegativeEQUAL() {
        Labels l = createLabels();
        List<String> values = new ArrayList<>();
        values.add("value2");
        Requirement req = new Requirement("key1", "=", values);
        boolean actual = req.matches(l);
        boolean expected = false;
        Assert.assertEquals(expected, actual);
    }

    // Positive scenario for IN Operator
    @Test
    public void testpositiveIN() {
        Labels l = createLabels();
        List<String> values = new ArrayList<>();
        values.add("value1");
        values.add("value2");
        Requirement req = new Requirement("key1", "in", values);
        boolean actual = req.matches(l);
        boolean expected = true;
        Assert.assertEquals(expected, actual);
    }

    // Negative scenario for IN Operator
    @Test
    public void testnegativeIN() {
        Labels l = createLabels();
        List<String> values = new ArrayList<>();
        values.add("value2");
        values.add("value3");
        Requirement req = new Requirement("key1", "in", values);
        boolean actual = req.matches(l);
        boolean expected = false;
        Assert.assertEquals(expected, actual);
    }

    // Positive scenario for NOTIN Operator
    @Test
    public void testpositiveNOTIN() {
        Labels l = createLabels();
        List<String> values = new ArrayList<>();
        values.add("value2");
        values.add("value3");
        Requirement req = new Requirement("key1", "notin", values);
        boolean actual = req.matches(l);
        boolean expected = true;
        Assert.assertEquals(expected, actual);
    }

    // Negative scenario for NOTIN Operator
    @Test
    public void testnegativeNOTIN() {
        Labels l = createLabels();
        List<String> values = new ArrayList<>();
        values.add("value1");
        values.add("value2");
        Requirement req = new Requirement("key1", "notin", values);
        boolean actual = req.matches(l);
        boolean expected = false;
        Assert.assertEquals(expected, actual);
    }

    // Positive scenario for EXISTS Operator
    @Test
    public void testpositiveEXISTS() {
        Labels l = createLabels();
        List<String> values = new ArrayList<>();
        values.add("value2");
        Requirement req = new Requirement("key1", "exists", values);
        boolean actual = req.matches(l);
        boolean expected = true;
        Assert.assertEquals(expected, actual);
    }

    // Negative scenario for EXISTS Operator
    @Test
    public void testnegativeEXISTS() {
        Labels l = createLabels();
        List<String> values = new ArrayList<>();
        values.add("value2");
        Requirement req = new Requirement("key5", "exists", values);
        boolean actual = req.matches(l);
        boolean expected = false;
        Assert.assertEquals(expected, actual);
    }

    // tests for tostring() function with EQUAL,IN,NOTIN,EXISTS operator
    @Test
    public void testtostring() {
        List<String> values = new ArrayList<>();
        values.add("value1");
        Requirement req = new Requirement("key1", "=", values);
        String actual = req.toString();
        String expected = "key1=" + String.join("", values);
        Assert.assertEquals(expected, actual);

        values.add("value2");
        req = new Requirement("key1", "in", values);
        actual = req.toString();
        expected = "key1 in " + "{" + String.join(",", values) + "}";
        Assert.assertEquals(expected, actual);

        req = new Requirement("key1", "notin", values);
        actual = req.toString();
        expected = "key1 notin " + "{" + String.join(",", values) + "}";
        Assert.assertEquals(expected, actual);

        req = new Requirement("key1", "exists", values);
        actual = req.toString();
        expected = "key1key1" + String.join(",", values);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testvalues() {

        List<String> values = new ArrayList<>();
        values.add("value1");
        values.add("value2");
        Requirement req = new Requirement("key1", "Equal", values);
        Set<String> actual = req.values();
        Set<String> expected = new HashSet<>(values);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testKey() {
        List<String> values = new ArrayList<>();
        values.add("value1");
        Requirement req = new Requirement("key1", "=", values);
        String actual = req.key();
        String expected = "key1";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testoperator() {
        List<String> values = new ArrayList<>();
        values.add("value1");
        Requirement req = new Requirement("key1", "=", values);
        String actual = req.operator();
        String expected = "=";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testbuilder() {
        List<String> values = new ArrayList<>();
        values.add("value1");

        Requirement expected = new Requirement("key1", "=", values);
        Requirement actual = Requirement.newBuilder().key("key1").value("value1").equal().create();
        Assert.assertEquals(expected.toString(), actual.toString());

        expected = new Requirement("key1", "notin", values);
        actual = Requirement.newBuilder().key("key1").value("value1").notIn().create();
        Assert.assertEquals(expected.toString(), actual.toString());

        expected = new Requirement("key1", "exists", values);
        actual = Requirement.newBuilder().key("key1").value("value1").exists().create();
        Assert.assertEquals(expected.toString(), actual.toString());

        values.add("value2");
        expected = new Requirement("key1", "in", values);
        actual = Requirement.newBuilder().key("key1").values("value1", "value2").in().create();
        Assert.assertEquals(expected.toString(), actual.toString());
    }
}

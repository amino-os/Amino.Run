package amino.run.app.labelselector;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SelectorTest {

    private Labels labels;
    private Requirement req1, req2, req3, req4, req5;
    private Selector selector;

    @Before
    public void setUp() {
        selector = new Selector();
        labels =
                Labels.newBuilder()
                        .add("key1", "value1")
                        .add("key2", "value2")
                        .add("key3", "value3")
                        .create();
        req1 = Requirement.newBuilder().key("key1").equal().value("value1").create();
        req2 = Requirement.newBuilder().key("key2").exists().create();
        req3 = Requirement.newBuilder().key("key2").notIn().value("value1").create();
        req4 = Requirement.newBuilder().key("key1").in().values("value1", "value5").create();
        req5 = Requirement.newBuilder().key("key5").exists().values("value1", "value5").create();
    }

    // when the requirements in the selector matches with the labels
    @Test
    public void testMatches() {
        Selector selector1 = selector.add(req1, req2, req3, req4);
        Assert.assertEquals(true, selector1.matches(labels));
    }

    // when the requirement in the selector doesn't matches with the labels
    @Test
    public void testNotMatches() {
        Selector selector1 = selector.add(req1, req3, req4, req5);
        Assert.assertEquals(false, selector1.matches(labels));
    }

    // to test a function which coonverts selector in the form of requirement into string format
    @Test
    public void testToString() {
        Selector selector1 = selector.add(req1, req2, req3, req4);
        String expected =
                "key1=value1"
                        + ","
                        + "key2 exists"
                        + ","
                        + "key2 notin {value1}"
                        + ","
                        + "key1 in {value1,value5}";
        Assert.assertEquals(expected, selector1.toString());
    }
}

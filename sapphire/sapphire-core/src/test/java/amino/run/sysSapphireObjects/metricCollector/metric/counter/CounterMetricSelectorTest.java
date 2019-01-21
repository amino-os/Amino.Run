package amino.run.sysSapphireObjects.metricCollector.metric.counter;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Selector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CounterMetricSelectorTest {
    private CounterMetricSelector counterMetricSelector;
    private Selector selector;

    @Before
    public void setUp() {
        selector = mock(Selector.class);
        counterMetricSelector = new CounterMetricSelector("CounterMetric", selector);
    }

    @Test
    public void testGetName() {
        Assert.assertEquals("CounterMetric", counterMetricSelector.getName());
    }

    @Test
    public void testGetMetricSelector() {
        Assert.assertEquals(counterMetricSelector, counterMetricSelector.getMetricSelector());
    }

    @Test
    public void testGetSelector() {
        Assert.assertEquals(selector, counterMetricSelector.getSelector());
    }

    @Test
    public void testToString() {
        String exp =
                "CounterMetricSelector < metricName: "
                        + "CounterMetric"
                        + ","
                        + "Selector: "
                        + selector.toString()
                        + ">";
        Assert.assertEquals(exp, counterMetricSelector.toString());
    }
}

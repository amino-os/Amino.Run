package amino.run.sysSapphireObjects.metricCollector.metric.wma;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Selector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WMAMetricSelectorTest {

    private WMAMetricSelector wmaMetricSelector;
    private Selector selector;

    @Before
    public void setUp() {
        selector = mock(Selector.class);
        wmaMetricSelector = new WMAMetricSelector("WMAMetric", selector);
    }

    @Test
    public void testGetName() {
        Assert.assertEquals("WMAMetric", wmaMetricSelector.getName());
    }

    @Test
    public void testGetMetricSelector() {
        Assert.assertEquals(wmaMetricSelector, wmaMetricSelector.getMetricSelector());
    }

    @Test
    public void testGetSelector() {
        Assert.assertEquals(selector, wmaMetricSelector.getSelector());
    }
}

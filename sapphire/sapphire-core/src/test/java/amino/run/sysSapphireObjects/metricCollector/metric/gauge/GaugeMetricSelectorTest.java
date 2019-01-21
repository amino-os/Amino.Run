package amino.run.sysSapphireObjects.metricCollector.metric.gauge;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Selector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GaugeMetricSelectorTest {

    private GaugeMetricSelector gaugeMetricSelector;
    private Selector selector;

    @Before
    public void setUp() {
        selector = mock(Selector.class);
        gaugeMetricSelector = new GaugeMetricSelector("GaugeMetric", selector);
    }

    @Test
    public void testGetName() {
        Assert.assertEquals("GaugeMetric", gaugeMetricSelector.getName());
    }

    @Test
    public void testGetMetricSelector() {
        Assert.assertEquals(gaugeMetricSelector, gaugeMetricSelector.getMetricSelector());
    }

    @Test
    public void testGetSelector() {
        Assert.assertEquals(selector, gaugeMetricSelector.getSelector());
    }
}

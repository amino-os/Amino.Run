package amino.run.sysSapphireObjects.metricCollector.metric.histogram;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Selector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HistogramMetricSelectorTest {

    private HistogramMetricSelector histogramMetricSelector;
    private Selector selector;

    @Before
    public void setUp() {
        selector = mock(Selector.class);
        histogramMetricSelector = new HistogramMetricSelector("HistogramMetric", selector);
    }

    @Test
    public void testGetName() {
        Assert.assertEquals("HistogramMetric", histogramMetricSelector.getName());
    }

    @Test
    public void testGetMetricSelector() {
        Assert.assertEquals(histogramMetricSelector, histogramMetricSelector.getMetricSelector());
    }

    @Test
    public void testGetSelector() {
        Assert.assertEquals(selector, histogramMetricSelector.getSelector());
    }
}

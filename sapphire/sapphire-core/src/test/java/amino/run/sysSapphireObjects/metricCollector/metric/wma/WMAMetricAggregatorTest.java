package amino.run.sysSapphireObjects.metricCollector.metric.wma;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Labels;
import amino.run.sysSapphireObjects.metricCollector.MetricSelector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WMAMetricAggregatorTest {

    private WMAMetricAggregator wmaMetricAggregator;
    private Labels labels;
    private MetricSelector wmaMetricSelector;

    @Before
    public void setUp() {
        wmaMetricSelector = mock(MetricSelector.class);
        labels = mock(Labels.class);
        wmaMetricAggregator = new WMAMetricAggregator("WMAMetric", labels, 3);
    }

    @Test
    public void testGetMetric() {
        WMAMetric exp =
                WMAMetric.newBuilder()
                        .setMetricName("WMAMetric")
                        .setFrequency(3000)
                        .setLabels(labels)
                        .setBucketSize(3)
                        .setValue(0f)
                        .create();
        Assert.assertEquals(
                exp.toString(), wmaMetricAggregator.getMetric(wmaMetricSelector).toString());
    }

    @Test
    public void testToString() {
        String exp = "WMAMetric" + "<" + labels.toString() + ":" + "0.0" + ">";
        Assert.assertEquals(exp, wmaMetricAggregator.toString());
    }
}

package amino.run.sysSapphireObjects.metricCollector.metric.gauge;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Labels;
import amino.run.sysSapphireObjects.metricCollector.MetricSelector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GaugeMetricAggregatorTest {

    private GaugeMetricAggregator gaugeMetricAggregator;
    private Labels labels;
    private MetricSelector gaugeMetricSelector;

    @Before
    public void setUp() {
        gaugeMetricSelector = mock(MetricSelector.class);
        labels = mock(Labels.class);
        gaugeMetricAggregator = new GaugeMetricAggregator("GaugeMetric", labels);
    }

    @Test
    public void testGetMetric() {
        GaugeMetric exp =
                GaugeMetric.newBuilder().setMetricName("GaugeMetric").setLabels(labels).create();
        Assert.assertEquals(
                exp.toString(), gaugeMetricAggregator.getMetric(gaugeMetricSelector).toString());
    }

    @Test
    public void testToString() {
        String exp = "GaugeMetric" + "<" + labels.toString() + ":0.0" + ">";
        Assert.assertEquals(exp, gaugeMetricAggregator.toString());
    }
}

package amino.run.sysSapphireObjects.metricCollector.metric.histogram;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Labels;
import amino.run.sysSapphireObjects.metricCollector.MetricSelector;
import java.util.LinkedHashMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HistogramMetricAggregatorTest {

    private HistogramMetricAggregator histogramMetricAggregator;
    private Labels labels;
    private MetricSelector histogramMetricSelector;

    @Before
    public void setUp() {
        histogramMetricSelector = mock(MetricSelector.class);
        labels = mock(Labels.class);
        histogramMetricAggregator = new HistogramMetricAggregator("HistogramMetric", labels);
    }

    @Test
    public void testGetMetric() {
        HistogramMetric exp =
                HistogramMetric.newBuilder()
                        .setMetricName("HistogramMetric")
                        .setLabels(labels)
                        .setvalues(new LinkedHashMap<>())
                        .create();
        Assert.assertEquals(
                exp.getClass(),
                histogramMetricAggregator.getMetric(histogramMetricSelector).getClass());
    }
}

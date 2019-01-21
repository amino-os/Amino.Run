package amino.run.sysSapphireObjects.metricCollector.metric.counter;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Labels;
import amino.run.sysSapphireObjects.metricCollector.MetricSelector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CounterMetricAggregatorTest {

    private CounterMetricAggregator counterMetricAggregator;
    private Labels labels;
    private MetricSelector counterMetricSelector;

    @Before
    public void setUp() {
        counterMetricSelector = mock(MetricSelector.class);
        labels = mock(Labels.class);
        counterMetricAggregator = new CounterMetricAggregator("CounterMetric", labels);
    }

    @Test
    public void testGetMetric() {
        CounterMetric exp =
                CounterMetric.newBuilder()
                        .setMetricName("CounterMetric")
                        .setLabels(labels)
                        .setCount(0)
                        .create();
        Assert.assertEquals(
                exp.toString(),
                counterMetricAggregator.getMetric(counterMetricSelector).toString());
    }

    @Test
    public void testToString() {
        String exp = "CounterMetric" + "<" + labels.toString() + ":" + 0 + ">";
        Assert.assertEquals(exp, counterMetricAggregator.toString());
    }
}

package amino.run.sysSapphireObjects.metricCollector.metric.counter;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Labels;
import amino.run.app.labelselector.Requirement;
import amino.run.app.labelselector.Selector;
import amino.run.sysSapphireObjects.metricCollector.Collector;
import amino.run.sysSapphireObjects.metricCollector.Metric;
import amino.run.sysSapphireObjects.metricCollector.MetricSelector;
import amino.run.sysSapphireObjects.metricCollector.SendMetric;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CounterCollectorTest {

    private Collector counterCollector;
    private Labels labels;
    private Selector selector;
    private MetricSelector counterMetricSelector;
    private SendMetric sendMetric;
    private Metric counterMetric;
    private Requirement req1, req2, req3, req4;

    @Before
    public void setUp() {
        labels = Labels.newBuilder().add("key1", "value1").add("key2", "value2").create();
        counterCollector = new CounterCollector(labels);
        req1 = Requirement.newBuilder().key("key1").equal().value("value1").create();
        req2 = Requirement.newBuilder().key("key1").in().values("value1", "value2").create();
        req3 = Requirement.newBuilder().key("key2").notIn().value("value1").create();
        req4 = Requirement.newBuilder().key("key2").exists().value("value1").create();
        selector = new Selector();
        selector.add(req1, req2, req3, req4);
        counterMetricSelector = new CounterMetricSelector("CounterMetric", selector);
        sendMetric = mock(SendMetric.class);
        counterMetric =
                CounterMetric.newBuilder()
                        .setLabels(labels)
                        .setMetricName("CounterMetric")
                        .setFrequency(3000)
                        .setSendMetric(sendMetric)
                        .create();
    }

    @Test
    public void testCollectAndRetrieve() throws Exception {
        counterCollector.collect(counterMetric);
        List<Metric> exp = new ArrayList<>(Arrays.asList(counterMetric));
        Assert.assertEquals(
                exp.toString(), counterCollector.retrieve(counterMetricSelector).toString());
    }
}

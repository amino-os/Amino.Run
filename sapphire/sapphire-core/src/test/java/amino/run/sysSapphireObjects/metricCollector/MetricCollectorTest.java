package amino.run.sysSapphireObjects.metricCollector;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Labels;
import amino.run.app.labelselector.Requirement;
import amino.run.app.labelselector.Selector;
import amino.run.sysSapphireObjects.metricCollector.metric.counter.CounterMetric;
import amino.run.sysSapphireObjects.metricCollector.metric.counter.CounterMetricSelector;
import amino.run.sysSapphireObjects.metricCollector.metric.counter.Schema;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MetricCollectorTest {

    private Metric metric;
    private Labels labels;
    private MetricCollector metricCollector;
    private MetricSchema metricSchema;
    SendMetric sendMetric;

    @Before
    public void setUp() {
        sendMetric = mock(SendMetric.class);
        labels = Labels.newBuilder().add("key1", "value1").add("key2", "value2").create();
        metricCollector = new MetricCollector();
    }

    // this test will register a new metric schema for "CounterMetric" and push it in Collector
    @Test
    public void testRegisterAndPushCounterMetric() throws Exception {
        // when MetricSchema is null then it will not get register in the MetricCollector
        Assert.assertEquals(false, metricCollector.register(metricSchema));

        // constructs a new MetricSchema of "CounterMetric"
        metricSchema = new Schema("CounterMetric", labels);

        // when MetricSchema is not yet registered then it will return false value
        Assert.assertEquals(false, metricCollector.registered(metricSchema));

        // when MetricSchema is initialized wth metricName and labels then it will get register in
        // the MetricCollector
        Assert.assertEquals(true, metricCollector.register(metricSchema));

        // when MetricSchema is registered then it will return true value
        Assert.assertEquals(true, metricCollector.registered(metricSchema));

        // when metric is null then we cannot push it in the MetricCollector and it returns false
        // value
        Assert.assertEquals(false, metricCollector.push(metric));

        metric =
                CounterMetric.newBuilder()
                        .setMetricName("CounterMetric")
                        .setLabels(labels)
                        .setFrequency(3000)
                        .setSendMetric(sendMetric)
                        .create();

        // when metric is initialized then we can push in the MetricCollector and it returns true
        // value
        Assert.assertEquals(true, metricCollector.push(metric));
    }

    // this test case will return an ArrayList of CounterMetric whose labels satisfies the
    // requirements in the MetricSelector and also tests toString() conversion of MetricSelector
    @Test
    public void testGetCounterMetric() throws Exception {
        metricSchema = new Schema("CounterMetric", labels);
        metricCollector.register(metricSchema);
        metric =
                CounterMetric.newBuilder()
                        .setMetricName("CounterMetric")
                        .setLabels(labels)
                        .setFrequency(3000)
                        .setSendMetric(sendMetric)
                        .create();
        metricCollector.push(metric);
        List<Metric> expected = new ArrayList<>(Arrays.asList(metric));
        Selector selector = getSelector();
        MetricSelector counterMetricSelector = new CounterMetricSelector("CounterMetric", selector);

        Assert.assertEquals(
                expected.get(0).toString(),
                metricCollector.get(counterMetricSelector).get(0).toString());
    }

    // creates a new selector object and adds requirements (in the form of key,operator & values)
    private Selector getSelector() {
        Requirement req1 = Requirement.newBuilder().key("key1").equal().value("value1").create();
        Requirement req2 =
                Requirement.newBuilder().key("key1").in().values("value1", "value2").create();
        Requirement req3 = Requirement.newBuilder().key("key1").notIn().value("value2").create();
        Requirement req4 = Requirement.newBuilder().key("key2").exists().value("value1").create();
        Selector selector = new Selector();
        selector.add(req1, req2, req3, req4);
        return selector;
    }
}

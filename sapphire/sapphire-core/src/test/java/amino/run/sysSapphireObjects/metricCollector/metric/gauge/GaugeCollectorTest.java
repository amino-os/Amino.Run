package amino.run.sysSapphireObjects.metricCollector.metric.gauge;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Labels;
import amino.run.app.labelselector.Requirement;
import amino.run.app.labelselector.Selector;
import amino.run.sysSapphireObjects.metricCollector.Metric;
import amino.run.sysSapphireObjects.metricCollector.MetricSelector;
import amino.run.sysSapphireObjects.metricCollector.SendMetric;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GaugeCollectorTest {

    private GaugeCollector gaugeCollector;
    private Labels labels;
    private Metric gaugeMetric;
    private MetricSelector gaugeMetricSelector;
    private Selector selector;
    private Requirement req1, req2, req3, req4;
    private SendMetric sendMetric;

    @Before
    public void setUp() {
        labels = Labels.newBuilder().add("key1", "value1").add("key2", "value2").create();
        gaugeCollector = new GaugeCollector(labels);
        sendMetric = mock(SendMetric.class);
        gaugeMetric =
                GaugeMetric.newBuilder()
                        .setMetricName("GaugeMetric")
                        .setFrequency(3000)
                        .setLabels(labels)
                        .setSendMetric(sendMetric)
                        .create();
        req1 = Requirement.newBuilder().key("key1").equal().value("value1").create();
        req2 = Requirement.newBuilder().key("key1").in().values("value1", "value2").create();
        req3 = Requirement.newBuilder().key("key2").notIn().value("value1").create();
        req4 = Requirement.newBuilder().key("key2").exists().value("value1").create();
        selector = new Selector();
        selector.add(req1, req2, req3, req4);
        gaugeMetricSelector = new GaugeMetricSelector("GaugeMetric", selector);
    }

    @Test
    public void testCollectAndRetrieve() throws Exception {
        gaugeCollector.collect(gaugeMetric);
        List<Metric> exp = new ArrayList<>(Arrays.asList(gaugeMetric));
        Assert.assertEquals(
                exp.toString(), gaugeCollector.retrieve(gaugeMetricSelector).toString());
    }
}

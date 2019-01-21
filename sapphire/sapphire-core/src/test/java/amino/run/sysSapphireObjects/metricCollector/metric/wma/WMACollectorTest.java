package amino.run.sysSapphireObjects.metricCollector.metric.wma;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Labels;
import amino.run.app.labelselector.Requirement;
import amino.run.app.labelselector.Selector;
import amino.run.sysSapphireObjects.metricCollector.MetricSelector;
import amino.run.sysSapphireObjects.metricCollector.SendMetric;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WMACollectorTest {

    private WMACollector wmaCollector;
    private Labels labels;
    private WMAMetric wmaMetric;
    private MetricSelector wmaMetricSelector;
    private Selector selector;
    private Requirement req1, req2, req3, req4;
    private SendMetric sendMetric;

    @Before
    public void setUp() {
        labels = Labels.newBuilder().add("key1", "value1").add("key2", "value2").create();
        wmaCollector = new WMACollector(labels, 3);
        sendMetric = mock(SendMetric.class);

        req1 = Requirement.newBuilder().key("key1").equal().value("value1").create();
        req2 = Requirement.newBuilder().key("key1").in().values("value1", "value2").create();
        req3 = Requirement.newBuilder().key("key2").notIn().value("value1").create();
        req4 = Requirement.newBuilder().key("key2").exists().value("value1").create();
        selector = new Selector();
        selector.add(req1, req2, req3, req4);
        wmaMetricSelector = new WMAMetricSelector("WMAMetric", selector);
    }

    @Test
    public void testCollectAndRetrieve() throws Exception {
        ArrayList<Float> values = new ArrayList<>(Arrays.asList(1f));
        wmaMetric =
                WMAMetric.newBuilder()
                        .setMetricName("WMAMetric")
                        .setFrequency(3000)
                        .setLabels(labels)
                        .setBucketSize(3)
                        .setvalues(values)
                        .setSendMetric(sendMetric)
                        .create();

        wmaMetric.setValue(1, 1f);
        wmaCollector.collect(wmaMetric);
        String exp = "WMAMetric" + "<" + labels.toString() + ":" + values + ">";
        Assert.assertEquals(exp, wmaCollector.retrieve(wmaMetricSelector).get(0).toString());
    }
}

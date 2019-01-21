package amino.run.sysSapphireObjects.metricCollector.metric.histogram;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Labels;
import amino.run.sysSapphireObjects.metricCollector.SendMetric;
import java.util.LinkedHashMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HistogramMetricTest {

    private HistogramMetric histogramMetric;
    private Labels labels;
    private SendMetric sendMetric;

    @Before
    public void setUp() {
        labels = mock(Labels.class);
        sendMetric = mock(SendMetric.class);
        histogramMetric =
                HistogramMetric.newBuilder()
                        .setMetricName("HistogramMetric")
                        .setLabels(labels)
                        .setSendMetric(sendMetric)
                        .setFrequency(15)
                        .setBucketSize(2)
                        .setvalues(new LinkedHashMap<>())
                        .create();
    }

    @Test
    public void testGetName() {
        Assert.assertEquals("HistogramMetric", histogramMetric.getName());
    }

    @Test
    public void testGetMetric() {
        Assert.assertEquals(histogramMetric, histogramMetric.getMetric());
    }

    @Test
    public void testGetLabels() {
        Assert.assertEquals(labels, histogramMetric.getLabels());
    }

    @Test
    public void testObservedValues() {
        LinkedHashMap<Long, Object> exp = new LinkedHashMap<>();

        // this will return the value set during histogramMetric so the expected value is empty
        // LinkedHashMap
        Assert.assertEquals(exp, histogramMetric.getValue());

        // this will return false as the value list is empty
        Assert.assertEquals(false, histogramMetric.modified());

        exp.put(1L, "One");
        histogramMetric.setValue(1L, "One");

        histogramMetric.start();

        // set the observedvalue in histogramMetric retrieves it and it matches with our expectation
        Assert.assertEquals(exp, histogramMetric.getValue());

        // this will return true as the value list is not empty
        Assert.assertEquals(true, histogramMetric.modified());

        histogramMetric.reset();

        // reseyts the observedvalue so now the expected value is empty LinkedHashMap
        Assert.assertEquals(new LinkedHashMap<>(), histogramMetric.getValue());

        histogramMetric.stop();
    }
}

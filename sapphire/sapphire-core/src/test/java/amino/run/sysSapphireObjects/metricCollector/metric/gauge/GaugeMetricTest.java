package amino.run.sysSapphireObjects.metricCollector.metric.gauge;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Labels;
import amino.run.sysSapphireObjects.metricCollector.SendMetric;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GaugeMetricTest {

    private GaugeMetric gaugeMetric;
    private Labels labels;
    private SendMetric sendMetric;

    @Before
    public void setUp() {
        sendMetric = mock(SendMetric.class);
        labels = mock(Labels.class);
        gaugeMetric =
                GaugeMetric.newBuilder()
                        .setMetricName("GaugeMetric")
                        .setLabels(labels)
                        .setFrequency(3000)
                        .setSendMetric(sendMetric)
                        .setValue(0)
                        .create();
    }

    @Test
    public void testGetName() {
        Assert.assertEquals("GaugeMetric", gaugeMetric.getName());
    }

    @Test
    public void testGetMetric() {
        Assert.assertEquals(gaugeMetric, gaugeMetric.getMetric());
    }

    @Test
    public void testToString() {
        String exp = "GaugeMetric" + "<" + labels.toString() + ":0.0" + ">";
        Assert.assertEquals(exp, gaugeMetric.toString());
    }

    @Test
    public void testValues() {

        // this will get the value which is initialized during gaugeMetric
        Assert.assertEquals(0.0, gaugeMetric.getValue(), 0);

        //// this will return false as the value in the gaugeMetric is 0
        Assert.assertEquals(false, gaugeMetric.modified());

        gaugeMetric.setValue(5);

        // this will set the value 5 and returns a new value which is the mean of old and new value
        // so now the expected value is 2.5
        Assert.assertEquals(2.5, gaugeMetric.getValue(), 0);

        // this will return true as the value in the gaugemetric is not 0
        Assert.assertEquals(true, gaugeMetric.modified());

        GaugeMetric gaugeMetric1 =
                GaugeMetric.newBuilder()
                        .setMetricName("GaugeMetric")
                        .setLabels(labels)
                        .setFrequency(5)
                        .setSendMetric(sendMetric)
                        .create();

        gaugeMetric1.setValue(4);

        gaugeMetric1.start();

        gaugeMetric.reset();

        // this will reset the value in the gaugeMetric to 0 so now the expected value is 0
        Assert.assertEquals(0.0, gaugeMetric.getValue(), 0);

        // this will return false as the value in the gaugeMetric is nw 0
        Assert.assertEquals(false, gaugeMetric.modified());

        gaugeMetric1.stop();
    }

    @Test
    public void testLabels() {
        Assert.assertEquals(labels, gaugeMetric.getLabels());
    }
}

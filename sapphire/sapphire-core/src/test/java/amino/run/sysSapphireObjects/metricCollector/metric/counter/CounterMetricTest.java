package amino.run.sysSapphireObjects.metricCollector.metric.counter;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Labels;
import amino.run.sysSapphireObjects.metricCollector.SendMetric;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CounterMetricTest {

    private CounterMetric countermetric;
    private Labels labels;
    private SendMetric sendMetric;

    @Before
    public void setUp() {
        labels = mock(Labels.class);
        sendMetric = mock(SendMetric.class);
        countermetric =
                CounterMetric.newBuilder()
                        .setLabels(labels)
                        .setFrequency(25)
                        .setMetricName("CounterMetric")
                        .setSendMetric(sendMetric)
                        .setCount(0)
                        .create();
    }

    @Test
    public void testGetName() {
        Assert.assertEquals("CounterMetric", countermetric.getName());
    }

    @Test
    public void testGetMetric() {
        Assert.assertEquals(countermetric, countermetric.getMetric());
    }

    @Test
    public void testToString() {
        String exp = "CounterMetric" + "<" + labels.toString() + ":0" + ">";
        Assert.assertEquals(exp, countermetric.toString());
    }

    @Test
    public void testGetLabels() {
        Assert.assertEquals(labels, countermetric.getLabels());
    }

    @Test
    public void testCount() {
        // this will get the value of the count which is initialized during countermetric
        Assert.assertEquals(0, countermetric.getCount());

        // this will return false value as the count in the countermetric is 0
        Assert.assertEquals(false, countermetric.modified());

        countermetric.incCount();

        countermetric.start();

        // increment the value of count by 1 so now the expected value is 1
        Assert.assertEquals(1, countermetric.getCount());

        // this will return true as the count in the countermetric is changed from 0 to 1
        Assert.assertEquals(true, countermetric.modified());

        countermetric.reset();

        // reset the value of counter to 0 so now the expected value is 0
        Assert.assertEquals(0, countermetric.getCount());

        // this will return false as the count in countermetric is 0
        Assert.assertEquals(false, countermetric.modified());

        countermetric.stop();
    }
}

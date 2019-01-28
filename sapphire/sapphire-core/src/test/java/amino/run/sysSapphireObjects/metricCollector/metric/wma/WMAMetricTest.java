package amino.run.sysSapphireObjects.metricCollector.metric.wma;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Labels;
import amino.run.sysSapphireObjects.metricCollector.SendMetric;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WMAMetricTest {

    WMAMetric wmaMetric;
    Labels labels;
    SendMetric sendMetric;

    @Before
    public void setUp() {
        labels = Labels.newBuilder().add("key1", "value1").add("key2", "value2").create();
        sendMetric = mock(SendMetric.class);
        wmaMetric =
                WMAMetric.newBuilder()
                        .setMetricName("WMAMetric")
                        .setLabels(labels)
                        .setFrequency(15)
                        .setBucketSize(3)
                        .setValue(0f)
                        .create();
    }

    @Test
    public void testGetName() {
        Assert.assertEquals("WMAMetric", wmaMetric.getName());
    }

    @Test
    public void testGetMetric() {
        Assert.assertEquals(wmaMetric, wmaMetric.getMetric());
    }

    @Test
    public void testGetLabels() {
        Assert.assertEquals(labels, wmaMetric.getLabels());
    }

    @Test
    public void testObservedValues() {
        List<Float> exp = new ArrayList<>();

        // this will return false as value is empty arraylist
        Assert.assertEquals(false, wmaMetric.modified());

        exp.add(1f);
        exp.add(2f);
        exp.add(3f);

        wmaMetric.setValue(1, 1f);
        wmaMetric.setValue(2, 2f);
        wmaMetric.setValue(3, 3f);

        wmaMetric.start();

        // this will check the each value which are set in WMAMetric
        Assert.assertEquals((float) exp.get(0), wmaMetric.getObservations().get(0).getValue(), 0);
        Assert.assertEquals((float) exp.get(1), wmaMetric.getObservations().get(1).getValue(), 0);
        Assert.assertEquals((float) exp.get(2), wmaMetric.getObservations().get(2).getValue(), 0);

        // this will return true as value in WMAMetric is not empty
        Assert.assertEquals(true, wmaMetric.modified());

        wmaMetric.reset();

        wmaMetric.stop();
    }
}

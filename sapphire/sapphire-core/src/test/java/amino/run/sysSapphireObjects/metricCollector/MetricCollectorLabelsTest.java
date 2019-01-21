package amino.run.sysSapphireObjects.metricCollector;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MetricCollectorLabelsTest {

    private MetricCollectorLabels metricCollectorLabels;

    @Before
    public void setUp() {
        metricCollectorLabels = new MetricCollectorLabels();
    }

    @Test
    public void testMetricCollectorLabels() {
        String exp = "sys/sapphireObject" + "=" + "MetricCollector";
        Assert.assertEquals(exp, MetricCollectorLabels.labels.toString());
    }
}

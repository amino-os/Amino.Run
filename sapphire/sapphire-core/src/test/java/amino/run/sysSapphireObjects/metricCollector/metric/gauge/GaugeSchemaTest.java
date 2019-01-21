package amino.run.sysSapphireObjects.metricCollector.metric.gauge;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Labels;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GaugeSchemaTest {

    private GaugeSchema gaugeSchema;
    private Labels labels;
    private GaugeCollector gaugeCollector;

    @Before
    public void setUp() {
        labels = mock(Labels.class);
        gaugeCollector = new GaugeCollector(labels);
        gaugeSchema = new GaugeSchema("GaugeMetric", labels);
    }

    @Test
    public void testGetName() {
        Assert.assertEquals("GaugeMetric", gaugeSchema.getName());
    }

    @Test
    public void testGetCollector() {
        Assert.assertEquals(gaugeCollector.getClass(), gaugeSchema.getCollector().getClass());
    }
}

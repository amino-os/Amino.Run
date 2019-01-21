package amino.run.sysSapphireObjects.metricCollector.metric.wma;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Labels;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WMASchemaTest {

    private WMASchema wmaSchema;
    private Labels labels;

    @Before
    public void setUp() {
        labels = mock(Labels.class);
        wmaSchema = new WMASchema("WMAMetric", labels, 3);
    }

    @Test
    public void testGetName() {
        Assert.assertEquals("WMAMetric", wmaSchema.getName());
    }

    @Test
    public void testGetCollector() {
        WMACollector wmaCollector = new WMACollector(labels, 3);
        Assert.assertEquals(wmaCollector.getClass(), wmaSchema.getCollector().getClass());
    }
}

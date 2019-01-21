package amino.run.sysSapphireObjects.metricCollector.metric.histogram;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Labels;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HistogramSchemaTest {

    private HistogramSchema histogramSchema;
    private Labels labels;

    @Before
    public void setUp() {
        labels = mock(Labels.class);
        histogramSchema = new HistogramSchema("HistogramMetric", labels);
    }

    @Test
    public void testGetName() {
        Assert.assertEquals("HistogramMetric", histogramSchema.getName());
    }

    @Test
    public void testGetCollector() {
        HistogramCollector histogramCollector = new HistogramCollector(labels);
        Assert.assertEquals(
                histogramCollector.getClass(), histogramSchema.getCollector().getClass());
    }
}

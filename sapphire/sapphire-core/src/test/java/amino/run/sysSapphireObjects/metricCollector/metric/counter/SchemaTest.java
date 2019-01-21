package amino.run.sysSapphireObjects.metricCollector.metric.counter;

import static org.mockito.Mockito.mock;

import amino.run.app.labelselector.Labels;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SchemaTest {

    private Schema schema;
    private Labels labels;
    private CounterCollector counterCollector;

    @Before
    public void setUp() {
        labels = mock(Labels.class);
        schema = new Schema("CounterMetric", labels);
        counterCollector = new CounterCollector(labels);
    }

    @Test
    public void testGetName() {
        Assert.assertEquals("CounterMetric", schema.getName());
    }

    @Test
    public void testGetCollector() {
        Assert.assertEquals(counterCollector.getClass(), schema.getCollector().getClass());
    }
}

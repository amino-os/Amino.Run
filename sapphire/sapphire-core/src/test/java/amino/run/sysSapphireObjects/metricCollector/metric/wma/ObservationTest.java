package amino.run.sysSapphireObjects.metricCollector.metric.wma;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ObservationTest {

    private Observation observation;

    @Before
    public void setUp() {
        observation = new Observation(0L, 0);
    }

    @Test
    public void testTimeInMillis() {
        observation.setTimeInMillis(2000L);
        Assert.assertEquals(2000L, observation.getTimeInMillis());
    }

    @Test
    public void testValue() {
        observation.setValue(2);
        Assert.assertEquals(2, observation.getValue(), 0);
    }
}

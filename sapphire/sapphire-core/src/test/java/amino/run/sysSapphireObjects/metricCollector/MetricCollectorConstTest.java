package amino.run.sysSapphireObjects.metricCollector;

import amino.run.app.Language;
import amino.run.app.SapphireObjectSpec;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MetricCollectorConstTest {

    private MetricCollectorConst metricCollectorConst;

    @Before
    public void setUp() {
        metricCollectorConst = new MetricCollectorConst();
    }

    @Test
    public void testSystemSoName() {
        Assert.assertEquals("MetricCollector", MetricCollectorConst.SYSTEM_SO_NAME);
    }

    @Test
    public void testSapphireObjectSpec() {
        SapphireObjectSpec exp =
                SapphireObjectSpec.newBuilder()
                        .setJavaClassName(
                                "amino.run.sysSapphireObjects.metricCollector.MetricCollector")
                        .setLang(Language.java)
                        .create();
        Assert.assertEquals(exp.toString(), MetricCollectorConst.SAPPHIRE_OBJECT_SPEC.toString());
    }
}

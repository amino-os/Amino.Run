package amino.run.sysSapphireObjects.metricCollector;

import amino.run.app.Language;
import amino.run.app.SapphireObjectSpec;
import amino.run.sysSapphireObjects.SystemSapphireObjectDeploy;

public class MetricCollectorConst {
    public static String SYSTEM_SO_NAME = "MetricCollector";
    public static SystemSapphireObjectDeploy SYSTEM_SO_DEPLOY = new MetricCollectorDeploy();
    public static SapphireObjectSpec SAPPHIRE_OBJECT_SPEC =
            SapphireObjectSpec.newBuilder()
                    .setJavaClassName(
                            "amino.run.sysSapphireObjects.metricCollector.MetricCollector")
                    .setLang(Language.java)
                    .create();
}

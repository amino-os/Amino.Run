package amino.run.sysSapphireObjects.metricCollector;

import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.sysSapphireObjects.SystemSapphireObjectDeploy;

public class MetricCollectorConst {
    public static String SYSTEM_SO_NAME = "MetricCollector";
    public static SystemSapphireObjectDeploy SYSTEM_SO_DEPLOY = new MetricCollectorDeploy();
    public static MicroServiceSpec SAPPHIRE_OBJECT_SPEC =
            MicroServiceSpec.newBuilder()
                    .setJavaClassName(
                            "amino.run.sysSapphireObjects.metricCollector.MetricCollector")
                    .setLang(Language.java)
                    .create();
}

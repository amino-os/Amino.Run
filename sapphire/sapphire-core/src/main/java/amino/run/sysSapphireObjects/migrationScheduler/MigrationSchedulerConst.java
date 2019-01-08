package amino.run.sysSapphireObjects.migrationScheduler;

import amino.run.app.Language;
import amino.run.app.SapphireObjectSpec;
import amino.run.app.labelselector.Labels;
import amino.run.sysSapphireObjects.SystemSapphireObjectDeploy;

public class MigrationSchedulerConst {
    public static final String SYSTEM_SO_NAME = "MigrationScheduler";
    public static final SystemSapphireObjectDeploy SYSTEM_SO_DEPLOY =
            new MigrationSchedulerDeploy();
    public static final SapphireObjectSpec SAPPHIRE_OBJECT_SPEC =
            SapphireObjectSpec.newBuilder()
                    .setJavaClassName(
                            "amino.run.sysSapphireObjects.metricCollector.MigrationScheduler")
                    .setLang(Language.java)
                    .create();
    public static final Labels LABELS;

    static {
        LABELS = Labels.newBuilder().add("sys/sapphireObject", "MigrationScheduler").create();
    }
}

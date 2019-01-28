package amino.run.sysSapphireObjects.migrationScheduler;

import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.app.labelselector.Labels;
import amino.run.sysSapphireObjects.SystemSapphireObjectDeploy;
import java.awt.*;

public class MigrationSchedulerConst {
    public static final String SYSTEM_SO_NAME = "MigrationScheduler";
    public static final SystemSapphireObjectDeploy SYSTEM_SO_DEPLOY =
            new MigrationSchedulerDeploy();
    public static final MicroServiceSpec SAPPHIRE_OBJECT_SPEC =
            MicroServiceSpec.newBuilder()
                    .setJavaClassName(
                            "amino.run.sysSapphireObjects.migrationScheduler.MigrationScheduler")
                    .setLang(Language.java)
                    .create();
    public static final Labels LABELS;
    public static final Labels MIGRATION;

    static {
        LABELS = Labels.newBuilder().add("sys/sapphireObject", "MigrationScheduler").create();
        MIGRATION =
                Labels.newBuilder()
                        .add(
                                amino.run.policy.metric.MetricDMConstants.SAPPHIRE_OBJECT_MIGRATION,
                                Boolean.toString(true))
                        .create();
    }
}

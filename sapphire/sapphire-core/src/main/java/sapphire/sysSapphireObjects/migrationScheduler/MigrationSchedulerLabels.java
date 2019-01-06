package sapphire.sysSapphireObjects.migrationScheduler;

import sapphire.app.labelselector.Labels;

public class MigrationSchedulerLabels {
    public static final Labels labels;

    static {
        labels = Labels.newBuilder().add("sys/sapphireObject", "MigrationScheduler").create();
    }
}

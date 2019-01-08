package amino.run.sysSapphireObjects;

import amino.run.sysSapphireObjects.metricCollector.MetricCollectorConst;
import amino.run.sysSapphireObjects.migrationScheduler.MigrationSchedulerConst;
import java.util.HashMap;
import java.util.Map;

public class SystemSapphireObjectList {
    public static Map<String, SystemSapphireObjectDeploy> sysSOList = new HashMap<>();
    public static Map<String, SystemSapphireObjectDeploy> sysDefaultSOList = new HashMap<>();

    // register all system sapphire object here
    static {
        // list of default system sapphire object

        // list of system sapphire object
        sysSOList.put(MetricCollectorConst.SYSTEM_SO_NAME, MetricCollectorConst.SYSTEM_SO_DEPLOY);
        sysSOList.put(
                MigrationSchedulerConst.SYSTEM_SO_NAME, MigrationSchedulerConst.SYSTEM_SO_DEPLOY);

        // list of all system sapphire object
        sysSOList.putAll(sysDefaultSOList);
    }
}

package amino.run.sysSapphireObjects.migrationScheduler;

import amino.run.app.MicroServiceSpec;
import amino.run.app.SapphireObjectServer;
import amino.run.app.labelselector.Labels;
import amino.run.app.labelselector.Selector;
import amino.run.common.AppObjectStub;
import amino.run.oms.SystemSapphireObjectHandler;
import amino.run.sysSapphireObjects.SystemSapphireObjectDeploy;
import amino.run.sysSapphireObjects.migrationScheduler.policy.Config;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/** Client class for deploying {@link MigrationScheduler} */
public class MigrationSchedulerDeploy implements SystemSapphireObjectDeploy {
    private static Logger logger = Logger.getLogger(MigrationScheduler.class.getName());
    boolean isDeploymentStarted = false;

    @Override
    public void start(SapphireObjectServer server, String specFile) throws Exception {
        if (isDeploymentStarted) {
            logger.warning("deployment started for MigrationScheduler");
            return;
        }
        isDeploymentStarted = true;
        server.createSapphireObject(
                getSpec(specFile),
                MigrationSchedulerPolicies.ExecutionPolicy,
                getMigrationSchedulerSpec());

        // use sys label to get deployed metric collector SO.
        // get sys selector
        Selector selector = MigrationSchedulerConst.LABELS.asSelector();
        // acquire sapphire objects based on selector
        ArrayList<AppObjectStub> sapphireStubList = server.acquireSapphireObjectStub(selector);

        if (sapphireStubList.size() != 1) {
            throw new Exception("Migration scheduler deployment Failed!!!");
        }

        AppObjectStub appObjectStub = null;
        for (AppObjectStub appStub : sapphireStubList) {
            appObjectStub = appStub;
        }

        MigrationScheduler app = (MigrationScheduler) appObjectStub;

        app.start();

        logger.info("Migration scheduler deployment Success!!!");
    }

    private static String getSpec(String specFile) throws Exception {
        MicroServiceSpec spec = MigrationSchedulerConst.SAPPHIRE_OBJECT_SPEC;
        if (!SystemSapphireObjectHandler.EMPTY_SO_SPEC.equals(specFile)) {
            File file = new File(specFile);
            List<String> lines = Files.readAllLines(file.toPath());
            String userDefinedSpec = String.join("\n", lines);

            spec = MicroServiceSpec.fromYaml(userDefinedSpec);
        }

        Labels metricCollectorLabels =
                Labels.newBuilder()
                        .merge(spec.getMicroServiceLabels())
                        .merge(MigrationSchedulerConst.LABELS)
                        .create();

        spec.setMicroServiceLabels(metricCollectorLabels);

        return spec.toString();
    }

    private static String getMigrationSchedulerSpec() throws Exception {
        Config config = new Config();
        config.setMetricLabels(MigrationSchedulerConst.MIGRATION);
        return config.toString();
    }
}

package amino.run.sysSapphireObjects.metricCollector;

import amino.run.app.SapphireObjectServer;
import amino.run.app.SapphireObjectSpec;
import amino.run.app.labelselector.Labels;
import amino.run.app.labelselector.Selector;
import amino.run.common.AppObjectStub;
import amino.run.common.SapphireObjectID;
import amino.run.oms.SystemSapphireObjectHandler;
import amino.run.sysSapphireObjects.SystemSapphireObjectDeploy;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/** Client class for deploying {@link MetricCollector} */
public class MetricCollectorDeploy implements SystemSapphireObjectDeploy {
    private static Logger logger = Logger.getLogger(MetricCollectorDeploy.class.getName());

    @Override
    public void start(SapphireObjectServer server, String specFile) throws Exception {
        // TODO: retry twice if sapphire object deployment fails
        SapphireObjectID oid = server.createSapphireObject(getSpec(specFile));
        // use sys label to get deployed metric collector SO.
        // get sys selector
        Selector selector = MetricCollectorLabels.labels.asSelector();
        // acquire sapphire objects based on selector
        ArrayList<AppObjectStub> sapphireStubList = server.acquireSapphireObjectStub(selector);

        if (sapphireStubList.size() != 1) {
            throw new Exception("invalid list of stubs");
        }

        logger.info("Metric collector deployment Success!!!");
    }

    // TODO: define a common interface for system sapphire object spec processing
    private static String getSpec(String specFile) throws Exception {
        SapphireObjectSpec spec = MetricCollectorConst.SAPPHIRE_OBJECT_SPEC;
        if (!SystemSapphireObjectHandler.EMPTY_SO_SPEC.equals(specFile)) {
            File file = new File(specFile);
            List<String> lines = Files.readAllLines(file.toPath());
            String userDefinedSpec = String.join("\n", lines);

            spec = SapphireObjectSpec.fromYaml(userDefinedSpec);
        }

        Labels metricCollectorLabels =
                Labels.newBuilder()
                        .merge(spec.getSapphireObjectLabels())
                        .merge(MetricCollectorLabels.labels)
                        .create();

        spec.setSapphireObjectLabels(metricCollectorLabels);

        return spec.toString();
    }
}

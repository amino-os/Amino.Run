package sapphire.sysSapphireObjects.metricCollector;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import sapphire.app.SapphireObjectServer;
import sapphire.app.SapphireObjectSpec;
import sapphire.app.labelselector.Selector;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectID;
import sapphire.kernel.server.KernelServerImpl;

/** Client class for deploying {@link MetricCollector} */
public class MetricCollectorDeploy {
    private static Logger logger = Logger.getLogger(MetricCollectorDeploy.class.getName());

    public static void main(String[] args) throws Exception {
        SapphireObjectServer server = getSapphireObjectServer(args[0], args[1]);

        SapphireObjectID oid = server.createSapphireObject(getSpec());

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

    private static SapphireObjectServer getSapphireObjectServer(String omsIp, String omsPort)
            throws Exception {
        new KernelServerImpl(
                new InetSocketAddress("127.0.0.2", 11111),
                new InetSocketAddress(omsIp, Integer.parseInt(omsPort)));
        Registry registry = LocateRegistry.getRegistry(omsIp, Integer.parseInt(omsPort));
        SapphireObjectServer server = (SapphireObjectServer) registry.lookup("SapphireOMS");
        return server;
    }

    private static String getSpec() throws Exception {
        ClassLoader classLoader = new MetricCollectorDeploy().getClass().getClassLoader();
        File file = new File(classLoader.getResource("MetricCollector.yaml").getFile());
        List<String> lines = Files.readAllLines(file.toPath());
        String userDefinedSpec = String.join("\n", lines);

        SapphireObjectSpec spec = SapphireObjectSpec.fromYaml(userDefinedSpec);
        Map<String, String> userDefinedLabels = spec.getSapphireObjectLabels().getLabels();

        // add MetricCollector labels
        for (Map.Entry<String, String> sysLabels :
                MetricCollectorLabels.labels.getLabels().entrySet()) {
            userDefinedLabels.put(sysLabels.getKey(), sysLabels.getValue());
        }

        spec.getSapphireObjectLabels().setLabels(userDefinedLabels);

        return spec.toString();
    }
}

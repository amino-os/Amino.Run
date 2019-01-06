package sapphire.sysSapphireObjects.migrationScheduler;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import sapphire.app.SapphireObjectServer;
import sapphire.app.SapphireObjectSpec;
import sapphire.app.labelselector.Labels;
import sapphire.app.labelselector.Selector;
import sapphire.common.AppObjectStub;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.sysSapphireObjects.migrationScheduler.policy.Config;

/** Client class for deploying {@link MigrationScheduler} */
public class MigrationSchedulerDeploy {
    private static Logger logger = Logger.getLogger(MigrationScheduler.class.getName());

    public static void main(String[] args) throws Exception {
        SapphireObjectServer server = getSapphireObjectServer(args[0], args[1]);

        server.createSapphireObject(
                getSOSpec(),
                MigrationSchedulerPolicies.ExecutionPolicy,
                getMigrationSchedulerSpec(args[0], args[1]));

        // use sys label to get deployed metric collector SO.
        // get sys selector
        Selector selector = MigrationSchedulerLabels.labels.asSelector();
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

    private static SapphireObjectServer getSapphireObjectServer(String omsIp, String omsPort)
            throws Exception {
        new KernelServerImpl(
                new InetSocketAddress("127.0.0.2", 11111),
                new InetSocketAddress(omsIp, Integer.parseInt(omsPort)));
        Registry registry = LocateRegistry.getRegistry(omsIp, Integer.parseInt(omsPort));
        SapphireObjectServer server = (SapphireObjectServer) registry.lookup("SapphireOMS");
        return server;
    }

    private static String getSOSpec() throws Exception {
        ClassLoader classLoader = new MigrationSchedulerDeploy().getClass().getClassLoader();
        File file = new File(classLoader.getResource("MigrationScheduler.yaml").getFile());
        List<String> lines = Files.readAllLines(file.toPath());
        String userDefinedSpec = String.join("\n", lines);

        SapphireObjectSpec spec = SapphireObjectSpec.fromYaml(userDefinedSpec);
        Labels migrationSchedulerLabels =
                Labels.newBuilder()
                        .merge(spec.getSapphireObjectLabels())
                        .merge(MigrationSchedulerLabels.labels)
                        .create();

        spec.setSapphireObjectLabels(migrationSchedulerLabels);

        return spec.toString();
    }

    private static String getMigrationSchedulerSpec(String omsIP, String omsPort) throws Exception {
        ClassLoader classLoader = new MigrationSchedulerDeploy().getClass().getClassLoader();
        File file = new File(classLoader.getResource("MigrationSchedulerSpec.yaml").getFile());
        List<String> lines = Files.readAllLines(file.toPath());
        String userDefinedSpec = String.join("\n", lines);

        Config config = Config.fromYaml(userDefinedSpec);

        return config.toString();
    }
}

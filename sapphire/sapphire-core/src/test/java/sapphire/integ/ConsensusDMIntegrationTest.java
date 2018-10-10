package sapphire.integ;

import static java.lang.Thread.sleep;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.SapphireObjectID;
import sapphire.demo.KVStore;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

/**
 * Test <strong>complex</strong> deployment managers, e.g. Consensus, that require multiple kernel
 * servers are covered here.
 */
public class ConsensusDMIntegrationTest {
    private String ip = "127.0.0.1";
    private int omsPort = 22346;
    private String hostIp = "127.0.0.2";
    private int hostPort = 22333;
    private OMSServer oms;
    Process omsProcess = null;
    Process kernelServerProcess1 = null;
    Process kernelServerProcess2 = null;
    Process kernelServerProcess3 = null;

    @Before
    public void startOmsAndKernelServer() throws Exception {
        Runtime runtime = Runtime.getRuntime();

        /* Start OMS and kernel server as separate process and invoke rpc from app client */
        try {
            String cwd = System.getProperty("user.dir");
            String myJavaHome = System.getProperty("my_java_home");
            String javaExe = "java";
            if (myJavaHome != null) {
                javaExe = Paths.get(myJavaHome, "bin", "java").toString();
                System.out.println("java to call: " + javaExe);
            }
            String sapphireCore =
                    cwd
                            + "/../sapphire-core/build/libs/sapphire-core-1.0.0.jar:"
                            + cwd
                            + "/../dependencies/java.rmi/build/libs/java.rmi-1.0.0.jar:"
                            + cwd
                            + "/../dependencies/apache.harmony/build/libs/apache.harmony-1.0.0.jar:"
                            + cwd
                            + "/../sapphire-core/build/classes/java/test:"
                            + cwd
                            + "/../sapphire-core/libs/snakeyaml-1.23.jar:"
                            + cwd
                            + "/../../../../../consensus/graalvm-ce-1.0.0-rc6/jre/lib/boot/graal-sdk.jar";

            String omsCmd =
                    javaExe
                            + " -cp "
                            + sapphireCore
                            + " sapphire.oms.OMSServerImpl "
                            + ip
                            + " 22346 ";
            omsProcess = runtime.exec(omsCmd);
            sleep(1000);
            String ksCmd1 =
                    javaExe
                            + " -cp "
                            + sapphireCore
                            + " sapphire.kernel.server.KernelServerImpl "
                            + ip
                            + " 22345 "
                            + ip
                            + " 22346 ";
            kernelServerProcess1 = runtime.exec(ksCmd1);
            String ksCmd2 =
                    javaExe
                            + " -cp "
                            + sapphireCore
                            + " sapphire.kernel.server.KernelServerImpl "
                            + ip
                            + " 22344 "
                            + ip
                            + " 22346 ";
            kernelServerProcess2 = runtime.exec(ksCmd2);
            String ksCmd3 =
                    javaExe
                            + " -cp "
                            + sapphireCore
                            + " sapphire.kernel.server.KernelServerImpl "
                            + ip
                            + " 22343 "
                            + ip
                            + " 22346 ";
            kernelServerProcess3 = runtime.exec(ksCmd3);
            sleep(500);

            Registry registry = LocateRegistry.getRegistry(ip, omsPort);
            oms = (OMSServer) registry.lookup("SapphireOMS");

            new KernelServerImpl(
                    new InetSocketAddress(hostIp, hostPort), new InetSocketAddress(ip, omsPort));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Test sapphire object specifications in <code>src/test/resources/specs</code> directory.
     *
     * @throws Exception
     */
    @Test
    public void testDMs() throws Exception {
        File[] files = getResourceFiles("specs/complex-dm/");
        for (File f : files) {
            if (!f.getName().endsWith("yaml")) {
                continue;
            }

            SapphireObjectSpec spec = readSapphireSpec(f);
            System.out.println("Running test for DM: " + spec.getDmList());
            runTest(spec);
            System.out.println("Test passed");
            if (omsProcess != null) {
                omsProcess.destroy();
            }
            if (kernelServerProcess1 != null) {
                kernelServerProcess1.destroy();
            }
            if (kernelServerProcess2 != null) {
                kernelServerProcess2.destroy();
            }
            if (kernelServerProcess3 != null) {
                kernelServerProcess3.destroy();
            }
        }
    }

    /**
     * Returns files from given subdirectory in resources.
     *
     * @param folder the path relative to src/test/resources
     * @return a list of {@link File}s one for each file in src/test/resources/folder
     */
    private static File[] getResourceFiles(String folder) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(folder);
        String path = url.getPath();
        return new File(path).listFiles();
    }

    private SapphireObjectSpec readSapphireSpec(File file) throws Exception {
        List<String> lines = Files.readAllLines(file.toPath());
        String yamlStr = String.join("\n", lines);
        SapphireObjectSpec spec = SapphireObjectSpec.fromYaml(yamlStr);
        return spec;
    }

    private void runTest(SapphireObjectSpec spec) throws Exception {
        String key = "k1";
        String value = "v1";

        SapphireObjectID sapphireObjId = oms.createSapphireObject(spec.toString());
        sleep(3000);
        KVStore store = (KVStore) oms.acquireSapphireObjectStub(sapphireObjId);
        store.set(key, value);
        Assert.assertEquals(value, store.get(key));
    }
}

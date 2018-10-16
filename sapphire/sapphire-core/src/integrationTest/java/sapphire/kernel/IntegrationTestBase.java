package sapphire.kernel;

import static java.lang.Thread.sleep;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import sapphire.app.SapphireObjectSpec;

public class IntegrationTestBase {
    public static String omsIp = "127.0.0.1";
    public static int omsPort = 22346;
    public static String hostIp = "127.0.0.2";
    public static int hostPort = 22333;
    private static Process omsProcess = null;
    private static Process kernelServerProcess1 = null;
    private static Process kernelServerProcess2 = null;
    private static Process kernelServerProcess3 = null;

    public static void startOmsAndKernelServers(String KernelServerRegion) throws Exception {
        Runtime runtime = Runtime.getRuntime();

        /* Start OMS and kernel server as separate process and invoke rpc from app client */
        String myJavaHome = System.getProperty("DCAP_JAVA_HOME");
        String javaExe = "java";
        if (myJavaHome != null) {
            javaExe = Paths.get(myJavaHome, "bin", "java").toString();
            System.out.println("java to call: " + javaExe);
        }

        String classPath = System.getProperty("java.class.path");

        String omsCmd =
                javaExe
                        + " -cp "
                        + classPath
                        + " sapphire.oms.OMSServerImpl "
                        + omsIp
                        + " 22346 ";
        omsProcess = runtime.exec(omsCmd);
        sleep(1000);

        String ksCmd1 =
                javaExe
                        + " -cp "
                        + classPath
                        + " sapphire.kernel.server.KernelServerImpl "
                        + omsIp
                        + " 22345 "
                        + omsIp
                        + " 22346 "
                        + " "
                        + KernelServerRegion;
        kernelServerProcess1 = runtime.exec(ksCmd1);

        String ksCmd2 =
                javaExe
                        + " -cp "
                        + sapphireCore
                        + " sapphire.kernel.server.KernelServerImpl "
                        + omsIp
                        + " 22344 "
                        + omsIp
                        + " 22346 "
                        + " "
                        + KernelServerRegion;
        kernelServerProcess2 = runtime.exec(ksCmd2);

        String ksCmd3 =
                javaExe
                        + " -cp "
                        + sapphireCore
                        + " sapphire.kernel.server.KernelServerImpl "
                        + omsIp
                        + " 22343 "
                        + omsIp
                        + " 22346 "
                        + " "
                        + KernelServerRegion;
        kernelServerProcess3 = runtime.exec(ksCmd3);
        sleep(500);
    }

    /**
     * Returns files from given subdirectory in resources.
     *
     * @param fileName the path relative to src/test/resources
     * @return a {@link File} in src/test/resources/folder
     */
    public static File getResourceFile(String fileName) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        File file = new File(loader.getResource(fileName).getFile());
        return file;
    }

    public static SapphireObjectSpec readSapphireSpec(File file) throws Exception {
        List<String> lines = Files.readAllLines(file.toPath());
        String yamlStr = String.join("\n", lines);
        SapphireObjectSpec spec = SapphireObjectSpec.fromYaml(yamlStr);
        return spec;
    }

    public static void killOmsAndKernelServers() {
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

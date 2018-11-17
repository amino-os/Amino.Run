package sapphire.kernel;

import static java.lang.Thread.sleep;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.util.List;
import sapphire.app.SapphireObjectSpec;

public class IntegrationTestBase {
    public static String omsIp = "127.0.0.1";
    public static String ksIp = omsIp;
    public static int omsPort = 22346;
    public static int[] ksPort = {22345, 22344, 22343};
    public static String hostIp = "127.0.0.2";
    public static int hostPort = 22333;
    private static Process omsProcess = null;
    private static Process[] kernelServerProcess = {null, null, null};

    static void waitForSockListen(String ip, int port) {

        Socket socket = null;
        while (socket == null) {
            System.out.printf("Waiting for socket %d\n!", port);
            try {
                socket = new Socket(ip, port);
            } catch (IOException e) {
                try {
                    sleep(1000);
                } catch (InterruptedException e1) {
                    System.out.println(e1.toString());
                }
            }
        }
    }

    static void waitForSockClose(String ip, int port) {
        Socket socket = null;
        try {
            socket = new Socket(ip, port);
        } catch (IOException e) {
            System.out.println(e.toString());
        }
        while (socket != null) {
            try {
                System.out.printf("Waiting for socket %d\n!", port);
                socket = new Socket(ip, port);
                try {
                    sleep(100);
                } catch (InterruptedException e1) {
                    System.out.println(e1.toString());
                }
            } catch (IOException e) {
                break;
            }
        }
    }

    /**
     * start the OMS process
     *
     * @param ip
     * @param port
     * @return The OMS Process
     * @throws java.io.IOException if the process failed to start.
     */
    public static Process startOms(String ip, int port) throws java.io.IOException {
        String cmd =
                "java  -cp "
                        + System.getProperty("java.class.path")
                        + " sapphire.oms.OMSServerImpl "
                        + ip
                        + " "
                        + port;
        Process process = Runtime.getRuntime().exec(cmd);
        waitForSockListen(ip, port);
        return process;
    }

    /**
     * start a kernel server process
     *
     * @param ip address to listen on
     * @param port port to listen on
     * @param omsIp address of OMS
     * @param omsPort port of OMS
     * @param region region to assign to this kernel server
     * @param labels labels to assign to this kernel server
     * @return The kernel server process
     * @throws java.io.IOException if the process fails to start
     */
    public static Process startKernelServer(
            String ip, int port, String omsIp, int omsPort, String region, String[] labels)
            throws java.io.IOException {
        String cmd =
                "java  -cp "
                        + System.getProperty("java.class.path")
                        + " sapphire.kernel.server.KernelServerImpl "
                        + ip
                        + " "
                        + port
                        + " "
                        + omsIp
                        + " "
                        + omsPort
                        + " "
                        + region
                        + " ";
        if (null != labels && labels.length > 0) {
            cmd += "--labels " + String.join(",", labels);
        }
        System.out.printf("Starting kernel server with command line \'%s\'\n", cmd);
        Process process = Runtime.getRuntime().exec(cmd);
        waitForSockListen(ip, port);
        return process;
    }

    public static void startOms() throws java.io.IOException {
        omsProcess = startOms(omsIp, omsPort);
    }

    public static void startOmsAndKernelServers(String kernelServerRegion) throws Exception {
        startOmsAndKernelServers(kernelServerRegion, null);
    }

    // @Deprecated "Please use {@link #startOms()} and {@link startKernelServer()} instead"
    public static void startOmsAndKernelServers(String kernelServerRegion, String labels[])
            throws Exception {
        omsProcess = startOms(omsIp, omsPort);
        waitForSockListen(omsIp, omsPort);

        for (int i = 0; i < ksPort.length; i++) {
            kernelServerProcess[i] =
                    startKernelServer(ksIp, ksPort[i], omsIp, omsPort, kernelServerRegion, labels);
            waitForSockListen(ksIp, ksPort[i]);
        }
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
            waitForSockClose(omsIp, omsPort);
        }

        for (int i = 0; i < ksPort.length; i++) {
            if (kernelServerProcess[i] != null) {
                kernelServerProcess[i].destroy();
                waitForSockClose(ksIp, ksPort[i]);
            }
        }
    }
}

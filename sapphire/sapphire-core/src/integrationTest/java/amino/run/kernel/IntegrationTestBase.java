package amino.run.kernel;

import static java.lang.Thread.sleep;

import amino.run.app.MicroServiceSpec;
import amino.run.demo.KVStore;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.oms.OMSServerImpl;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.List;
import org.junit.Assert;

public class IntegrationTestBase {
    public static String omsIp = "127.0.0.1";
    public static String ksIp = omsIp;
    public static int omsPort = 22346;
    public static int[] ksPort = {22345, 22344, 22343};
    public static String hostIp = "127.0.0.2";
    public static int hostPort = 22333;
    private static Process omsProcess = null;
    private static Process[] kernelServerProcess = {null, null, null};
    private static String javaHome = System.getProperty("java.home");
    private static String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
    private static String classpath = System.getProperty("java.class.path");

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
     * Wait the specified time for a key in the store to have a desired value
     *
     * @param store
     * @param key
     * @param desiredValue
     * @param timeoutMs How long to try for, in ms. If <= 0, use the default.
     * @throws InterruptedException
     */
    public static void waitForValue(KVStore store, String key, Object desiredValue, long timeoutMs)
            throws InterruptedException {
        long DEFAULT_TIMEOUT_MS = 1000;
        long DEFAULT_SLEEP_BETWEEN_TRIES_MS = 100;
        if (timeoutMs <= 0) {
            timeoutMs = DEFAULT_TIMEOUT_MS;
        }
        long startTime = System.currentTimeMillis();
        Object value;
        do {
            sleep(DEFAULT_SLEEP_BETWEEN_TRIES_MS);
            value = store.get(key);
        } while (!desiredValue.equals(value)
                && (System.currentTimeMillis() - startTime) < timeoutMs);
        Assert.assertEquals(desiredValue, value);
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
        String className = OMSServerImpl.class.getName();

        String[] args =
                new String[] {
                    javaBin,
                    "-cp",
                    classpath,
                    className,
                    OMSServerImpl.OMS_IP_OPT,
                    ip,
                    OMSServerImpl.OMS_PORT_OPT,
                    Integer.toString(port)
                };

        ProcessBuilder builder = new ProcessBuilder(args);
        Process process = builder.inheritIO().start();

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
     * @param labels labels to assign to this kernel server
     * @return The kernel server process
     * @throws java.io.IOException if the process fails to start
     */
    public static Process startKernelServer(
            String ip, int port, String omsIp, int omsPort, String labels)
            throws java.io.IOException {
        String className = KernelServerImpl.class.getName();
        if (labels == null) {
            labels = "";
        }

        String[] args =
                new String[] {
                    javaBin,
                    "-cp",
                    classpath,
                    className,
                    KernelServerImpl.KERNEL_SERVER_IP_OPT,
                    ip,
                    KernelServerImpl.KERNEL_SERVER_PORT_OPT,
                    Integer.toString(port),
                    OMSServerImpl.OMS_IP_OPT,
                    omsIp,
                    OMSServerImpl.OMS_PORT_OPT,
                    Integer.toString(omsPort),
                    KernelServerImpl.LABEL_OPT,
                    labels
                };

        System.out.printf(
                "Starting kernel server at %s:%d with command line \'%s\'\n",
                ip, port, String.join(",", args));
        ProcessBuilder builder = new ProcessBuilder(args);
        Process process = builder.inheritIO().start();

        waitForSockListen(ip, port);
        return process;
    }

    public static void startOms() throws java.io.IOException {
        omsProcess = startOms(omsIp, omsPort);
    }

    // @Deprecated "Please use {@link #startOms()} and {@link startKernelServer()} instead"
    public static void startOmsAndKernelServers(String labels) throws Exception {
        omsProcess = startOms(omsIp, omsPort);
        waitForSockListen(omsIp, omsPort);

        for (int i = 0; i < ksPort.length; i++) {
            kernelServerProcess[i] = startKernelServer(ksIp, ksPort[i], omsIp, omsPort, labels);
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

    public static MicroServiceSpec readMicroServiceSpec(File file) throws Exception {
        List<String> lines = Files.readAllLines(file.toPath());
        String yamlStr = String.join("\n", lines);
        MicroServiceSpec spec = MicroServiceSpec.fromYaml(yamlStr);

        return spec;
    }

    public static void killOmsAndKernelServers() {

        for (int i = 0; i < ksPort.length; i++) {
            if (kernelServerProcess[i] != null) {
                kernelServerProcess[i].destroy();
                waitForSockClose(ksIp, ksPort[i]);
            }
        }

        if (omsProcess != null) {
            omsProcess.destroy();
            waitForSockClose(omsIp, omsPort);
        }
    }
}

package amino.run.kernel;

import static java.lang.Thread.sleep;

import amino.run.app.MicroServiceSpec;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.oms.OMSServerImpl;
import java.io.*;
import java.net.Socket;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

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
                System.out.printf("Waiting for socket close %d\n!", port);
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

        System.out.printf(
                "Starting OMS at %s:%d with command line \'%s\'\n",
                ip, port, StringUtils.join(args, ","));

        Process process = new ProcessBuilder(args).start();

        // Must read the command STDOUT otherwise command is hanging.
        // 1.7 Java version has processBuilder.inhertIO() will read the logs and print on console.
        StreamReader error = new StreamReader(process.getErrorStream(), "ERROR");
        StreamReader output = new StreamReader(process.getInputStream(), "");

        error.start();
        output.start();

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
                ip, port, StringUtils.join(args, ","));

        Process process = new ProcessBuilder(args).start();

        // Must read the command STDOUT otherwise command is hanging.
        // 1.7 Java version has processBuilder.inhertIO() will read the logs and print on console.
        StreamReader error = new StreamReader(process.getErrorStream(), "ERROR");
        StreamReader output = new StreamReader(process.getInputStream(), "");

        error.start();
        output.start();

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
        List<String> lines = FileUtils.readLines(file);
        String yamlStr = StringUtils.join(lines, "\n");
        return MicroServiceSpec.fromYaml(yamlStr);
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

/**
 * This class is created for reading the command output and print on console. Must read the command
 * STDOUT otherwise command is hanging. 1.7 Java version has processBuilder.inhertIO() will read the
 * logs and print on console.
 */
class StreamReader extends Thread {
    InputStream is;
    String type;

    StreamReader(InputStream is, String type) {
        this.is = is;
        this.type = type;
    }

    @Override
    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                // TODO: stream output is not printing in console currently. give --info for debug
                // ex: [./gradlew clean build --info]
                System.out.println(type + line);
            }

        } catch (IOException ioe) {
            System.out.println(ioe.toString());
        }
    }
}

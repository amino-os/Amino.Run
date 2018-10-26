package sapphire.kernel;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertNotNull;
import static java.lang.System.out;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

public class IntegrationTestBase {
    private int basePort;  // Must be set by derived classes to prevent port overlaps
    private int omsPort;
    private int baseKernelServerPort;
    public static final String omsIp = "127.0.0.1",
            ksIp = omsIp,
            appIp = omsIp;
    public static int DEFAULT_KERNEL_SERVER_COUNT = 3;
    public static String DEFAULT_KERNEL_SERVER_REGION = "";
    private static Collection<Integer> kernelServerPort = new ArrayList<>();
    private static Process omsProcess = null;
    private static Collection<Process> kernelServerProcess = new ArrayList<>();
    private OMSServer oms;

    private IntegrationTestBase() {} // Hide default constructor - must call constructor below.

    protected IntegrationTestBase(int basePort) throws Exception {
        bootstrap(basePort);
    }

    // TODO: Remove
    // int getOmsPort() { return omsPort; }

    protected OMSServer getOms() { return this.oms; }

    private void bootstrap(int basePort) throws Exception {
        if (this.basePort != 0) {
            throw new ExceptionInInitializerError("IntegrationTestBase: Attempt to bootstrap more than once.");
        }
        this.basePort = basePort;
        omsPort = basePort + 1;
        baseKernelServerPort = omsPort + 1;
        startOmsAndKernelServers(baseKernelServerPort, DEFAULT_KERNEL_SERVER_COUNT, "r1");
    }

    @After
    public void cleanUp() throws InterruptedException {
        killOmsAndKernelServers();
    }

    @Before
    public void setUp() throws Exception {
        Registry registry = LocateRegistry.getRegistry(omsIp, omsPort);
        assertNotNull("RMI registry must not be null", registry);
        oms = (OMSServer) registry.lookup("SapphireOMS");
        assertNotNull("OMS must not be null", oms);
    }

    protected KernelServer createEmbeddedKernelServer(int port) {
        KernelServer ks =
                new KernelServerImpl(
                        new InetSocketAddress(appIp, port),
                        new InetSocketAddress(omsIp, omsPort));
        assertNotNull("Kernel server must not be null", ks);
        return ks;
    }
    static void waitForSockListen(String ip, int port) throws java.lang.InterruptedException {
        long WAIT_TIMEOUT_MS = 5000;
        long startTime = System.currentTimeMillis();
        Socket socket = null;
        while (socket == null) {
            try {
                socket = new Socket(ip, port);
            } catch (IOException e) {
                if (System.currentTimeMillis() - startTime < WAIT_TIMEOUT_MS) {
                    // Do nothing and try again
                    sleep(1000);
                }else {
                    new Error(e);
                }
            }
        }
    }

    /**
     * Wait for and object to be available on OMS
     * @param objId The ID of the Object
     * @return The stub of the object, if found
     * @throws SapphireObjectNotFoundException if, after retrying, the object could still not be found.
     * @throws java.rmi.RemoteException if OMs could not be contacted
     */
    public AppObjectStub waitForObjectStub(SapphireObjectID objId) throws SapphireObjectNotFoundException, java.rmi.RemoteException, java.lang.InterruptedException {
        final long WAIT_TIMEOUT_MS = 5000;
        long startTime = System.currentTimeMillis();
        AppObjectStub stub = null;
        while(stub == null) {
            try {
                stub = getOms().acquireSapphireObjectStub(objId);
            } catch (SapphireObjectNotFoundException e) {
                if (System.currentTimeMillis() - startTime < WAIT_TIMEOUT_MS) {
                    // ignore and try again
                    sleep(100);
                } else {
                    throw e;
                }
            }
        }
        return stub;
    }

    static void waitForSockClose(String ip, int port) throws InterruptedException{
        long WAIT_TIMEOUT_MS = 5000;
        long startTime = System.currentTimeMillis();
        Socket socket = null;
        do {
            try {
                socket = new Socket(ip, port);
                socket.close();
            } catch (IOException e) {
                socket = null;
            }
        } while(socket != null  && System.currentTimeMillis() - startTime < WAIT_TIMEOUT_MS);
        if (socket != null) {
            throw new InterruptedException(String.format("Timed out after %d ms waiting for socket %s:%d to close", WAIT_TIMEOUT_MS, ip, port));
        }
    }

    /**
     * Start OMS and Kernel Servers
     * @param kernelServerBasePort Port on which first kernel server will listen.  Increasing port numbers additional kernel servers.
     * @param kernelServerCount Count of kernel servers to be started.
     * @param KernelServerRegion Region in which kernel servers must be started.
     * @throws Exception
     */
    public void startOmsAndKernelServers(int kernelServerBasePort, int kernelServerCount, String KernelServerRegion) throws Exception {
        Runtime runtime = Runtime.getRuntime();
        /* Start OMS and kernel servers as separate processes */
        String myJavaHome = System.getProperty("DCAP_JAVA_HOME");
        String javaExe = "java";
        if (myJavaHome != null) {
            javaExe = Paths.get(myJavaHome, "bin", "java").toString();
            System.out.println("java to call: " + javaExe);
        }

        String classPath = System.getProperty("java.class.path");

        String omsCmd =
                javaExe + " -cp " + classPath + " sapphire.oms.OMSServerImpl " + omsIp + " " + omsPort + " ";
        omsProcess = runtime.exec(omsCmd);
        waitForSockListen(omsIp, omsPort);

        int ksPort = kernelServerBasePort;
        for(int i = 0; i < kernelServerCount; i++) {
            String ksCmd =
                    javaExe
                            + " -cp "
                            + classPath
                            + " sapphire.kernel.server.KernelServerImpl "
                            + ksIp
                            + " " + ksPort + " "
                            + omsIp
                            + " " + omsPort + " "
                            + KernelServerRegion;
            out.printf("Running kernel server:%d\n", i);
            kernelServerProcess.add(runtime.exec(ksCmd));
            kernelServerPort.add(ksPort);
            waitForSockListen(ksIp, ksPort);
            ksPort++;
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

    public void killOmsAndKernelServers() throws InterruptedException {
        if (omsProcess != null) {
            omsProcess.destroy();
            waitForSockClose(omsIp, omsPort);
        }
        Iterator<Integer> ksPort = kernelServerPort.iterator();
        for (Process ksProc: kernelServerProcess) {
            if (ksProc != null) {
                ksProc.destroy();
                waitForSockClose(ksIp, ksPort.next());
            }
        }
    }
}

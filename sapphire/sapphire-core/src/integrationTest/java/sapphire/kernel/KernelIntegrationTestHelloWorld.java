package sapphire.kernel;

import static java.lang.Thread.sleep;

import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import sapphire.appexamples.helloworld.HelloWorld;
import sapphire.common.SapphireObjectID;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.oms.OMSServerImpl;

/** Tests the SO creation process in Kernel Server and OMS. */
// note: running in IDE may fail due to java path issue. Please ensure graalVM is the default java in that case.
public class KernelIntegrationTestHelloWorld {
    @Test
    public void testCreateSapphireObject() throws Exception {
        String world = "Disney";
        String omsIp = "127.0.0.1";
        int omsPort = 22346;
        String kstIp = "127.0.0.1";
        int ksPort = 22345;
        String hostIp = "127.0.0.2";
        int hostPort = 22333;

        OMSServerImpl.main(new String[] {omsIp, String.valueOf(omsPort)});
        KernelServerImpl.main(
                new String[] {kstIp, String.valueOf(ksPort), omsIp, String.valueOf(omsPort), "r1"});

        Registry registry = LocateRegistry.getRegistry(omsIp, omsPort);
        OMSServer server = (OMSServer) registry.lookup("SapphireOMS");

        KernelServer nodeServer =
                new KernelServerImpl(
                        new InetSocketAddress(hostIp, hostPort),
                        new InetSocketAddress(omsIp, omsPort));

        SapphireObjectID sapphireObjId =
                server.createSapphireObject("sapphire.appexamples.helloworld.HelloWorld", world);
        HelloWorld helloWorld = (HelloWorld) server.acquireSapphireObjectStub(sapphireObjId);
        Assert.assertEquals("Hi " + world, helloWorld.sayHello());
        server.deleteSapphireObject(sapphireObjId);
    }

    // TODO: This test is broken!
    @Ignore
    @Test
    public void testSOCreation() {
    public void testSOCreation() throws Exception {
        String ip = "127.0.0.1";
        String[] appHost = new String[] {ip, "22346", "10.0.2.15", "22344"};
        Runtime runtime = Runtime.getRuntime();
        Process omsProcess = null;
        Process kernelServerProcess = null;

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
                            + "/../examples/helloworld/build/libs/helloworld.jar ";

            String omsCmd =
                    javaExe + " -cp " + sapphireCore + " sapphire.oms.OMSServerImpl " + ip + " 22346 ";
            omsProcess = runtime.exec(omsCmd);
            sleep(500);
            String ksCmd =
                    javaExe
                            +" -cp "
                            + sapphireCore
                            + " sapphire.kernel.server.KernelServerImpl "
                            + ip
                            + " 22345 "
                            + ip
                            + " 22346 ";
            kernelServerProcess = runtime.exec(ksCmd);
            sleep(1000);

            Registry registry = LocateRegistry.getRegistry(ip, Integer.parseInt("22346"));
            OMSServer server = (OMSServer) registry.lookup("SapphireOMS");

            KernelServer appKernel =
                    new KernelServerImpl(
                            new InetSocketAddress(appHost[2], Integer.parseInt(appHost[3])),
                            new InetSocketAddress(appHost[0], Integer.parseInt(appHost[1])));

            SapphireObjectID sapphireObjId =
                    server.createSapphireObject("sapphire.appexamples.helloworld.HelloWorld");
            HelloWorld helloWorld = (HelloWorld) server.acquireSapphireObjectStub(sapphireObjId);

            System.out.println("result.." + helloWorld.sayHello());
            Assert.assertTrue(helloWorld.sayHello().startsWith("Hi"));
        } finally {
            if (omsProcess != null) {
                omsProcess.destroy();
            }
            if (kernelServerProcess != null) {
                kernelServerProcess.destroy();
            }
        }
    }
}

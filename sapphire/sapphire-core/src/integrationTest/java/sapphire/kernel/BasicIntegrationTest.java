package sapphire.kernel;

import static java.lang.Thread.sleep;

import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.junit.Assert;
import org.junit.Test;
import sapphire.app.DMSpec;
import sapphire.app.Language;
import sapphire.app.SapphireObjectSpec;
import sapphire.appexamples.helloworld.HelloWorld;
import sapphire.common.SapphireObjectID;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.oms.OMSServerImpl;
import sapphire.policy.atleastoncerpc.AtLeastOnceRPCPolicy;

/** Tests the SO creation process in Kernel Server and OMS. */
public class BasicIntegrationTest {

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

        SapphireObjectSpec spec =
                SapphireObjectSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("sapphire.appexamples.helloworld.HelloWorld")
                        .addDMSpec(
                                DMSpec.newBuilder()
                                        .setName(AtLeastOnceRPCPolicy.class.getName())
                                        .create())
                        .create();

        SapphireObjectID sapphireObjId = server.createSapphireObject(spec.toString(), world);
        HelloWorld helloWorld = (HelloWorld) server.acquireSapphireObjectStub(sapphireObjId);
        Assert.assertEquals("Hi " + world, helloWorld.sayHello());
        server.deleteSapphireObject(sapphireObjId);
    }

    // note: running in IDE may fail due to java path issue. Please ensure graalVM is the default
    // java in that case.
    @Test
    public void testSOCreation() throws Exception {
        String ip = "127.0.0.1";
        String[] appHost = new String[] {ip, "22446", "10.0.2.15", "22444"};
        Runtime runtime = Runtime.getRuntime();
        Process omsProcess = null;
        Process kernelServerProcess = null;

        /* Start OMS and kernel server as separate process and invoke rpc from app client */
        try {
            String cwd = System.getProperty("user.dir");
            String javaHome = System.getProperty("DCAP_JAVA_HOME");
            String javaExe = "java";
            if (javaHome != null) {
                javaExe = Paths.get(javaHome, "bin", "java").toString();
                System.out.println("java to call: " + javaExe);
            }

            String classPath = System.getProperty("java.class.path");
            String omsCmd =
                    javaExe + " -cp " + classPath + " sapphire.oms.OMSServerImpl " + ip + " 22446 ";
            omsProcess = runtime.exec(omsCmd);
            sleep(500);
            String ksCmd =
                    javaExe
                            + " -cp "
                            + classPath
                            + " sapphire.kernel.server.KernelServerImpl "
                            + ip
                            + " 22445 "
                            + ip
                            + " 22446 ";
            kernelServerProcess = runtime.exec(ksCmd);
            sleep(1000);

            Registry registry = LocateRegistry.getRegistry(ip, Integer.parseInt("22446"));
            OMSServer server = (OMSServer) registry.lookup("SapphireOMS");

            KernelServer appKernel =
                    new KernelServerImpl(
                            new InetSocketAddress(appHost[2], Integer.parseInt(appHost[3])),
                            new InetSocketAddress(appHost[0], Integer.parseInt(appHost[1])));

            SapphireObjectSpec spec =
                    SapphireObjectSpec.newBuilder()
                            .setLang(Language.java)
                            .setJavaClassName("sapphire.appexamples.helloworld.HelloWorld")
                            .addDMSpec(
                                    DMSpec.newBuilder()
                                            .setName(AtLeastOnceRPCPolicy.class.getName())
                                            .create())
                            .create();
            SapphireObjectID sapphireObjId = server.createSapphireObject(spec.toString());
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

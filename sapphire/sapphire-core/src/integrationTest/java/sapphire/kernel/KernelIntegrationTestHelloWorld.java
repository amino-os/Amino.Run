package sapphire.kernel;

import static java.lang.Thread.sleep;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.junit.Assert;
import org.junit.Test;
import sapphire.appexamples.helloworld.HelloWorld;
import sapphire.common.SapphireObjectID;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

/** Tests the SO creation process in Kernel Server and OMS. */
public class KernelIntegrationTestHelloWorld {

    @Test
    public void testSOCreation() {
        String ip = "127.0.0.1";
        String[] appHost = new String[] {ip, "22346", "10.0.2.15", "22344"};
        Runtime runtime = Runtime.getRuntime();
        Process omsProcess = null;
        Process kernelServerProcess = null;

        /* Start OMS and kernel server as separate process and invoke rpc from app client */
        try {
            String cwd = System.getProperty("user.dir");
            String sapphireCore =
                    cwd
                            + "/sapphire-core/build/libs/sapphire-core-1.0.0.jar:"
                            + cwd
                            + "/dependencies/java.rmi/build/libs/java.rmi-1.0.0.jar:"
                            + cwd
                            + "/dependencies/apache.harmony/build/libs/apache.harmony-1.0.0.jar:"
                            + cwd
                            + "/examples/helloworld/build/libs/helloworld.jar ";

            String omsCmd =
                    "java -cp " + sapphireCore + " sapphire.oms.OMSServerImpl " + ip + " 22346 ";
            omsProcess = runtime.exec(omsCmd);
            sleep(500);
            String ksCmd =
                    "java -cp "
                            + sapphireCore
                            + " sapphire.kernel.server.KernelServerImpl "
                            + ip
                            + " 22345 "
                            + ip
                            + " 22346 ";
            kernelServerProcess = runtime.exec(ksCmd);
            sleep(500);

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
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (omsProcess != null) {
            omsProcess.destroy();
        }
        if (kernelServerProcess != null) {
            kernelServerProcess.destroy();
        }
    }
}

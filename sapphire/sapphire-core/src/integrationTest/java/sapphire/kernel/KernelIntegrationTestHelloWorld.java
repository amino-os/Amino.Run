package sapphire.kernel;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.junit.Assert;
import org.junit.Test;
import sapphire.appexamples.helloworld.HelloWorld;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.oms.OMSServerImpl;

/** Tests the SO creation process in Kernel Server and OMS. */
public class KernelIntegrationTestHelloWorld {

    @Test
    public void testSOCreation() {

        try {
            String ip = "127.0.0.1";
            String[] appHost = new String[] {ip, "22346", "10.0.2.15", "22344"};

            OMSServerImpl.main(
                    new String[] {ip, "22346", "sapphire.appexamples.helloworld.HelloWorld"});

            KernelServerImpl.main(new String[] {ip, "22345", ip, "22346"});

            Registry registry = LocateRegistry.getRegistry(ip, Integer.parseInt("22346"));
            OMSServer server = (OMSServer) registry.lookup("SapphireOMS");

            HelloWorld helloWorld = (HelloWorld) server.getAppEntryPoint();

            KernelServer appKernel =
                    new KernelServerImpl(
                            new InetSocketAddress(appHost[2], Integer.parseInt(appHost[3])),
                            new InetSocketAddress(appHost[0], Integer.parseInt(appHost[1])));

            System.out.println("result.." + helloWorld.sayHello());
            Assert.assertTrue(helloWorld.sayHello().startsWith("Hi"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package sapphire.kernel;

import org.junit.Test;

/**
 * Tests the SO creation process in Kernel Server and OMS.
 */
public class KernelIntegrationTest {

    @Test
    public void testSOCreation() {
//        // The following codes do not work. It just describe the
//        // general idea of the test.
//
//        // 1. Start OMS
//        OMSServerImpl.main(new String[]{"217.0.0.1", "22346", "sapphire.appexamples.helloworld.HelloWorld"});
//
//        // 2. Start Kernel Server
//        KernelServerImpl.main(new String[]{"217.0.0.1", "22345", "217.0.0.1", "22346", "us-east"});
//
//        // 3. Get HelloWorld Reference
//        Registry registry = LocateRegistry.getRegistry("args[0]", 0);
//        OMSServer server = (OMSServer) registry.lookup("SapphireOMS");
//        HelloWorld helloWorld = (HelloWorld) server.getAppEntryPoint();
//
//        // 4. Verify result
//        Assert.assertTrue(helloWorld.sayHello().startsWith("Hi"));
    }
}

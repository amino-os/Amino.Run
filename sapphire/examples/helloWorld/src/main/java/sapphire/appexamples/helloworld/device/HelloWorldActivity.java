package sapphire.appexamples.helloworld.device;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import sapphire.appexamples.helloworld.app.HelloWorld;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

public class HelloWorldActivity {

	public HelloWorldActivity() {
	}
	
	public static void main(String[] args) {
		Registry registry;
		try {
			registry = LocateRegistry.getRegistry(args[0],Integer.parseInt(args[1]));
			OMSServer server = (OMSServer) registry.lookup("SapphireOMS");
			System.out.println(server);
			HelloWorld hw = (HelloWorld) server.getAppEntryPoint();
			System.out.println("Received hw: " + hw);

			KernelServer nodeServer = new KernelServerImpl(new InetSocketAddress(args[2], Integer.parseInt(args[3])), new InetSocketAddress(args[0], Integer.parseInt(args[1])));

			System.out.println(hw.printName("testClient"));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

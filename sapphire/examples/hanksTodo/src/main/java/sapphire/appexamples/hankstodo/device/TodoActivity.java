package sapphire.appexamples.hankstodo.device;

import junit.framework.Assert;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Level;

import sapphire.appexamples.hankstodo.app.TodoList;
import sapphire.appexamples.hankstodo.app.TodoListManager;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

public class TodoActivity {

	public TodoActivity() {	
	}
	
	public static void main(String[] args) {
		String ListName = "New List 1";
		String Do1_1 = "Do this 1.1";
		String Do1_2 = "Do this 1.2";
		String Do2_1 = "Do that 2.1";
		String Do2_2 = "Do that 2.2";

		Registry registry;
		AndroidLoggingHandler.reset(new AndroidLoggingHandler());
		java.util.logging.Logger.getLogger("my.category").setLevel(Level.FINEST);
		try {
			registry = LocateRegistry.getRegistry(args[0],Integer.parseInt(args[1]));
			OMSServer server = (OMSServer) registry.lookup("SapphireOMS");
			System.out.println(server);
			TodoListManager tlm = (TodoListManager) server.getAppEntryPoint();
            System.out.println("Received tlm: " + tlm);
            
            KernelServer nodeServer = new KernelServerImpl(new InetSocketAddress(args[2], Integer.parseInt(args[3])), new InetSocketAddress(args[0], Integer.parseInt(args[1])));

			TodoList td1 = tlm.newTodoList(ListName);
			System.out.println("new to do list for 1");

			// Add to-do items.
            td1.addToDo("1", Do1_1);
			System.out.println(Do1_1);
            td1.addToDo("1", Do1_2);
			System.out.println(Do1_2);
            td1.addToDo("2", Do2_1);
			System.out.println(Do2_1);
            td1.addToDo("2", Do2_2);
			System.out.println(Do2_2);

			// Retrieve to-do items.
            TodoList getTd1 = tlm.getToDoList(ListName);
            String testTdString2 = getTd1.getToDoString("2");
            String expectedTdString2 = Do2_1 + " : " + Do2_2+ " : ";
			System.out.println("Expected testTdString for 2: " + expectedTdString2);
			System.out.println("Actual testTdString for 2: " + testTdString2);

            String testTdString1 = getTd1.getToDoString("1");
			String expectedTdString1 = Do1_1 + " : " + Do1_2+ " : ";
			System.out.println("Expected testTdString for 1: " + expectedTdString1);
			System.out.println("Actual testTdString for 1: " + testTdString1);

            tlm.doSomething("do something");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

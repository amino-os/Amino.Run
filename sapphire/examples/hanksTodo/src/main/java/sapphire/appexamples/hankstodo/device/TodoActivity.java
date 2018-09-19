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

import static java.lang.Thread.sleep;

public class TodoActivity {

	public TodoActivity() {	
	}
	
	public static void main(String[] args) {
		String ListName = "New List 1";
		String Do1_1 = "Do this 1.1";
		String Do1_2 = "Do this 1.2";
		String Do2_1 = "Do that 2.1";
		String Do2_2 = "Do that 2.2";
		String Do3_1 = "Do three 3.1";
		String Do3_2 = "Do three 3.2";

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
			sleep(10000);   // Added for ConsensusRSM testing.
			System.out.println("new to do list for 1");

			for (int i=0; i<1; i++) {
				// Add to-do items.
				System.out.println("Adding to do 1.1");
				td1.addToDo(1, Do1_1+"<"+i+">");
				System.out.println("Adding to do 1.2");
				td1.addToDo(1, Do1_2+"<"+i+">");
				System.out.println("Adding to do 2.1");
				td1.addToDo(2, Do2_1+"<"+i+">");
				System.out.println("Adding to do 2.2");
				td1.addToDo(2, Do2_2+"<"+i+">");
//				td1.addToDo(3, Do3_1+"<"+i+">");
//				td1.addToDo(3, Do3_2+"<"+i+">");

				// Retrieve to-do items.
				TodoList getTd1 = tlm.getToDoList(ListName);
				String testTdString2 = getTd1.getToDoString(2);
				String expectedTdString2 = Do2_1 + " : " + Do2_2 + " : ";
//				System.out.println("Expect testTdString for 2: " + expectedTdString2);
				System.out.println("Actual testTdString for 2: " + testTdString2);

				String testTdString1 = getTd1.getToDoString(1);
				String expectedTdString1 = Do1_1 + " : " + Do1_2 + " : ";
//				System.out.println("Expect testTdString for 1: " + expectedTdString1);
				System.out.println("Actual testTdString for 1: " + testTdString1);

//				String testTdString3 = getTd1.getToDoString(3);
//				String expectedTdString3 = Do3_1 + " : " + Do3_2 + " : ";
//				System.out.println("Expect testTdString for 3: " + expectedTdString3);
//				System.out.println("Actual testTdString for 3: " + testTdString3);
			}

            tlm.doSomething("Testing completed.");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

package sapphire.appexamples.hankstodo.device;

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

            TodoList td1 = tlm.newTodoList("1");
			System.out.println("new to do list for 1");
			TodoList td2 = tlm.newTodoList("2");
			System.out.println("new to do list for 2");

            td1.addToDo("1", "Do this 1.1");
			System.out.println("do this 1.1");
            td1.addToDo("1", "Do this 1.2");
			System.out.println("do this 1.2");
            td2.addToDo("2", "Do that 2.1");
			System.out.println("do that 2.1");
            td2.addToDo("2", "Do that 2.2");
			System.out.println("do that 2.2");

            TodoList testTd2 = tlm.getToDoList("2");
            String testTdString2 = testTd2.getToDoString("2");
            System.out.println("Printing testTdString for 2: " + testTdString2);

            TodoList testTd1 = tlm.getToDoList("1");
            String testTdString1 = testTd1.getToDoString("1");
            System.out.println("Printing testTdString for 1: " + testTdString1);

            tlm.doSomething("do 1");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

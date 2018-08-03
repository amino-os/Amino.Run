package sapphire.appexamples.hankstodo.device;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import sapphire.appexamples.hankstodo.app.AppGrpcClient;
import sapphire.appexamples.hankstodo.app.GlobalGrpcClientRef;
import sapphire.appexamples.hankstodo.app.TodoList;
import sapphire.appexamples.hankstodo.app.TodoListManager;
import sapphire.appexamples.hankstodo.app.grpcStubs.TodoListManager_Stub;
import sapphire.appexamples.hankstodo.app.grpcStubs.TodoList_Stub;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

public class TodoActivity {
	
	public TodoActivity() {	
	}
	
	public static void main(String[] args) {
		Registry registry;
		try {
			AppGrpcClient grpcClient = new AppGrpcClient(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), args[4], Integer.parseInt(args[5]));
			GlobalGrpcClientRef.grpcClient = grpcClient;
			TodoListManager_Stub tlm;
			try {
				tlm = new TodoListManager_Stub();
				System.out.println("Finished");

			} catch (Exception e) {
				return;
			}
			/*registry = LocateRegistry.getRegistry(args[0],Integer.parseInt(args[1]));
			OMSServer server = (OMSServer) registry.lookup("SapphireOMS");
			System.out.println(server);
			TodoListManager tlm = (TodoListManager) server.getAppEntryPoint();
            System.out.println("Received tlm: " + tlm);
            
            KernelServer nodeServer = new KernelServerImpl(new InetSocketAddress(args[2], Integer.parseInt(args[3])), new InetSocketAddress(args[0], Integer.parseInt(args[1])));
            */
			TodoList_Stub tl = tlm.newTodoList("Hanks");
			System.out.println("Received tl1: " + tl);
			System.out.println(tl.addToDo("First todo"));
			System.out.println(tl.addToDo("Second todo"));
			System.out.println(tl.addToDo("Third todo"));

			TodoList_Stub tl2 = tlm.newTodoList("AAA");
			System.out.println("Received tl2: " + tl2);
			System.out.println(tl2.addToDo("First todo"));
			System.out.println(tl2.addToDo("Second todo"));
			System.out.println(tl2.addToDo("Third todo"));
	
			TodoList_Stub tl3 = tlm.newTodoList("HHH");
			System.out.println("Received tl3: " + tl3);
			System.out.println(tl3.addToDo("First todo"));
			System.out.println(tl3.addToDo("Second todo"));
			System.out.println(tl3.addToDo("Third todo"));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

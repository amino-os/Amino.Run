package example.hello;
	
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
	
public class Server implements Hello {
    //Test t;

	
    public Server() {}

    public String sayHello() {
	return "Hello, world!";
    }

    public Test getTest() throws RemoteException {
    	return new TestImpl();
    }
	
    public static void main(String args[]) {

        // set ip address of rmi server
	//System.setProperty("java.rmi.server.hostname", "10.0.2.2");
	
	try {
	    Server obj = new Server();
	    Hello stub = (Hello) UnicastRemoteObject.exportObject(obj, 0);


	    // Bind the remote object's stub in the registry
	    // Registry registry = LocateRegistry.getRegistry();
	    
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("Hello", stub);

	    System.err.println("Server ready");
	} catch (Exception e) {
	    System.err.println("Server exception: " + e.toString());
	    e.printStackTrace();
	}
    }
}

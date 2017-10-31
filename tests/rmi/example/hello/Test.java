package example.hello;
	
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
	
public interface Test extends Remote {
    public String sayTest() throws RemoteException;
}

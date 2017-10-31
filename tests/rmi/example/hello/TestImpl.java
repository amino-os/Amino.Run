package example.hello;
	
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
	
public class TestImpl extends UnicastRemoteObject implements Test {
    public TestImpl() throws RemoteException {
        super();
    }

    public String sayTest() throws RemoteException {
	return "Test!";
    }
}

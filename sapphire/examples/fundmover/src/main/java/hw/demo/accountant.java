package hw.demo;

import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.runtime.Sapphire;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import static sapphire.runtime.Sapphire.new_;

public class accountant {
    public static void main(String[] args) throws RemoteException {
        System.out.println("Hello transaction demo");

        System.out.println("creating local Sapphire runtime env...");
        InetSocketAddress host = new InetSocketAddress("127.0.0.1", 55555);
        InetSocketAddress oms = new InetSocketAddress("127.0.0.1", 55556);
        KernelServerImpl kernelServer = new KernelServerImpl(host, oms);

        System.out.println("bringing up the local RMI facility ...");
        KernelServer stub = (KernelServer) UnicastRemoteObject.exportObject(kernelServer, 0);
        Registry registry = LocateRegistry.createRegistry(55555);
        registry.rebind("SapphireKernelServer", stub);

        wallet wallet = (wallet)new_(hw.demo.wallet.class);
        wallet.credit(100);
        bankaccount bankaccount = (bankaccount) new_(bankaccount.class);

        System.out.println("creating the fainance object...");
        finance finance = (finance) new_(hw.demo.finance.class,wallet, bankaccount);

        System.out.println("trnasfering fund between 2 entities...");
        try {
            finance.transferFromWallet(51);
        }catch (Exception e) {
            System.out.println("---------- error accurred -----");
            System.out.println(e.toString());
        }

        System.out.println("checking the current balance after the transaction...");
        System.out.printf("finance details: %s\r\n", finance.getDetails());
    }
}

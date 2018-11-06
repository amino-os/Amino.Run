package sapphire.appexamples.fundmover;

import sapphire.app.DMSpec;
import sapphire.app.Language;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.SapphireObjectID;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.policy.transaction.TwoPCCoordinatorPolicy;
import  sapphire.policy.transaction.TwoPCExtResourceCohortPolicy;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import static sapphire.runtime.Sapphire.new_;

public class FundmoverMain {
    public static void main(String[] args) throws RemoteException {

        if (args.length < 4) {
            System.out.println("Incorrect arguments to the program");
            System.out.println("usage: " +FundmoverMain.class.getSimpleName() + " <host-ip> <host-port> <oms ip> <oms-port>");
            System.exit(1);
        }
        String hostIp = args[0], hostPort = args[1], omsIp = args[2], omsPort = args[3];
        InetSocketAddress hostAddr = new InetSocketAddress(hostIp, Integer.parseInt(hostPort)), omsAddr = new InetSocketAddress(omsIp, Integer.parseInt(omsPort));


        Registry registry;
        try{
            registry = LocateRegistry.getRegistry(args[0],Integer.parseInt(args[1]));
            OMSServer omsserver = (OMSServer) registry.lookup("SapphireOMS");
            System.out.println(omsserver);

            KernelServer nodeServer = new KernelServerImpl(new InetSocketAddress(args[2], Integer.parseInt(args[3])), new InetSocketAddress(args[0], Integer.parseInt(args[1])));

            SapphireObjectSpec walletSpec,bankAccountSpec,financeSpec ;
            walletSpec= SapphireObjectSpec.newBuilder()
                         .setLang(Language.java)
                         .setJavaClassName(Wallet.class.getName())
                         .addDMSpec(
                                 DMSpec.newBuilder()
                                 .setName(TwoPCExtResourceCohortPolicy.class.getName())
                                 .create())
                         .create();
            bankAccountSpec = SapphireObjectSpec.newBuilder()
                               .setLang(Language.java)
                               .setJavaClassName(BankAccount.class.getName())
                               .addDMSpec(
                                       DMSpec.newBuilder()
                                       .setName(TwoPCExtResourceCohortPolicy.class.getName())
                                       .create())
                               .create();
            financeSpec = SapphireObjectSpec.newBuilder()
                           .setLang(Language.java)
                           .setJavaClassName(Finance.class.getName())
                           .addDMSpec(
                                   DMSpec.newBuilder()
                                   .setName(TwoPCCoordinatorPolicy.class.getName())
                                   .create())
                           .create();

            SapphireObjectID walletSapphireObjectID= omsserver.createSapphireObject(walletSpec.toString());

            Wallet wallet = (Wallet)omsserver.acquireSapphireObjectStub(walletSapphireObjectID);;

            wallet.credit(100);

            SapphireObjectID bankAccountSapphireObjectID= omsserver.createSapphireObject(bankAccountSpec.toString());

            BankAccount bankaccount = (BankAccount)omsserver.acquireSapphireObjectStub(bankAccountSapphireObjectID);

            System.out.println("creating the finance object...");

            SapphireObjectID financeSapphireObjectID= omsserver.createSapphireObject(financeSpec.toString(),wallet,bankaccount);

            Finance finance = (Finance) omsserver.acquireSapphireObjectStub(financeSapphireObjectID);

            System.out.println("transfering fund between 2 entities...");

            finance.transferFromWallet(30);
            System.out.println("checking the current balance after the transaction...");
            System.out.printf("finance details: %s\r\n", finance.getDetails());
        }catch (Exception e) {
            System.out.println("---------- error accurred -----");
            System.out.println(e.toString());
        }


    }
}

package amino.run.appexamples.fundmover;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Collections;

import amino.run.app.DMSpec;
import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.app.Registry;
import amino.run.common.ArgumentParser.AppArgumentParser;
import amino.run.common.MicroServiceID;
import amino.run.kernel.server.KernelServer;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.policy.transaction.TwoPCCoordinatorPolicy;
import amino.run.policy.transaction.TwoPCExtResourceCohortPolicy;
import com.google.devtools.common.options.OptionsParser;

public class FundmoverMain {
    public static void main(String[] args) throws RemoteException {
        OptionsParser parser = OptionsParser.newOptionsParser(AppArgumentParser.class);
        if (args.length < 8) {
            System.out.println("Incorrect arguments to the program");
            printUsage(parser);
            return;
        }

        try {
            parser.parse(args);
        } catch (Exception e) {
            printUsage(parser);
            return;
        }

        AppArgumentParser appArgs = parser.getOptions(AppArgumentParser.class);

        java.rmi.registry.Registry registry;
        try{
            registry = LocateRegistry.getRegistry(appArgs.omsIP, appArgs.omsPort);
            Registry server = (Registry) registry.lookup("io.amino.run.oms");
            System.out.println(server);

            KernelServer nodeServer = new KernelServerImpl(new InetSocketAddress(appArgs.kernelServerIP, appArgs.kernelServerPort), new InetSocketAddress(appArgs.omsIP, appArgs.omsPort));

            MicroServiceSpec walletSpec,bankAccountSpec,financeSpec ;
            walletSpec= MicroServiceSpec.newBuilder()
                         .setLang(Language.java)
                         .setJavaClassName(Wallet.class.getName())
                         .addDMSpec(
                                 DMSpec.newBuilder()
                                 .setName(TwoPCExtResourceCohortPolicy.class.getName())
                                 .create())
                         .create();
            bankAccountSpec = MicroServiceSpec.newBuilder()
                               .setLang(Language.java)
                               .setJavaClassName(BankAccount.class.getName())
                               .addDMSpec(
                                       DMSpec.newBuilder()
                                       .setName(TwoPCExtResourceCohortPolicy.class.getName())
                                       .create())
                               .create();
            financeSpec = MicroServiceSpec.newBuilder()
                           .setLang(Language.java)
                           .setJavaClassName(Finance.class.getName())
                           .addDMSpec(
                                   DMSpec.newBuilder()
                                   .setName(TwoPCCoordinatorPolicy.class.getName())
                                   .create())
                           .create();

            MicroServiceID walletMicroServiceID = server.create(walletSpec.toString());

            Wallet wallet = (Wallet)server.acquireStub(walletMicroServiceID);;

            wallet.credit(100);

            MicroServiceID bankAccountMicroServiceID = server.create(bankAccountSpec.toString());

            BankAccount bankaccount = (BankAccount)server.acquireStub(bankAccountMicroServiceID);

            System.out.println("creating the finance object...");

            MicroServiceID financeMicroServiceID = server.create(financeSpec.toString(),wallet,bankaccount);

            Finance finance = (Finance) server.acquireStub(financeMicroServiceID);

            System.out.println("transferring fund between 2 entities...");

            finance.transferFromWallet(40);
            System.out.println("checking the current balance after the transaction...");
            System.out.printf("finance details: %s\r\n", finance.getDetails());

            finance.transferFromBank(20);
            System.out.println("checking the current balance after the transaction...");
            System.out.printf("finance details: %s\r\n", finance.getDetails());
            // Verifying the rollback use case
            finance.transferFromWallet(85);

        }catch (Exception e) {
            System.out.println("---------- error occurred -----");
            System.out.println(e.toString());

        }


    }

    private static void printUsage(OptionsParser parser) {
        System.out.println(
                "Usage: java -cp <classpath> "
                        + FundmoverMain.class.getSimpleName()
                        + System.lineSeparator()
                        + parser.describeOptions(
                        Collections.<String, String>emptyMap(),
                        OptionsParser.HelpVerbosity.LONG));
    }
}

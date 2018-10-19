package sapphire.demo;

import sapphire.app.DMSpec;
import sapphire.app.Language;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.SapphireObjectCreationException;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.oms.OMSServerImpl;
import sapphire.policy.SapphirePolicyUpcalls;
import sapphire.policy.atleastoncerpc.AtLeastOnceRPCPolicy;
import sapphire.policy.dht.DHTPolicy;
import sapphire.policy.scalability.LoadBalancedMasterSlaveSyncPolicy;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class pinkisClient {

    static final int NumOfRegions = 2;
    static final int NumOfKernelServersPerRegion = 2;
    static final String RegionsNameBase = "Region";
    static final String ip = "127.0.0.1";
    static int omsPort = 22346;
    static int ksPort = 30001;

    public static void main(String[] args) {
        System.out.println("hello demo");

        // pinkisServer server = new pinkisServer();

        pinkisServer server = null;

        try {
//            OMSServerImpl.main(new String[] {ip, String.valueOf(omsPort)});
//            Thread.sleep(1000);
//
//            for (int i = 1; i < NumOfRegions + 1; i++) {
//                for (int j = 0; j < NumOfKernelServersPerRegion; j++) {
//                    KernelServerImpl.main(
//                            new String[] {
//                                    ip, String.valueOf(ksPort), ip, String.valueOf(omsPort), RegionsNameBase + i
//                            });
//                    ksPort++;
//                    Thread.sleep(200);
//                }
//            }

            KernelServer dummyLocalKS = new KernelServerImpl(new InetSocketAddress("127.0.0.2", 11111), new InetSocketAddress(args[0], Integer.parseInt(args[1])));

            Registry registry = LocateRegistry.getRegistry(args[0],Integer.parseInt(args[1]));
            OMSServer omsserver = (OMSServer) registry.lookup("SapphireOMS");

            DHTPolicy.Config dhtConfig = new DHTPolicy.Config();
            dhtConfig.setNumOfShards(2);

            SapphireObjectSpec spec;
            spec = SapphireObjectSpec.newBuilder()
                    .setLang(Language.java)
                    .setJavaClassName("sapphire.demo.pinkisServer")
                    .addDMSpec(
                            DMSpec.newBuilder()
                                    .setName(DHTPolicy.class.getName())
                                    .addConfig(dhtConfig)
                                    .create())
                    .addDMSpec(DMSpec.newBuilder()
                                    .setName(LoadBalancedMasterSlaveSyncPolicy.class.getName()).create())
                    .create();

            SapphireObjectID soid = omsserver.createSapphireObject(spec.toString());
            server = (pinkisServer)omsserver.acquireSapphireObjectStub(soid);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (SapphireObjectCreationException e) {
            e.printStackTrace();
        } catch (SapphireObjectNotFoundException e) {
            e.printStackTrace();
        }

//        catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        for (int i=0; i<30; ++i) {
            server.set("foo_"+i, "bar_"+i);
            Serializable value = server.get("foo_"+i);
            System.out.println("got: " + value);
        }
    }
}

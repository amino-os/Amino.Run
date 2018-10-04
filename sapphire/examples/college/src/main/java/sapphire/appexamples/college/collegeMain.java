package sapphire.appexamples.college;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import sapphire.app.DMSpec;
import sapphire.app.Language;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.SapphireObjectID;
import sapphire.graal.io.GraalContext;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.policy.atleastoncerpc.AtLeastOnceRPCPolicy;
import sapphire.policy.dht.DHTPolicy;
import org.graalvm.polyglot.*;

import sapphire.appexamples.college.stubs.*;

public class collegeMain {

    public static void main(String[] args) {

        if (args.length < 4) {
            System.out.println("Incorrect arguments to the program");
            System.out.println("<host-ip> <host-port> <oms ip> <oms-port>");
            return;
        }
        Registry registry;
        try {
            registry = LocateRegistry.getRegistry(args[0],Integer.parseInt(args[1]));
            OMSServer omsserver = (OMSServer) registry.lookup("SapphireOMS");
            System.out.println(omsserver);

            KernelServer nodeServer = new KernelServerImpl(new InetSocketAddress(args[2], Integer.parseInt(args[3])), new InetSocketAddress(args[0], Integer.parseInt(args[1])));

            SapphireObjectSpec spec = SapphireObjectSpec.newBuilder()
                    .setLang(Language.js)
                    .setName("College")
//                    .addDMSpec(DMSpec.newBuilder()
//                            .setName(AtLeastOnceRPCPolicy.class.getName())
//                            .create())
                    .create();

            SapphireObjectID sapphireObjId = omsserver.createSapphireObject(spec.toString());
            College_ClientStub ccs = (College_ClientStub)omsserver.acquireSapphireObjectStub(sapphireObjId);

            Value v = GraalContext.getContext().asValue("newCollege");
            ccs.setName(v);
            System.out.println("after setname, name is " + ccs.getName());

            omsserver.deleteSapphireObject(sapphireObjId);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            System.out.println("Exception Received : " + e);
            e.printStackTrace();
        }
    }
}
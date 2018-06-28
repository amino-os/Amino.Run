/**
 * Created by Jithu Thomas on 21/6/18.
 */

package sapphire.userApp.Greeting;


import com.google.protobuf.InvalidProtocolBufferException;
import greeting_proto.GreetingProto;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import sapphire.api.*;

import java.util.concurrent.TimeUnit;

public class Greeting {

    public static final String sapphire_objName = "Greeting";

    // gRPC client code for connecting to the Sapphire Object
    // running on the Sapphire Process. -->
    // TODO: This part of code need to be made more generic
    // and placed in another class which can be easily injected.
    private final ManagedChannel channel;
    private MgmtgrpcServiceGrpc.MgmtgrpcServiceBlockingStub blockingStub;

    public static final String sapphire_process_host = "localhost";
    public static final int sapphire_process_port = 7000;

    public Greeting(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build());
    }

    Greeting(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = MgmtgrpcServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    // <-- End of gRPC client code.

    public String createSapphireObject(String sobjName) {
        System.out.println("Creating SapphireObject: " + sobjName);

        CreateRequest request = CreateRequest.newBuilder().setName(sobjName).build();
        CreateReply response;
        try {
            response = blockingStub.createSapphireObject(request);
        } catch (StatusRuntimeException e) {
            System.out.println("createSapphireObject RPC failed: " + e.getStatus());
            return null;
        }
        System.out.println("createSapphireObject response: " + response.getObjId());
        return response.getObjId();
    }

    public boolean deleteSapphireObject(String sobjId) {
        System.out.println("Deleting SapphireObject: " + sobjId);

        DeleteRequest request = DeleteRequest.newBuilder().setObjId(sobjId).build();
        DeleteReply response;
        try {
            response = blockingStub.deleteSapphireObject(request);
        } catch (StatusRuntimeException e) {
            System.out.println("deleteSapphireObject RPC failed: " + e.getStatus());
            return false;
        }
        System.out.println("deleteSapphireObject response: " + response.getFlag());
        return response.getFlag();
    }

    public String sapphireFunc_HelloWorld(int reqNum, String reqString) {
        System.out.printf("Sapphire function HelloWorld triggered, with number: %d and string: %s\n", reqNum, reqString);

        String sobjId = createSapphireObject(sapphire_objName);
        System.out.println("CreateSapphireObject Response is: " + sobjId);

        GenericMethodRequest request = GenericMethodRequest.newBuilder()
                .setFuncName("HelloWorld")
                .setObjId(sobjId)
                .setSapphireObjName(sapphire_objName)
                .setParams(GreetingProto.HelloWorldRequest.newBuilder().setNum(reqNum).setName(reqString).build().toByteString())
                .build();
        GenericMethodReply response;
        try {
            response = blockingStub.genericMethodInvoke(request);
        } catch (StatusRuntimeException e) {
            System.out.println("genericMethodInvoke RPC failed: " + e.getStatus());
            return "";
        }

        GreetingProto.HelloWorldReply methodResponse;
        try {
            methodResponse = GreetingProto.HelloWorldReply.parseFrom(response.getRet());
        } catch (InvalidProtocolBufferException e) {
            System.out.println("genericMethodInvoke parameter deserializing failed: " + e.getMessage());
            return "";
        }

        Boolean status = deleteSapphireObject(sobjId);
        if (status != true) {
            System.out.println("DeleteSapphireObject failed: " + status);
            return "";
        }
        return methodResponse.getRetName();
    }

    public static void main(String[] args) throws Exception {

        System.out.println("Inside userApp Greeting main()");

        // Establish connection using gRPC.
        Greeting client = new Greeting(sapphire_process_host, sapphire_process_port);

        // Sappharized user-function "HelloWorld"
        String resp_string;
        //resp_string = client.HelloWorld(2018, "HelloWorld from DCAP.!!!");
        resp_string = client.sapphireFunc_HelloWorld(2018, "HelloWorld from DCAP.!!!");

        System.out.println("Response received: " + resp_string);
    }
}

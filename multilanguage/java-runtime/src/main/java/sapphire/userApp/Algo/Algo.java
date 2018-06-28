/**
 * Created by Jithu Thomas on 25/6/18.
 */

package sapphire.userApp.Algo;

import algo_proto.AlgoProto;
import com.google.common.primitives.Ints;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import sapphire.api.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Algo {


    public static final String sapphire_objName = "Algo";

    // gRPC client code for connecting to the Sapphire Object
    // running on the Sapphire Process. -->
    // TODO: This part of code need to be made more generic
    // and placed in another class which can be easily injected.
    private final ManagedChannel channel;
    private MgmtgrpcServiceGrpc.MgmtgrpcServiceBlockingStub blockingStub;

    public static final String sapphire_process_host = "localhost";
    public static final int sapphire_process_port = 7000;

    public Algo(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build());
    }

    Algo(ManagedChannel channel) {
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

    public int[] sapphireFunc_Sort(int[] reqArr) {
        System.out.println("Sapphire function Sort triggered, with array: " + reqArr);

        String sobjId = createSapphireObject(sapphire_objName);
        System.out.println("CreateSapphireObject Response is: " + sobjId);

        List<Integer> list = Ints.asList(reqArr);

        GenericMethodRequest request = GenericMethodRequest.newBuilder()
                .setFuncName("Sort")
                .setObjId(sobjId)
                .setSapphireObjName(sapphire_objName)
                .setParams(AlgoProto.SortRequest.newBuilder().addAllArr(list).build().toByteString())
                .build();
        GenericMethodReply response;
        try {
            response = blockingStub.genericMethodInvoke(request);
        } catch (StatusRuntimeException e) {
            System.out.println("genericMethodInvoke RPC failed: " + e.getStatus());
            return null;
        }

        AlgoProto.SortReply methodResponse;
        try {
            methodResponse = AlgoProto.SortReply.parseFrom(response.getRet());
        } catch (InvalidProtocolBufferException e) {
            System.out.println("genericMethodInvoke parameter deserializing failed: " + e.getMessage());
            return null;
        }

        Boolean status = deleteSapphireObject(sobjId);
        if (status != true) {
            System.out.println("DeleteSapphireObject failed: " + status);
            return null;
        }

        int resp[] = methodResponse.getArrList().stream().mapToInt(i->i).toArray();
        return resp;
    }

    public static void main(String[] args) throws Exception {

        System.out.println("Inside user_app Algo main()");

        // Establish connection using gRPC.
        Algo client = new Algo(sapphire_process_host, sapphire_process_port);

        // Sappharized user-function "Sort"
        int arr[] = {10, 6, 5, 20, 1};
        //int sort_arr[] = client.Sort(arr);
        int sort_arr[] = client.sapphireFunc_Sort(arr);
        System.out.println("Response received, sorted array: " + Arrays.toString(sort_arr));
    }
}

/**
 * Created by Jithu Thomas on 25/6/18.
 */

package sapphire.userApp.Generic;

import algo_proto.AlgoProto;
import com.google.common.primitives.Ints;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import greeting_proto.GreetingProto;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import sapphire.api.*;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Generic {

    // gRPC client code for connecting to the Sapphire Object
    // running on the Sapphire Process. -->
    // TODO: This part of code need to be made more generic
    // and placed in another class which can be easily injected.
    private final ManagedChannel channel;
    private static MgmtgrpcServiceGrpc.MgmtgrpcServiceBlockingStub blockingStub;

    public static final String sapphire_process_host = "localhost";
    public static final int sapphire_process_port = 7000;

    public Generic(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build());
    }

    Generic(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = MgmtgrpcServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    // <-- End of gRPC client code.

    public static String createSapphireObject(String sobjName) {
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

    public static boolean deleteSapphireObject(String sobjId) {
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

    public static GenericMethodReply invokeSapphireObjFunc (String sapphire_objName, String sapphire_funcName, ByteString params) {

        // Establish connection using gRPC.
        Generic client = new Generic(sapphire_process_host, sapphire_process_port);

        // Create the Sapphire Object instance.
        String sobjId = createSapphireObject(sapphire_objName);
        System.out.println("CreateSapphireObject Response is: " + sobjId);

        // Invoke the Sapphire Object function.
        GenericMethodRequest genericReq = GenericMethodRequest.newBuilder()
                .setFuncName(sapphire_funcName)
                .setSapphireObjName(sapphire_objName)
                .setObjId(sobjId)
                .setParams(params).build();

        GenericMethodReply genericRes;
        try {
            genericRes = blockingStub.genericMethodInvoke(genericReq);
        } catch (StatusRuntimeException e) {
            System.out.println("invokeSapphireObjFunc: genericMethodInvoke RPC failed: " + e.getStatus());
            return null;
        }

        // Delete the Sapphire Object instance
        Boolean status = deleteSapphireObject(sobjId);
        if (status != true) {
            System.out.println("DeleteSapphireObject failed: " + status);
            return null;
        }

        return genericRes;
    }

    public static String sapphireFunc_HelloWorld(String sapphire_objName, String sapphire_funcName, int reqNum, String reqString) {
        System.out.printf("HelloWorld triggered, with number: %d and string: %s\n", reqNum, reqString);

        ByteString params = GreetingProto.HelloWorldRequest.newBuilder()
                .setNum(reqNum)
                .setName(reqString).build().toByteString();

        GenericMethodReply response = invokeSapphireObjFunc(sapphire_objName, sapphire_funcName, params);
        if (response == null) {
            System.out.println("Generic invokeSapphireObjFunc failed.!!!");
            return "";
        }

        GreetingProto.HelloWorldReply methodResponse;
        try {
            methodResponse = GreetingProto.HelloWorldReply.parseFrom(response.getRet());
        } catch (InvalidProtocolBufferException e) {
            System.out.println("genericMethodInvoke parameter deserializing failed: " + e.getMessage());
            return "";
        }

        return methodResponse.getRetName();
    }

    public static String sapphireFunc_Fibbonaci(String sapphire_objName, String sapphire_funcName, int reqNum) {
        System.out.println("Fibbonaci triggered, with number: " + reqNum);

        ByteString params = GreetingProto.FibbonaciRequest.newBuilder()
                .setNum(reqNum).build().toByteString();

        GenericMethodReply response = invokeSapphireObjFunc(sapphire_objName, sapphire_funcName, params);
        if (response == null) {
            System.out.println("Generic invokeSapphireObjFunc failed.!!!");
            return "";
        }

        GreetingProto.FibbonaciReply methodResponse;
        try {
            methodResponse = GreetingProto.FibbonaciReply.parseFrom(response.getRet());
        } catch (InvalidProtocolBufferException e) {
            System.out.println("genericMethodInvoke parameter deserializing failed: " + e.getMessage());
            return "";
        }

        return methodResponse.getRet();
    }

    public static int[] sapphireFunc_Sort(String sapphire_objName, String sapphire_funcName, int[] arr) {
        System.out.println("Sort triggered, with array: " + Arrays.toString(arr));

        List<Integer> list = Ints.asList(arr);

        ByteString params = AlgoProto.SortRequest.newBuilder()
                .addAllArr(list).build().toByteString();

        GenericMethodReply response = invokeSapphireObjFunc(sapphire_objName, sapphire_funcName, params);
        if (response == null) {
            System.out.println("Generic invokeSapphireObjFunc failed.!!!");
            return null;
        }

        AlgoProto.SortReply methodResponse;
        try {
            methodResponse = AlgoProto.SortReply.parseFrom(response.getRet());
        } catch (InvalidProtocolBufferException e) {
            System.out.println("genericMethodInvoke parameter deserializing failed: " + e.getMessage());
            return null;
        }

        int resp[] = methodResponse.getArrList().stream().mapToInt(i->i).toArray();
        return resp;
    }

    public static void main(String[] args) throws Exception {

        System.out.println("Inside userApp Generic main()");
        System.out.println();

        Scanner sc = new Scanner(System.in);
        System.out.print("Enter Sapphire object name: ");
        String sapphire_objName = sc.next();
        System.out.print("Enter Sapphire function name: ");
        String sapphire_funcName = sc.next();

        if (sapphire_objName != "" && sapphire_funcName != "") {
            System.out.printf("Entered sapphire_objName is: %s and sapphire_funcName is: %s\n", sapphire_objName, sapphire_funcName);
        } else {
            System.out.println("Entered invalid sapphire_objName or sapphire_funcName. Please try again.!!!");
            sc.close();
            return;
        }
        System.out.println();

        if (sapphire_funcName.equals("HelloWorld")) {
            // Sappharized user-function "HelloWorld"
            String resp_string;
            //resp_string = HelloWorld(2018, "HelloWorld from DCAP.!!!");
            resp_string = sapphireFunc_HelloWorld(sapphire_objName, sapphire_funcName, 2018, "HelloWorld from DCAP.!!!");
            System.out.println("Response received: " + resp_string);

        } else if (sapphire_funcName.equals("Fibbonaci")) {
            // Sappharized user-function "Fibbonaci"
            int fibReq = 10;
            String fibRes;
            //fibRes = Fibbonaci(fibReq);
            fibRes = sapphireFunc_Fibbonaci(sapphire_objName, sapphire_funcName, fibReq);
            System.out.printf("Response received, Fibbonaci of num: %d is: %s\n", fibReq, fibRes );

        } else if (sapphire_funcName.equals("Sort")) {
            // Sappharized user-function "Sort"
            int arr[] = {10, 6, 5, 20, 1};
            //int sort_arr[] = client.Sort(arr);
            int sort_arr[] = sapphireFunc_Sort(sapphire_objName, sapphire_funcName, arr);
            System.out.println("Response received, sorted array: " + Arrays.toString(sort_arr));

        } else {
            System.out.println("Unknown function provided. Please try again.!!!");
            return;
        }

        sc.close();
        System.out.println();
    }
}

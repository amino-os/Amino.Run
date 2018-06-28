/**
 * Created by Jithu Thomas on 14/6/18.
 */
package sapphire.kernel;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import sapphire.api.*;

import java.util.concurrent.TimeUnit;

public class KernelServer_Client {

    private final ManagedChannel channel;
    private MgmtgrpcServiceGrpc.MgmtgrpcServiceBlockingStub blockingStub;

    public KernelServer_Client(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build());
    }

    KernelServer_Client(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = MgmtgrpcServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public CreateReply CreateSapphireObject(Kernel_CreateRequest req) {
        System.out.println("Kernel_server: Inside CreateSapphireObject, name: " + req.getName());

        CreateRequest request = CreateRequest.newBuilder().setName(req.getName()).build();
        CreateReply response;
        try {
            response = blockingStub.createSapphireObject(request);
        } catch (StatusRuntimeException e) {
            System.out.println("Kernel_server: CreateSapphireObject RPC failed: " + e.getStatus());
            return null;
        }
        System.out.println("Kernel_server: CreateSapphireObject response: " + response.getObjId());
        return response;
    }

    public DeleteReply DeleteSapphireObject(Kernel_DeleteRequest req) {
        System.out.println("Kernel_server: Inside DeleteSapphireObject");

        DeleteRequest request = DeleteRequest.newBuilder().setObjId(req.getObjId()).build();
        DeleteReply response;
        try {
            response = blockingStub.deleteSapphireObject(request);
        } catch (StatusRuntimeException e) {
            System.out.println("Kernel_server: DeleteSapphireObject RPC failed: " + e.getStatus());
            return null;
        }
        System.out.println("Kernel_server: DeleteSapphireObject response: " + response.getFlag());
        return response;
    }

    public GenericMethodReply GenericMethodInvoke(Kernel_GenericMethodRequest req) {
        System.out.println("Kernel_server: Inside GenericMethodInvoke");

        GenericMethodRequest request = GenericMethodRequest.newBuilder()
                .setObjId(req.getObjId())
                .setSapphireObjName(req.getSapphireObjName())
                .setFuncName(req.getFuncName())
                .setParams(req.getParams())
                //.addAllParamNames(req.getParamNamesList())
                .addAllDmsList(req.getDmsListList()).build();

        GenericMethodReply response;
        try {
            response = blockingStub.genericMethodInvoke(request);
        } catch (StatusRuntimeException e) {
            System.out.println("Kernel_server: GenericMethodInvoke RPC failed: " + e.getStatus());
            return null;
        }
        System.out.println("Kernel_server: GenericMethodInvoke response: " + response.getRet());
        return response;
    }
}

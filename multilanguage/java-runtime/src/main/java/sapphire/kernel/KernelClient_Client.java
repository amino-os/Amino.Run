/**
 * Created by Jithu Thomas on 14/6/18.
 */
package sapphire.kernel;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import sapphire.api.CreateRequest;
import sapphire.api.DeleteRequest;
import sapphire.api.GenericMethodRequest;

import java.util.concurrent.TimeUnit;

public class KernelClient_Client {

    private final ManagedChannel channel;
    private Kernel_MgmtgrpcServiceGrpc.Kernel_MgmtgrpcServiceBlockingStub blockingStub;

    public KernelClient_Client(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build());
    }

    KernelClient_Client(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = Kernel_MgmtgrpcServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public Kernel_CreateReply CreateSapphireObject(CreateRequest req) {

        System.out.println("Kernel_client: Inside CreateSapphireObject, name: " + req.getName());

        Kernel_CreateRequest request = Kernel_CreateRequest.newBuilder().setName(req.getName()).build();
        Kernel_CreateReply response;
        try {
            response = blockingStub.kernelCreateSapphireObject(request);
        }catch (StatusRuntimeException e) {
            System.out.println("Kernel_client: CreateSapphireObject RPC failed: " + e.getStatus());
            return null;
        }
        System.out.println("Kernel_client: CreateSapphireObject response: " + response.getObjId());
        return response;
    }

    public Kernel_DeleteReply DeleteSapphireObject(DeleteRequest req) {
        System.out.println("Kernel_client: Inside DeleteSapphireObject");

        Kernel_DeleteRequest request = Kernel_DeleteRequest.newBuilder().setObjId(req.getObjId()).build();
        Kernel_DeleteReply response;
        try {
            response = blockingStub.kernelDeleteSapphireObject(request);
        } catch (StatusRuntimeException e) {
            System.out.println("Kernel_client: DeleteSapphireObject RPC failed: " + e.getStatus());
            return null;
        }
        System.out.println("Kernel_client: DeleteSapphireObject response: " + response.getFlag());
        return response;
    }

    public Kernel_GenericMethodReply GenericMethodInvoke(GenericMethodRequest req) {
        System.out.println("Kernel_client: Inside GenericMethodInvoke");

        Kernel_GenericMethodRequest request = Kernel_GenericMethodRequest.newBuilder()
                .setObjId(req.getObjId())
                .setSapphireObjName(req.getSapphireObjName())
                .setFuncName(req.getFuncName())
                .setParams(req.getParams())
                //.addAllParamNames(req.getParamNamesList())
                .addAllDmsList(req.getDmsListList()).build();

        Kernel_GenericMethodReply response;
        try {
            response = blockingStub.kernelGenericMethodInvoke(request);
        } catch (StatusRuntimeException e) {
            System.out.println("Kernel_client: GenericMethodInvoke RPC failed: " + e.getStatus());
            return null;
        }
        System.out.println("Kernel_client: GenericMethodInvoke response: " + response.getRet());
        return response;
    }

    public void demo(String name) {
        System.out.println("Reached demo: " + name);
    }
}

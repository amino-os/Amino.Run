/**
 * Created by Jithu Thomas on 14/6/18.
 */
package sapphire.kernel;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import sapphire.api.*;

import java.io.IOException;

public class KernelClient {

    // Start of code for Application gRPC server running at local Kernel -->
    private Server app_server;

    private void app_server_start(int app_server_port) throws IOException {
        app_server = ServerBuilder.forPort(app_server_port)
                .addService(new MgmtgrpcServiceImpl())
                .build()
                .start();

        System.out.println("Kernel_client: App Server started, listening on port: " + app_server_port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("Kernel_client: shutting down App_gRPC server since JVM is shutting down");
                KernelClient.this.app_server_stop();
                System.err.println("Kernel_client: App_gRPC server shut down");
            }
        });
    }

    private void app_server_stop() {
        if (app_server != null) {
            app_server.shutdown();
        }
    }

    private void app_server_blockUntilShutdown() throws InterruptedException {
        if (app_server != null) {
            app_server.awaitTermination();
        }
    }

    static class MgmtgrpcServiceImpl extends MgmtgrpcServiceGrpc.MgmtgrpcServiceImplBase {

        // Function to handle CreateSapphireObject request from application.
        public void createSapphireObject(CreateRequest req, StreamObserver<CreateReply> responseObserver) {

            KernelClient_Client client = new KernelClient_Client(KernelAddress.kernel_server_host, KernelAddress.kernel_server_port);

            CreateReply reply = CreateReply.newBuilder()
                    .setObjId(client.CreateSapphireObject(req).getObjId()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        // Function to handle DeleteSapphireObject request from application.
        public void deleteSapphireObject(DeleteRequest req, StreamObserver<DeleteReply> responseObserver) {

            KernelClient_Client client = new KernelClient_Client(KernelAddress.kernel_server_host, KernelAddress.kernel_server_port);

            DeleteReply reply = DeleteReply.newBuilder()
                    .setFlag(client.DeleteSapphireObject(req).getFlag()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        // Function to handle GenericMethodInvoke request from application.
        public void genericMethodInvoke(GenericMethodRequest req, StreamObserver<GenericMethodReply> responseObserver) {

            KernelClient_Client client = new KernelClient_Client(KernelAddress.kernel_server_host, KernelAddress.kernel_server_port);

            GenericMethodReply reply = GenericMethodReply.newBuilder()
                    .setRet(client.GenericMethodInvoke(req).getRet()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
    // <-- End of code for Application gRPC server running at Local Kernel.

    public static void main(String[] args) throws Exception {

        System.out.println("Kernel_client: Inside Kernel Client main()");

        KernelClient client = new KernelClient();
        client.app_server_start(KernelAddress.app_server_port);
        client.app_server_blockUntilShutdown();

    }
}

/**
 * Created by Jithu Thomas on 14/6/18.
 */
package sapphire.kernel;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;

public class KernelServer {

    private Server server;

    private void start() throws IOException {
        server = ServerBuilder.forPort(KernelAddress.kernel_server_port)
                .addService(new Kernel_MgmtgrpcServiceImpl())
                .build()
                .start();
        System.out.println("Kernel_server: Server started, listening on port: " + KernelAddress.kernel_server_port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("Kernel_server: shutting down gRPC server since JVM is shutting down");
                KernelServer.this.stop();
                System.err.println("Kernel_server: server shut down");
            }
        });
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws Exception {

        System.out.println("Kernel_server: Inside Kernel Server main()");
        final KernelServer server = new KernelServer();
        server.start();
        server.blockUntilShutdown();
    }

    static class Kernel_MgmtgrpcServiceImpl extends Kernel_MgmtgrpcServiceGrpc.Kernel_MgmtgrpcServiceImplBase {

        public void kernelCreateSapphireObject(Kernel_CreateRequest req, StreamObserver<Kernel_CreateReply> responseObserver) {

            KernelServer_Client client = new KernelServer_Client(KernelAddress.sapphire_process_host, KernelAddress.sapphire_process_port);

            Kernel_CreateReply reply = Kernel_CreateReply.newBuilder()
                    .setObjId(client.CreateSapphireObject(req).getObjId()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        public void kernelDeleteSapphireObject(Kernel_DeleteRequest req, StreamObserver<Kernel_DeleteReply> responseObserver) {

            KernelServer_Client client = new KernelServer_Client(KernelAddress.sapphire_process_host, KernelAddress.sapphire_process_port);

            Kernel_DeleteReply reply = Kernel_DeleteReply.newBuilder()
                    .setFlag(client.DeleteSapphireObject(req).getFlag()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        public void kernelGenericMethodInvoke(Kernel_GenericMethodRequest req, StreamObserver<Kernel_GenericMethodReply> responseObserver) {

            KernelServer_Client client =new KernelServer_Client(KernelAddress.sapphire_process_host, KernelAddress.sapphire_process_port);

            Kernel_GenericMethodReply reply = Kernel_GenericMethodReply.newBuilder()
                    .setRet(client.GenericMethodInvoke(req).getRet()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}

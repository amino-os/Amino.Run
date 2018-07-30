package sapphire.kernel.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireReplicaID;
import sapphire.kernel.KernelServerApiToRuntime.CreateChildSObjResponse;
import sapphire.kernel.KernelServerApiToRuntime.DeleteChildSObjResponse;
import sapphire.kernel.KernelServerApiToRuntime.RuntimeKernelServerGrpcServiceGrpc;
import sapphire.kernel.common.DMConfigInfo;

/** Created by Venugopal Reddy K on 23/7/18. */
public class KernelGrpcServer {
    private static final Logger logger = Logger.getLogger(KernelGrpcServer.class.getName());
    private final InetSocketAddress serverAddr;
    private final Server server;
    private final KernelServer handler;

    public KernelGrpcServer(InetSocketAddress serverInetAddr, KernelServer instance)
            throws IOException {
        this(NettyServerBuilder.forAddress(serverInetAddr), serverInetAddr, instance);
    }

    public KernelGrpcServer(
            ServerBuilder<?> serverBuilder, InetSocketAddress serverInetAddr, KernelServer instance)
            throws IOException {
        this.serverAddr = serverInetAddr;
        handler = instance;
        server = serverBuilder.addService(new KernelGrpcServer.KernelService(handler)).build();
    }

    /** Start serving requests. */
    public void start() throws IOException {
        server.start();
        logger.info("Server started, listening on " + serverAddr);
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread() {
                            @Override
                            public void run() {
                                // Use stderr here since the logger may has been reset by its JVM
                                // shutdown hook.
                                System.err.println(
                                        "Shutting down gRPC server since JVM is shutting down");
                                KernelGrpcServer.this.stop();
                                System.err.println("Server shut down");
                            }
                        });
    }

    /** Stop serving requests and shutdown resources. */
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /** Await termination on the main thread since the grpc library uses daemon threads. */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    private static class KernelService
            extends RuntimeKernelServerGrpcServiceGrpc.RuntimeKernelServerGrpcServiceImplBase {
        private final KernelServer handler;

        public KernelService(KernelServer instance) {
            handler = instance;
        }

        @Override
        public void createChildSObj(
                sapphire.kernel.KernelServerApiToRuntime.CreateChildSObjRequest request,
                io.grpc.stub.StreamObserver<
                                sapphire.kernel.KernelServerApiToRuntime.CreateChildSObjResponse>
                        responseObserver) {
            DMConfigInfo dm;
            SapphireReplicaID replicaId;
            try {
                dm =
                        new DMConfigInfo(
                                request.getSObjDMInfo().getClientPolicy(),
                                request.getSObjDMInfo().getServerPolicy(),
                                request.getSObjDMInfo().getGroupPolicy());
                replicaId =
                        handler.createInnerSapphireObject(
                                request.getSObjName(),
                                dm,
                                new SapphireObjectID(
                                        Integer.parseInt(request.getSObjParentSObjId())),
                                request.getSObjReplicaObject().toByteArray());
                CreateChildSObjResponse reply =
                        CreateChildSObjResponse.newBuilder()
                                .setSObjReplicaId(String.valueOf(replicaId.getID()))
                                .setSObjId(String.valueOf(replicaId.getOID().getID()))
                                .build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (Exception e) {
                e.printStackTrace();
                responseObserver.onError(e);
            }
        }

        @Override
        public void deleteChildSObj(
                sapphire.kernel.KernelServerApiToRuntime.DeleteChildSObjRequest request,
                io.grpc.stub.StreamObserver<
                                sapphire.kernel.KernelServerApiToRuntime.DeleteChildSObjResponse>
                        responseObserver) {
            SapphireObjectID sapphireObjId;
            boolean status;
            try {
                sapphireObjId = new SapphireObjectID(Integer.parseInt(request.getSObjId()));
                status = handler.deleteInnerSapphireObject(sapphireObjId);
                DeleteChildSObjResponse reply =
                        DeleteChildSObjResponse.newBuilder().setStatus(status).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (Exception e) {
                e.printStackTrace();
                responseObserver.onError(e);
            }
        }
    }
}

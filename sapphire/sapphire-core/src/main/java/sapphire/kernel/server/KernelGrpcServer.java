package sapphire.kernel.server;

import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Logger;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireReplicaID;
import sapphire.kernel.app.*;
import sapphire.kernel.app.AppServiceGrpc;
import sapphire.kernel.common.DMConfigInfo;
import sapphire.kernel.common.ServerInfo;
import sapphire.kernel.runtime.*;
import sapphire.kernel.runtime.RuntimeServiceGrpc;
import sapphire.oms.OMSServer;
import sapphire.oms.SapphireClientInfo;

/** Created by Venugopal Reddy K 00900280 on 23/7/18. */
public class KernelGrpcServer {
    private static final Logger logger = Logger.getLogger(KernelGrpcServer.class.getName());
    private final InetSocketAddress serverAddr;
    private final Server server;

    public KernelGrpcServer(
            InetSocketAddress serverInetAddr, KernelServer instance, int role, OMSServer oms)
            throws IOException {
        this(NettyServerBuilder.forAddress(serverInetAddr), serverInetAddr, instance, role, oms);
    }

    public KernelGrpcServer(
            ServerBuilder<?> serverBuilder,
            InetSocketAddress serverInetAddr,
            KernelServer instance,
            int role,
            OMSServer oms)
            throws IOException {
        this.serverAddr = serverInetAddr;

        if (ServerInfo.ROLE_KERNEL_CLIENT == role) {
            server =
                    serverBuilder
                            .addService(new KernelGrpcServer.KernelServiceToApp(instance, oms))
                            .build();
        } else {
            server =
                    serverBuilder
                            .addService(new KernelGrpcServer.KernelServiceToRuntime(instance))
                            .build();
        }
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
                                        "Shutting down gRPC server for runtime interaction since JVM is shutting down");
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

    private static class KernelServiceToApp extends AppServiceGrpc.AppServiceImplBase {
        private final KernelServer kernelServerhandler;
        private final OMSServer omsHandler;

        public KernelServiceToApp(KernelServer kernelServer, OMSServer oms) {
            kernelServerhandler = kernelServer;
            omsHandler = oms;
        }

        @Override
        public void createSapphireObject(
                CreateRequest request,
                io.grpc.stub.StreamObserver<CreateResponse> responseObserver) {
            SapphireObjectID sapphireObjId;
            try {
                sapphireObjId =
                        omsHandler.createSapphireObject(
                                request.getSoName(),
                                request.getLangType(),
                                request.getConstructName(),
                                request.getConstructParams().toByteArray());
                CreateResponse reply =
                        CreateResponse.newBuilder()
                                .setSId(String.valueOf(sapphireObjId.getID()))
                                .build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (Exception e) {
                e.printStackTrace();
                responseObserver.onError(e);
            }
        }

        @Override
        public void deleteSapphireObject(
                DeleteRequest request,
                io.grpc.stub.StreamObserver<DeleteResponse> responseObserver) {
            boolean status;
            try {
                status =
                        omsHandler.deleteSapphireObject(
                                new SapphireObjectID(Integer.parseInt(request.getSId())));
                DeleteResponse reply = DeleteResponse.newBuilder().setStatus(status).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (Exception e) {
                e.printStackTrace();
                responseObserver.onError(e);
            }
        }

        @Override
        public void attach(
                AttachRequest request,
                io.grpc.stub.StreamObserver<AttachResponse> responseObserver) {
            SapphireClientInfo clientInfo;
            try {
                String clientRMIIpPort = request.getDmClientRmiEndPoint();
                if (clientRMIIpPort.indexOf(':') > -1) {
                    String[] host = clientRMIIpPort.split(":");
                    clientInfo =
                            omsHandler.attachToSapphireObject(
                                    request.getUrl(),
                                    new InetSocketAddress(host[0], Integer.parseInt(host[1])));
                    AttachResponse reply =
                            AttachResponse.newBuilder()
                                    .setClientId(String.valueOf(clientInfo.getClientId()))
                                    .setSId(String.valueOf(clientInfo.getSapphireId()))
                                    .setObjectStream(
                                            ByteString.copyFrom(clientInfo.getOpaqueObject()))
                                    .build();
                    responseObserver.onNext(reply);
                    responseObserver.onCompleted();
                } else {
                    throw new UnknownHostException(
                            "Invalid host :" + request.getDmClientRmiEndPoint());
                }

            } catch (Exception e) {
                e.printStackTrace();
                responseObserver.onError(e);
            }
        }

        @Override
        public void detach(
                DetachRequest request,
                io.grpc.stub.StreamObserver<DetachResponse> responseObserver) {
            boolean status;
            try {
                status =
                        omsHandler.detachFromSapphireObject(
                                new SapphireObjectID(Integer.parseInt(request.getSId())),
                                Integer.parseInt(request.getClientId()));
                DetachResponse reply = DetachResponse.newBuilder().setStatus(status).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (Exception e) {
                e.printStackTrace();
                responseObserver.onError(e);
            }
        }

        @Override
        public void setURL(
                URLRequest request, io.grpc.stub.StreamObserver<URLResponse> responseObserver) {
            try {
                omsHandler.setSapphireObjectName(
                        new SapphireObjectID(Integer.parseInt(request.getSId())), request.getUrl());
                URLResponse reply = URLResponse.newBuilder().setStatus(true).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (Exception e) {
                e.printStackTrace();
                responseObserver.onError(e);
            }
        }

        @Override
        public void acquireAppStub(
                AcquireRequest request,
                io.grpc.stub.StreamObserver<AcquireResponse> responseObserver) {
            SapphireClientInfo clientInfo;
            try {
                String clientRMIIpPort = request.getDmClientRmiEndPoint();
                if (clientRMIIpPort.indexOf(':') > -1) {
                    String[] host = clientRMIIpPort.split(":");
                    clientInfo =
                            omsHandler.acquireSapphireObjectStub(
                                    new SapphireObjectID(Integer.parseInt(request.getSId())),
                                    new InetSocketAddress(host[0], Integer.parseInt(host[1])));
                    AcquireResponse reply =
                            AcquireResponse.newBuilder()
                                    .setClientId(String.valueOf(clientInfo.getClientId()))
                                    .setObjectStream(
                                            ByteString.copyFrom(clientInfo.getOpaqueObject()))
                                    .build();
                    responseObserver.onNext(reply);
                    responseObserver.onCompleted();
                } else {
                    throw new UnknownHostException(
                            "Invalid host :" + request.getDmClientRmiEndPoint());
                }
            } catch (Exception e) {
                e.printStackTrace();
                responseObserver.onError(e);
            }
        }

        @Override
        public void releaseAppStub(
                ReleaseRequest request,
                io.grpc.stub.StreamObserver<ReleaseResponse> responseObserver) {
            boolean status;
            try {
                status =
                        omsHandler.releaseSapphireObjectStub(
                                new SapphireObjectID(Integer.parseInt(request.getSId())),
                                Integer.parseInt(request.getClientId()));
                ReleaseResponse reply = ReleaseResponse.newBuilder().setStatus(status).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (Exception e) {
                e.printStackTrace();
                responseObserver.onError(e);
            }
        }

        @Override
        public void genericInvoke(
                InvokeRequest request,
                io.grpc.stub.StreamObserver<InvokeResponse> responseObserver) {
            try {
                final ByteString inStream = request.getFuncParams();
                ArrayList<Object> params = new ArrayList<Object>();
                params.add(inStream);
                ByteString outStream =
                        kernelServerhandler.genericInvoke(
                                request.getDMClientId(), request.getFuncName(), params);
                InvokeResponse reply =
                        InvokeResponse.newBuilder().setObjectStream(outStream).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (Exception e) {
                e.printStackTrace();
                responseObserver.onError(e);
            }
        }
    }

    private static class KernelServiceToRuntime extends RuntimeServiceGrpc.RuntimeServiceImplBase {
        private final KernelServer handler;

        public KernelServiceToRuntime(KernelServer instance) {
            handler = instance;
        }

        @Override
        public void createChildSObj(
                CreateChildSObjRequest request,
                io.grpc.stub.StreamObserver<CreateChildSObjResponse> responseObserver) {
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
                DeleteChildSObjRequest request,
                io.grpc.stub.StreamObserver<DeleteChildSObjResponse> responseObserver) {
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

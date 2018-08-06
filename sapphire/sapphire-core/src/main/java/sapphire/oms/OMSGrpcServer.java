package sapphire.oms;

import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.UnknownHostException;
import java.util.logging.Logger;
import sapphire.common.SapphireObjectID;
import sapphire.oms.OmsApiToApp.OMSServiceGrpc;
import sapphire.oms.OmsApiToApp.OmsApiToApp;

/** Created by Venugopal Reddy K 00900280 on 22/7/18. */
public class OMSGrpcServer {
    private static final Logger logger = Logger.getLogger(OMSGrpcServer.class.getName());
    private final Server server;
    private final OMSServer handler;

    public OMSGrpcServer(InetSocketAddress serverInetAddr, OMSServer omsInstance)
            throws IOException {
        this(NettyServerBuilder.forAddress(serverInetAddr), serverInetAddr, omsInstance);
    }

    public OMSGrpcServer(
            ServerBuilder<?> serverBuilder, InetSocketAddress serverInetAddr, OMSServer omsInstance)
            throws IOException {
        handler = omsInstance;
        server = serverBuilder.addService(new OMSService(handler)).build();
    }

    /** Start serving requests. */
    public void start() throws IOException {
        server.start();
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread() {
                            @Override
                            public void run() {
                                // Use stderr here since the logger may has been reset by its JVM
                                // shutdown hook.
                                System.err.println(
                                        "Shutting down gRPC server since JVM is shutting down");
                                OMSGrpcServer.this.stop();
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

    private static class OMSService extends OMSServiceGrpc.OMSServiceImplBase {
        private final OMSServer handler;

        public OMSService(OMSServer omsInstance) {
            handler = omsInstance;
        }

        @Override
        public void createSapphireObject(
                sapphire.oms.OmsApiToApp.OmsApiToApp.CreateRequest request,
                io.grpc.stub.StreamObserver<sapphire.oms.OmsApiToApp.OmsApiToApp.CreateResponse>
                        responseObserver) {
            SapphireObjectID sapphireObjId;
            try {
                sapphireObjId =
                        handler.createSapphireObject(
                                request.getSoName(),
                                request.getLangType(),
                                request.getConstructName(),
                                request.getConstructParams().toByteArray());
                OmsApiToApp.CreateResponse reply =
                        OmsApiToApp.CreateResponse.newBuilder()
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
                sapphire.oms.OmsApiToApp.OmsApiToApp.DeleteRequest request,
                io.grpc.stub.StreamObserver<sapphire.oms.OmsApiToApp.OmsApiToApp.DeleteResponse>
                        responseObserver) {
            boolean status;
            try {
                status =
                        handler.deleteSapphireObject(
                                new SapphireObjectID(Integer.parseInt(request.getSId())));
                OmsApiToApp.DeleteResponse reply =
                        OmsApiToApp.DeleteResponse.newBuilder().setStatus(status).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (Exception e) {
                e.printStackTrace();
                responseObserver.onError(e);
            }
        }

        @Override
        public void attach(
                sapphire.oms.OmsApiToApp.OmsApiToApp.AttachRequest request,
                io.grpc.stub.StreamObserver<sapphire.oms.OmsApiToApp.OmsApiToApp.AttachResponse>
                        responseObserver) {
            SapphireClientInfo clientInfo;
            try {
                String clientRMIIpPort = request.getDmClientRmiEndPoint();
                if (clientRMIIpPort.indexOf(':') > -1) {
                    String[] host = clientRMIIpPort.split(":");
                    clientInfo =
                            handler.attachToSapphireObject(
                                    request.getUrl(),
                                    new InetSocketAddress(host[0], Integer.parseInt(host[1])));
                    OmsApiToApp.AttachResponse reply =
                            OmsApiToApp.AttachResponse.newBuilder()
                                    .setClientId(String.valueOf(clientInfo.getClientId())).setSId(String.valueOf(clientInfo.getSapphireId()))
                                    .setObjectStream(
                                            ByteString.copyFrom(clientInfo.getOpaqueObject()))
                                    .build();
                    responseObserver.onNext(reply);
                    responseObserver.onCompleted();
                } else {
                    throw new UnknownHostException(
                            "Invalid host " + request.getDmClientRmiEndPoint());
                }

            } catch (Exception e) {
                e.printStackTrace();
                responseObserver.onError(e);
            }
        }

        @Override
        public void detach(
                sapphire.oms.OmsApiToApp.OmsApiToApp.DetachRequest request,
                io.grpc.stub.StreamObserver<sapphire.oms.OmsApiToApp.OmsApiToApp.DetachResponse>
                        responseObserver) {
            boolean status;
            try {
                status = handler.detachFromSapphireObject(new SapphireObjectID(Integer.parseInt(request.getSId())), Integer.parseInt(request.getClientId()));
                OmsApiToApp.DetachResponse reply =
                        OmsApiToApp.DetachResponse.newBuilder().setStatus(status).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (Exception e) {
                e.printStackTrace();
                responseObserver.onError(e);
            }
        }

        @Override
        public void setURL(
                sapphire.oms.OmsApiToApp.OmsApiToApp.URLRequest request,
                io.grpc.stub.StreamObserver<sapphire.oms.OmsApiToApp.OmsApiToApp.URLResponse>
                        responseObserver) {
            try {
                handler.setSapphireObjectName(
                        new SapphireObjectID(Integer.parseInt(request.getSId())), request.getUrl());
                OmsApiToApp.URLResponse reply =
                        OmsApiToApp.URLResponse.newBuilder().setStatus(true).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (Exception e) {
                e.printStackTrace();
                responseObserver.onError(e);
            }
        }

        @Override
        public void acquireAppStub(
                sapphire.oms.OmsApiToApp.OmsApiToApp.AcquireRequest request,
                io.grpc.stub.StreamObserver<sapphire.oms.OmsApiToApp.OmsApiToApp.AcquireResponse>
                        responseObserver) {
            SapphireClientInfo clientInfo;
            try {
                String clientRMIIpPort = request.getDmClientRmiEndPoint();
                if (clientRMIIpPort.indexOf(':') > -1) {
                    String[] host = clientRMIIpPort.split(":");
                    clientInfo =
                            handler.acquireSapphireObjectStub(
                                    new SapphireObjectID(Integer.parseInt(request.getSId())),
                                    new InetSocketAddress(host[0], Integer.parseInt(host[1])));
                    OmsApiToApp.AcquireResponse reply =
                            OmsApiToApp.AcquireResponse
                                    .newBuilder()
                                    .setClientId(String.valueOf(clientInfo.getClientId()))
                                    .setObjectStream(
                                            ByteString.copyFrom(clientInfo.getOpaqueObject()))
                                    .build();
                    responseObserver.onNext(reply);
                    responseObserver.onCompleted();
                } else {
                    throw new UnknownHostException(
                            "Invalid host " + request.getDmClientRmiEndPoint());
                }
            } catch (Exception e) {
                e.printStackTrace();
                responseObserver.onError(e);
            }
        }

        @Override
        public void releaseAppStub(
                sapphire.oms.OmsApiToApp.OmsApiToApp.ReleaseRequest request,
                io.grpc.stub.StreamObserver<sapphire.oms.OmsApiToApp.OmsApiToApp.ReleaseResponse>
                        responseObserver) {
            boolean status;
            try {
                status =
                        handler.releaseSapphireObjectStub(
                                new SapphireObjectID(Integer.parseInt(request.getSId())),
                                Integer.parseInt(request.getClientId()));
                OmsApiToApp.ReleaseResponse reply =
                        OmsApiToApp.ReleaseResponse.newBuilder().setStatus(status).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (Exception e) {
                e.printStackTrace();
                responseObserver.onError(e);
            }
        }
        /*
        @Override
        public void genericInvoke() {

        } */
    }
}

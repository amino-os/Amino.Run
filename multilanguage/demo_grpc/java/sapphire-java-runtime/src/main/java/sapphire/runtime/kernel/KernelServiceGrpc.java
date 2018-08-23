package sapphire.runtime.kernel;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.9.0)",
    comments = "Source: RuntimeApiToKernelServer.proto")
public final class KernelServiceGrpc {

  private KernelServiceGrpc() {}

  public static final String SERVICE_NAME = "sapphire.runtime.kernel.KernelService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateSObjReplicaMethod()} instead. 
  public static final io.grpc.MethodDescriptor<sapphire.runtime.kernel.CreateSObjReplicaRequest,
      sapphire.runtime.kernel.CreateSObjReplicaResponse> METHOD_CREATE_SOBJ_REPLICA = getCreateSObjReplicaMethod();

  private static volatile io.grpc.MethodDescriptor<sapphire.runtime.kernel.CreateSObjReplicaRequest,
      sapphire.runtime.kernel.CreateSObjReplicaResponse> getCreateSObjReplicaMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<sapphire.runtime.kernel.CreateSObjReplicaRequest,
      sapphire.runtime.kernel.CreateSObjReplicaResponse> getCreateSObjReplicaMethod() {
    io.grpc.MethodDescriptor<sapphire.runtime.kernel.CreateSObjReplicaRequest, sapphire.runtime.kernel.CreateSObjReplicaResponse> getCreateSObjReplicaMethod;
    if ((getCreateSObjReplicaMethod = KernelServiceGrpc.getCreateSObjReplicaMethod) == null) {
      synchronized (KernelServiceGrpc.class) {
        if ((getCreateSObjReplicaMethod = KernelServiceGrpc.getCreateSObjReplicaMethod) == null) {
          KernelServiceGrpc.getCreateSObjReplicaMethod = getCreateSObjReplicaMethod = 
              io.grpc.MethodDescriptor.<sapphire.runtime.kernel.CreateSObjReplicaRequest, sapphire.runtime.kernel.CreateSObjReplicaResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sapphire.runtime.kernel.KernelService", "CreateSObjReplica"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.runtime.kernel.CreateSObjReplicaRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.runtime.kernel.CreateSObjReplicaResponse.getDefaultInstance()))
                  .build();
          }
        }
     }
     return getCreateSObjReplicaMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getDeleteSObjReplicaMethod()} instead. 
  public static final io.grpc.MethodDescriptor<sapphire.runtime.kernel.DeleteSObjReplicaRequest,
      sapphire.runtime.kernel.DeleteSObjReplicaResponse> METHOD_DELETE_SOBJ_REPLICA = getDeleteSObjReplicaMethod();

  private static volatile io.grpc.MethodDescriptor<sapphire.runtime.kernel.DeleteSObjReplicaRequest,
      sapphire.runtime.kernel.DeleteSObjReplicaResponse> getDeleteSObjReplicaMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<sapphire.runtime.kernel.DeleteSObjReplicaRequest,
      sapphire.runtime.kernel.DeleteSObjReplicaResponse> getDeleteSObjReplicaMethod() {
    io.grpc.MethodDescriptor<sapphire.runtime.kernel.DeleteSObjReplicaRequest, sapphire.runtime.kernel.DeleteSObjReplicaResponse> getDeleteSObjReplicaMethod;
    if ((getDeleteSObjReplicaMethod = KernelServiceGrpc.getDeleteSObjReplicaMethod) == null) {
      synchronized (KernelServiceGrpc.class) {
        if ((getDeleteSObjReplicaMethod = KernelServiceGrpc.getDeleteSObjReplicaMethod) == null) {
          KernelServiceGrpc.getDeleteSObjReplicaMethod = getDeleteSObjReplicaMethod = 
              io.grpc.MethodDescriptor.<sapphire.runtime.kernel.DeleteSObjReplicaRequest, sapphire.runtime.kernel.DeleteSObjReplicaResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sapphire.runtime.kernel.KernelService", "DeleteSObjReplica"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.runtime.kernel.DeleteSObjReplicaRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.runtime.kernel.DeleteSObjReplicaResponse.getDefaultInstance()))
                  .build();
          }
        }
     }
     return getDeleteSObjReplicaMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getSObjMethodInvokeMethod()} instead. 
  public static final io.grpc.MethodDescriptor<sapphire.runtime.kernel.SObjMethodInvokeRequest,
      sapphire.runtime.kernel.SObjMethodInvokeResponse> METHOD_SOBJ_METHOD_INVOKE = getSObjMethodInvokeMethod();

  private static volatile io.grpc.MethodDescriptor<sapphire.runtime.kernel.SObjMethodInvokeRequest,
      sapphire.runtime.kernel.SObjMethodInvokeResponse> getSObjMethodInvokeMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<sapphire.runtime.kernel.SObjMethodInvokeRequest,
      sapphire.runtime.kernel.SObjMethodInvokeResponse> getSObjMethodInvokeMethod() {
    io.grpc.MethodDescriptor<sapphire.runtime.kernel.SObjMethodInvokeRequest, sapphire.runtime.kernel.SObjMethodInvokeResponse> getSObjMethodInvokeMethod;
    if ((getSObjMethodInvokeMethod = KernelServiceGrpc.getSObjMethodInvokeMethod) == null) {
      synchronized (KernelServiceGrpc.class) {
        if ((getSObjMethodInvokeMethod = KernelServiceGrpc.getSObjMethodInvokeMethod) == null) {
          KernelServiceGrpc.getSObjMethodInvokeMethod = getSObjMethodInvokeMethod = 
              io.grpc.MethodDescriptor.<sapphire.runtime.kernel.SObjMethodInvokeRequest, sapphire.runtime.kernel.SObjMethodInvokeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sapphire.runtime.kernel.KernelService", "SObjMethodInvoke"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.runtime.kernel.SObjMethodInvokeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.runtime.kernel.SObjMethodInvokeResponse.getDefaultInstance()))
                  .build();
          }
        }
     }
     return getSObjMethodInvokeMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static KernelServiceStub newStub(io.grpc.Channel channel) {
    return new KernelServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static KernelServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new KernelServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static KernelServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new KernelServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class KernelServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void createSObjReplica(sapphire.runtime.kernel.CreateSObjReplicaRequest request,
        io.grpc.stub.StreamObserver<sapphire.runtime.kernel.CreateSObjReplicaResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateSObjReplicaMethod(), responseObserver);
    }

    /**
     */
    public void deleteSObjReplica(sapphire.runtime.kernel.DeleteSObjReplicaRequest request,
        io.grpc.stub.StreamObserver<sapphire.runtime.kernel.DeleteSObjReplicaResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteSObjReplicaMethod(), responseObserver);
    }

    /**
     */
    public void sObjMethodInvoke(sapphire.runtime.kernel.SObjMethodInvokeRequest request,
        io.grpc.stub.StreamObserver<sapphire.runtime.kernel.SObjMethodInvokeResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSObjMethodInvokeMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getCreateSObjReplicaMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                sapphire.runtime.kernel.CreateSObjReplicaRequest,
                sapphire.runtime.kernel.CreateSObjReplicaResponse>(
                  this, METHODID_CREATE_SOBJ_REPLICA)))
          .addMethod(
            getDeleteSObjReplicaMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                sapphire.runtime.kernel.DeleteSObjReplicaRequest,
                sapphire.runtime.kernel.DeleteSObjReplicaResponse>(
                  this, METHODID_DELETE_SOBJ_REPLICA)))
          .addMethod(
            getSObjMethodInvokeMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                sapphire.runtime.kernel.SObjMethodInvokeRequest,
                sapphire.runtime.kernel.SObjMethodInvokeResponse>(
                  this, METHODID_SOBJ_METHOD_INVOKE)))
          .build();
    }
  }

  /**
   */
  public static final class KernelServiceStub extends io.grpc.stub.AbstractStub<KernelServiceStub> {
    private KernelServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private KernelServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected KernelServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new KernelServiceStub(channel, callOptions);
    }

    /**
     */
    public void createSObjReplica(sapphire.runtime.kernel.CreateSObjReplicaRequest request,
        io.grpc.stub.StreamObserver<sapphire.runtime.kernel.CreateSObjReplicaResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateSObjReplicaMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void deleteSObjReplica(sapphire.runtime.kernel.DeleteSObjReplicaRequest request,
        io.grpc.stub.StreamObserver<sapphire.runtime.kernel.DeleteSObjReplicaResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteSObjReplicaMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void sObjMethodInvoke(sapphire.runtime.kernel.SObjMethodInvokeRequest request,
        io.grpc.stub.StreamObserver<sapphire.runtime.kernel.SObjMethodInvokeResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSObjMethodInvokeMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class KernelServiceBlockingStub extends io.grpc.stub.AbstractStub<KernelServiceBlockingStub> {
    private KernelServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private KernelServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected KernelServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new KernelServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public sapphire.runtime.kernel.CreateSObjReplicaResponse createSObjReplica(sapphire.runtime.kernel.CreateSObjReplicaRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateSObjReplicaMethod(), getCallOptions(), request);
    }

    /**
     */
    public sapphire.runtime.kernel.DeleteSObjReplicaResponse deleteSObjReplica(sapphire.runtime.kernel.DeleteSObjReplicaRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteSObjReplicaMethod(), getCallOptions(), request);
    }

    /**
     */
    public sapphire.runtime.kernel.SObjMethodInvokeResponse sObjMethodInvoke(sapphire.runtime.kernel.SObjMethodInvokeRequest request) {
      return blockingUnaryCall(
          getChannel(), getSObjMethodInvokeMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class KernelServiceFutureStub extends io.grpc.stub.AbstractStub<KernelServiceFutureStub> {
    private KernelServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private KernelServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected KernelServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new KernelServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<sapphire.runtime.kernel.CreateSObjReplicaResponse> createSObjReplica(
        sapphire.runtime.kernel.CreateSObjReplicaRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateSObjReplicaMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<sapphire.runtime.kernel.DeleteSObjReplicaResponse> deleteSObjReplica(
        sapphire.runtime.kernel.DeleteSObjReplicaRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteSObjReplicaMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<sapphire.runtime.kernel.SObjMethodInvokeResponse> sObjMethodInvoke(
        sapphire.runtime.kernel.SObjMethodInvokeRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSObjMethodInvokeMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_SOBJ_REPLICA = 0;
  private static final int METHODID_DELETE_SOBJ_REPLICA = 1;
  private static final int METHODID_SOBJ_METHOD_INVOKE = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final KernelServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(KernelServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_SOBJ_REPLICA:
          serviceImpl.createSObjReplica((sapphire.runtime.kernel.CreateSObjReplicaRequest) request,
              (io.grpc.stub.StreamObserver<sapphire.runtime.kernel.CreateSObjReplicaResponse>) responseObserver);
          break;
        case METHODID_DELETE_SOBJ_REPLICA:
          serviceImpl.deleteSObjReplica((sapphire.runtime.kernel.DeleteSObjReplicaRequest) request,
              (io.grpc.stub.StreamObserver<sapphire.runtime.kernel.DeleteSObjReplicaResponse>) responseObserver);
          break;
        case METHODID_SOBJ_METHOD_INVOKE:
          serviceImpl.sObjMethodInvoke((sapphire.runtime.kernel.SObjMethodInvokeRequest) request,
              (io.grpc.stub.StreamObserver<sapphire.runtime.kernel.SObjMethodInvokeResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (KernelServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .addMethod(getCreateSObjReplicaMethod())
              .addMethod(getDeleteSObjReplicaMethod())
              .addMethod(getSObjMethodInvokeMethod())
              .build();
        }
      }
    }
    return result;
  }
}

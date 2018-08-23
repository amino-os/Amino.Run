package sapphire.kernel.runtime;

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
    comments = "Source: KernelServerApiToRuntime.proto")
public final class RuntimeServiceGrpc {

  private RuntimeServiceGrpc() {}

  public static final String SERVICE_NAME = "sapphire.kernel.runtime.RuntimeService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateChildSObjMethod()} instead. 
  public static final io.grpc.MethodDescriptor<sapphire.kernel.runtime.CreateChildSObjRequest,
      sapphire.kernel.runtime.CreateChildSObjResponse> METHOD_CREATE_CHILD_SOBJ = getCreateChildSObjMethod();

  private static volatile io.grpc.MethodDescriptor<sapphire.kernel.runtime.CreateChildSObjRequest,
      sapphire.kernel.runtime.CreateChildSObjResponse> getCreateChildSObjMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<sapphire.kernel.runtime.CreateChildSObjRequest,
      sapphire.kernel.runtime.CreateChildSObjResponse> getCreateChildSObjMethod() {
    io.grpc.MethodDescriptor<sapphire.kernel.runtime.CreateChildSObjRequest, sapphire.kernel.runtime.CreateChildSObjResponse> getCreateChildSObjMethod;
    if ((getCreateChildSObjMethod = RuntimeServiceGrpc.getCreateChildSObjMethod) == null) {
      synchronized (RuntimeServiceGrpc.class) {
        if ((getCreateChildSObjMethod = RuntimeServiceGrpc.getCreateChildSObjMethod) == null) {
          RuntimeServiceGrpc.getCreateChildSObjMethod = getCreateChildSObjMethod = 
              io.grpc.MethodDescriptor.<sapphire.kernel.runtime.CreateChildSObjRequest, sapphire.kernel.runtime.CreateChildSObjResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sapphire.kernel.runtime.RuntimeService", "CreateChildSObj"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.runtime.CreateChildSObjRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.runtime.CreateChildSObjResponse.getDefaultInstance()))
                  .build();
          }
        }
     }
     return getCreateChildSObjMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getDeleteChildSObjMethod()} instead. 
  public static final io.grpc.MethodDescriptor<sapphire.kernel.runtime.DeleteChildSObjRequest,
      sapphire.kernel.runtime.DeleteChildSObjResponse> METHOD_DELETE_CHILD_SOBJ = getDeleteChildSObjMethod();

  private static volatile io.grpc.MethodDescriptor<sapphire.kernel.runtime.DeleteChildSObjRequest,
      sapphire.kernel.runtime.DeleteChildSObjResponse> getDeleteChildSObjMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<sapphire.kernel.runtime.DeleteChildSObjRequest,
      sapphire.kernel.runtime.DeleteChildSObjResponse> getDeleteChildSObjMethod() {
    io.grpc.MethodDescriptor<sapphire.kernel.runtime.DeleteChildSObjRequest, sapphire.kernel.runtime.DeleteChildSObjResponse> getDeleteChildSObjMethod;
    if ((getDeleteChildSObjMethod = RuntimeServiceGrpc.getDeleteChildSObjMethod) == null) {
      synchronized (RuntimeServiceGrpc.class) {
        if ((getDeleteChildSObjMethod = RuntimeServiceGrpc.getDeleteChildSObjMethod) == null) {
          RuntimeServiceGrpc.getDeleteChildSObjMethod = getDeleteChildSObjMethod = 
              io.grpc.MethodDescriptor.<sapphire.kernel.runtime.DeleteChildSObjRequest, sapphire.kernel.runtime.DeleteChildSObjResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sapphire.kernel.runtime.RuntimeService", "DeleteChildSObj"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.runtime.DeleteChildSObjRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.runtime.DeleteChildSObjResponse.getDefaultInstance()))
                  .build();
          }
        }
     }
     return getDeleteChildSObjMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RuntimeServiceStub newStub(io.grpc.Channel channel) {
    return new RuntimeServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RuntimeServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new RuntimeServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RuntimeServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new RuntimeServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class RuntimeServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void createChildSObj(sapphire.kernel.runtime.CreateChildSObjRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.runtime.CreateChildSObjResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateChildSObjMethod(), responseObserver);
    }

    /**
     */
    public void deleteChildSObj(sapphire.kernel.runtime.DeleteChildSObjRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.runtime.DeleteChildSObjResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteChildSObjMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getCreateChildSObjMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                sapphire.kernel.runtime.CreateChildSObjRequest,
                sapphire.kernel.runtime.CreateChildSObjResponse>(
                  this, METHODID_CREATE_CHILD_SOBJ)))
          .addMethod(
            getDeleteChildSObjMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                sapphire.kernel.runtime.DeleteChildSObjRequest,
                sapphire.kernel.runtime.DeleteChildSObjResponse>(
                  this, METHODID_DELETE_CHILD_SOBJ)))
          .build();
    }
  }

  /**
   */
  public static final class RuntimeServiceStub extends io.grpc.stub.AbstractStub<RuntimeServiceStub> {
    private RuntimeServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private RuntimeServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RuntimeServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new RuntimeServiceStub(channel, callOptions);
    }

    /**
     */
    public void createChildSObj(sapphire.kernel.runtime.CreateChildSObjRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.runtime.CreateChildSObjResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateChildSObjMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void deleteChildSObj(sapphire.kernel.runtime.DeleteChildSObjRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.runtime.DeleteChildSObjResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteChildSObjMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class RuntimeServiceBlockingStub extends io.grpc.stub.AbstractStub<RuntimeServiceBlockingStub> {
    private RuntimeServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private RuntimeServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RuntimeServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new RuntimeServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public sapphire.kernel.runtime.CreateChildSObjResponse createChildSObj(sapphire.kernel.runtime.CreateChildSObjRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateChildSObjMethod(), getCallOptions(), request);
    }

    /**
     */
    public sapphire.kernel.runtime.DeleteChildSObjResponse deleteChildSObj(sapphire.kernel.runtime.DeleteChildSObjRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteChildSObjMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class RuntimeServiceFutureStub extends io.grpc.stub.AbstractStub<RuntimeServiceFutureStub> {
    private RuntimeServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private RuntimeServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RuntimeServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new RuntimeServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<sapphire.kernel.runtime.CreateChildSObjResponse> createChildSObj(
        sapphire.kernel.runtime.CreateChildSObjRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateChildSObjMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<sapphire.kernel.runtime.DeleteChildSObjResponse> deleteChildSObj(
        sapphire.kernel.runtime.DeleteChildSObjRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteChildSObjMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_CHILD_SOBJ = 0;
  private static final int METHODID_DELETE_CHILD_SOBJ = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final RuntimeServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(RuntimeServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_CHILD_SOBJ:
          serviceImpl.createChildSObj((sapphire.kernel.runtime.CreateChildSObjRequest) request,
              (io.grpc.stub.StreamObserver<sapphire.kernel.runtime.CreateChildSObjResponse>) responseObserver);
          break;
        case METHODID_DELETE_CHILD_SOBJ:
          serviceImpl.deleteChildSObj((sapphire.kernel.runtime.DeleteChildSObjRequest) request,
              (io.grpc.stub.StreamObserver<sapphire.kernel.runtime.DeleteChildSObjResponse>) responseObserver);
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
      synchronized (RuntimeServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .addMethod(getCreateChildSObjMethod())
              .addMethod(getDeleteChildSObjMethod())
              .build();
        }
      }
    }
    return result;
  }
}

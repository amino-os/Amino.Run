package sapphire.api;

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
    comments = "Source: sapphire_proto.proto")
public final class MgmtgrpcServiceGrpc {

  private MgmtgrpcServiceGrpc() {}

  public static final String SERVICE_NAME = "sapphire_proto.MgmtgrpcService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateSapphireObjectMethod()} instead. 
  public static final io.grpc.MethodDescriptor<sapphire.api.CreateRequest,
      sapphire.api.CreateReply> METHOD_CREATE_SAPPHIRE_OBJECT = getCreateSapphireObjectMethod();

  private static volatile io.grpc.MethodDescriptor<sapphire.api.CreateRequest,
      sapphire.api.CreateReply> getCreateSapphireObjectMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<sapphire.api.CreateRequest,
      sapphire.api.CreateReply> getCreateSapphireObjectMethod() {
    io.grpc.MethodDescriptor<sapphire.api.CreateRequest, sapphire.api.CreateReply> getCreateSapphireObjectMethod;
    if ((getCreateSapphireObjectMethod = MgmtgrpcServiceGrpc.getCreateSapphireObjectMethod) == null) {
      synchronized (MgmtgrpcServiceGrpc.class) {
        if ((getCreateSapphireObjectMethod = MgmtgrpcServiceGrpc.getCreateSapphireObjectMethod) == null) {
          MgmtgrpcServiceGrpc.getCreateSapphireObjectMethod = getCreateSapphireObjectMethod = 
              io.grpc.MethodDescriptor.<sapphire.api.CreateRequest, sapphire.api.CreateReply>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sapphire_proto.MgmtgrpcService", "CreateSapphireObject"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  sapphire.api.CreateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  sapphire.api.CreateReply.getDefaultInstance()))
                  .setSchemaDescriptor(new MgmtgrpcServiceMethodDescriptorSupplier("CreateSapphireObject"))
                  .build();
          }
        }
     }
     return getCreateSapphireObjectMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getDeleteSapphireObjectMethod()} instead. 
  public static final io.grpc.MethodDescriptor<sapphire.api.DeleteRequest,
      sapphire.api.DeleteReply> METHOD_DELETE_SAPPHIRE_OBJECT = getDeleteSapphireObjectMethod();

  private static volatile io.grpc.MethodDescriptor<sapphire.api.DeleteRequest,
      sapphire.api.DeleteReply> getDeleteSapphireObjectMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<sapphire.api.DeleteRequest,
      sapphire.api.DeleteReply> getDeleteSapphireObjectMethod() {
    io.grpc.MethodDescriptor<sapphire.api.DeleteRequest, sapphire.api.DeleteReply> getDeleteSapphireObjectMethod;
    if ((getDeleteSapphireObjectMethod = MgmtgrpcServiceGrpc.getDeleteSapphireObjectMethod) == null) {
      synchronized (MgmtgrpcServiceGrpc.class) {
        if ((getDeleteSapphireObjectMethod = MgmtgrpcServiceGrpc.getDeleteSapphireObjectMethod) == null) {
          MgmtgrpcServiceGrpc.getDeleteSapphireObjectMethod = getDeleteSapphireObjectMethod = 
              io.grpc.MethodDescriptor.<sapphire.api.DeleteRequest, sapphire.api.DeleteReply>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sapphire_proto.MgmtgrpcService", "DeleteSapphireObject"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  sapphire.api.DeleteRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  sapphire.api.DeleteReply.getDefaultInstance()))
                  .setSchemaDescriptor(new MgmtgrpcServiceMethodDescriptorSupplier("DeleteSapphireObject"))
                  .build();
          }
        }
     }
     return getDeleteSapphireObjectMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGenericMethodInvokeMethod()} instead. 
  public static final io.grpc.MethodDescriptor<sapphire.api.GenericMethodRequest,
      sapphire.api.GenericMethodReply> METHOD_GENERIC_METHOD_INVOKE = getGenericMethodInvokeMethod();

  private static volatile io.grpc.MethodDescriptor<sapphire.api.GenericMethodRequest,
      sapphire.api.GenericMethodReply> getGenericMethodInvokeMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<sapphire.api.GenericMethodRequest,
      sapphire.api.GenericMethodReply> getGenericMethodInvokeMethod() {
    io.grpc.MethodDescriptor<sapphire.api.GenericMethodRequest, sapphire.api.GenericMethodReply> getGenericMethodInvokeMethod;
    if ((getGenericMethodInvokeMethod = MgmtgrpcServiceGrpc.getGenericMethodInvokeMethod) == null) {
      synchronized (MgmtgrpcServiceGrpc.class) {
        if ((getGenericMethodInvokeMethod = MgmtgrpcServiceGrpc.getGenericMethodInvokeMethod) == null) {
          MgmtgrpcServiceGrpc.getGenericMethodInvokeMethod = getGenericMethodInvokeMethod = 
              io.grpc.MethodDescriptor.<sapphire.api.GenericMethodRequest, sapphire.api.GenericMethodReply>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sapphire_proto.MgmtgrpcService", "GenericMethodInvoke"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  sapphire.api.GenericMethodRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  sapphire.api.GenericMethodReply.getDefaultInstance()))
                  .setSchemaDescriptor(new MgmtgrpcServiceMethodDescriptorSupplier("GenericMethodInvoke"))
                  .build();
          }
        }
     }
     return getGenericMethodInvokeMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static MgmtgrpcServiceStub newStub(io.grpc.Channel channel) {
    return new MgmtgrpcServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static MgmtgrpcServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new MgmtgrpcServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static MgmtgrpcServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new MgmtgrpcServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class MgmtgrpcServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void createSapphireObject(sapphire.api.CreateRequest request,
        io.grpc.stub.StreamObserver<sapphire.api.CreateReply> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateSapphireObjectMethod(), responseObserver);
    }

    /**
     */
    public void deleteSapphireObject(sapphire.api.DeleteRequest request,
        io.grpc.stub.StreamObserver<sapphire.api.DeleteReply> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteSapphireObjectMethod(), responseObserver);
    }

    /**
     */
    public void genericMethodInvoke(sapphire.api.GenericMethodRequest request,
        io.grpc.stub.StreamObserver<sapphire.api.GenericMethodReply> responseObserver) {
      asyncUnimplementedUnaryCall(getGenericMethodInvokeMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getCreateSapphireObjectMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                sapphire.api.CreateRequest,
                sapphire.api.CreateReply>(
                  this, METHODID_CREATE_SAPPHIRE_OBJECT)))
          .addMethod(
            getDeleteSapphireObjectMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                sapphire.api.DeleteRequest,
                sapphire.api.DeleteReply>(
                  this, METHODID_DELETE_SAPPHIRE_OBJECT)))
          .addMethod(
            getGenericMethodInvokeMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                sapphire.api.GenericMethodRequest,
                sapphire.api.GenericMethodReply>(
                  this, METHODID_GENERIC_METHOD_INVOKE)))
          .build();
    }
  }

  /**
   */
  public static final class MgmtgrpcServiceStub extends io.grpc.stub.AbstractStub<MgmtgrpcServiceStub> {
    private MgmtgrpcServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private MgmtgrpcServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MgmtgrpcServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new MgmtgrpcServiceStub(channel, callOptions);
    }

    /**
     */
    public void createSapphireObject(sapphire.api.CreateRequest request,
        io.grpc.stub.StreamObserver<sapphire.api.CreateReply> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateSapphireObjectMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void deleteSapphireObject(sapphire.api.DeleteRequest request,
        io.grpc.stub.StreamObserver<sapphire.api.DeleteReply> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteSapphireObjectMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void genericMethodInvoke(sapphire.api.GenericMethodRequest request,
        io.grpc.stub.StreamObserver<sapphire.api.GenericMethodReply> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGenericMethodInvokeMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class MgmtgrpcServiceBlockingStub extends io.grpc.stub.AbstractStub<MgmtgrpcServiceBlockingStub> {
    private MgmtgrpcServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private MgmtgrpcServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MgmtgrpcServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new MgmtgrpcServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public sapphire.api.CreateReply createSapphireObject(sapphire.api.CreateRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateSapphireObjectMethod(), getCallOptions(), request);
    }

    /**
     */
    public sapphire.api.DeleteReply deleteSapphireObject(sapphire.api.DeleteRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteSapphireObjectMethod(), getCallOptions(), request);
    }

    /**
     */
    public sapphire.api.GenericMethodReply genericMethodInvoke(sapphire.api.GenericMethodRequest request) {
      return blockingUnaryCall(
          getChannel(), getGenericMethodInvokeMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class MgmtgrpcServiceFutureStub extends io.grpc.stub.AbstractStub<MgmtgrpcServiceFutureStub> {
    private MgmtgrpcServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private MgmtgrpcServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MgmtgrpcServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new MgmtgrpcServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<sapphire.api.CreateReply> createSapphireObject(
        sapphire.api.CreateRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateSapphireObjectMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<sapphire.api.DeleteReply> deleteSapphireObject(
        sapphire.api.DeleteRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteSapphireObjectMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<sapphire.api.GenericMethodReply> genericMethodInvoke(
        sapphire.api.GenericMethodRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGenericMethodInvokeMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_SAPPHIRE_OBJECT = 0;
  private static final int METHODID_DELETE_SAPPHIRE_OBJECT = 1;
  private static final int METHODID_GENERIC_METHOD_INVOKE = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final MgmtgrpcServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(MgmtgrpcServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_SAPPHIRE_OBJECT:
          serviceImpl.createSapphireObject((sapphire.api.CreateRequest) request,
              (io.grpc.stub.StreamObserver<sapphire.api.CreateReply>) responseObserver);
          break;
        case METHODID_DELETE_SAPPHIRE_OBJECT:
          serviceImpl.deleteSapphireObject((sapphire.api.DeleteRequest) request,
              (io.grpc.stub.StreamObserver<sapphire.api.DeleteReply>) responseObserver);
          break;
        case METHODID_GENERIC_METHOD_INVOKE:
          serviceImpl.genericMethodInvoke((sapphire.api.GenericMethodRequest) request,
              (io.grpc.stub.StreamObserver<sapphire.api.GenericMethodReply>) responseObserver);
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

  private static abstract class MgmtgrpcServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    MgmtgrpcServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return sapphire.api.SapphireProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("MgmtgrpcService");
    }
  }

  private static final class MgmtgrpcServiceFileDescriptorSupplier
      extends MgmtgrpcServiceBaseDescriptorSupplier {
    MgmtgrpcServiceFileDescriptorSupplier() {}
  }

  private static final class MgmtgrpcServiceMethodDescriptorSupplier
      extends MgmtgrpcServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    MgmtgrpcServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (MgmtgrpcServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new MgmtgrpcServiceFileDescriptorSupplier())
              .addMethod(getCreateSapphireObjectMethod())
              .addMethod(getDeleteSapphireObjectMethod())
              .addMethod(getGenericMethodInvokeMethod())
              .build();
        }
      }
    }
    return result;
  }
}

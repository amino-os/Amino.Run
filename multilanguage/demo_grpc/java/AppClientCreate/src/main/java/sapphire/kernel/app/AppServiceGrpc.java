package sapphire.kernel.app;

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
    comments = "Source: KernelServerApiToApp.proto")
public final class AppServiceGrpc {

  private AppServiceGrpc() {}

  public static final String SERVICE_NAME = "sapphire.kernel.app.AppService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateSapphireObjectMethod()} instead. 
  public static final io.grpc.MethodDescriptor<sapphire.kernel.app.CreateRequest,
      sapphire.kernel.app.CreateResponse> METHOD_CREATE_SAPPHIRE_OBJECT = getCreateSapphireObjectMethod();

  private static volatile io.grpc.MethodDescriptor<sapphire.kernel.app.CreateRequest,
      sapphire.kernel.app.CreateResponse> getCreateSapphireObjectMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<sapphire.kernel.app.CreateRequest,
      sapphire.kernel.app.CreateResponse> getCreateSapphireObjectMethod() {
    io.grpc.MethodDescriptor<sapphire.kernel.app.CreateRequest, sapphire.kernel.app.CreateResponse> getCreateSapphireObjectMethod;
    if ((getCreateSapphireObjectMethod = AppServiceGrpc.getCreateSapphireObjectMethod) == null) {
      synchronized (AppServiceGrpc.class) {
        if ((getCreateSapphireObjectMethod = AppServiceGrpc.getCreateSapphireObjectMethod) == null) {
          AppServiceGrpc.getCreateSapphireObjectMethod = getCreateSapphireObjectMethod = 
              io.grpc.MethodDescriptor.<sapphire.kernel.app.CreateRequest, sapphire.kernel.app.CreateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sapphire.kernel.app.AppService", "createSapphireObject"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.app.CreateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.app.CreateResponse.getDefaultInstance()))
                  .build();
          }
        }
     }
     return getCreateSapphireObjectMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getSetURLMethod()} instead. 
  public static final io.grpc.MethodDescriptor<sapphire.kernel.app.URLRequest,
      sapphire.kernel.app.URLResponse> METHOD_SET_URL = getSetURLMethod();

  private static volatile io.grpc.MethodDescriptor<sapphire.kernel.app.URLRequest,
      sapphire.kernel.app.URLResponse> getSetURLMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<sapphire.kernel.app.URLRequest,
      sapphire.kernel.app.URLResponse> getSetURLMethod() {
    io.grpc.MethodDescriptor<sapphire.kernel.app.URLRequest, sapphire.kernel.app.URLResponse> getSetURLMethod;
    if ((getSetURLMethod = AppServiceGrpc.getSetURLMethod) == null) {
      synchronized (AppServiceGrpc.class) {
        if ((getSetURLMethod = AppServiceGrpc.getSetURLMethod) == null) {
          AppServiceGrpc.getSetURLMethod = getSetURLMethod = 
              io.grpc.MethodDescriptor.<sapphire.kernel.app.URLRequest, sapphire.kernel.app.URLResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sapphire.kernel.app.AppService", "setURL"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.app.URLRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.app.URLResponse.getDefaultInstance()))
                  .build();
          }
        }
     }
     return getSetURLMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getDeleteSapphireObjectMethod()} instead. 
  public static final io.grpc.MethodDescriptor<sapphire.kernel.app.DeleteRequest,
      sapphire.kernel.app.DeleteResponse> METHOD_DELETE_SAPPHIRE_OBJECT = getDeleteSapphireObjectMethod();

  private static volatile io.grpc.MethodDescriptor<sapphire.kernel.app.DeleteRequest,
      sapphire.kernel.app.DeleteResponse> getDeleteSapphireObjectMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<sapphire.kernel.app.DeleteRequest,
      sapphire.kernel.app.DeleteResponse> getDeleteSapphireObjectMethod() {
    io.grpc.MethodDescriptor<sapphire.kernel.app.DeleteRequest, sapphire.kernel.app.DeleteResponse> getDeleteSapphireObjectMethod;
    if ((getDeleteSapphireObjectMethod = AppServiceGrpc.getDeleteSapphireObjectMethod) == null) {
      synchronized (AppServiceGrpc.class) {
        if ((getDeleteSapphireObjectMethod = AppServiceGrpc.getDeleteSapphireObjectMethod) == null) {
          AppServiceGrpc.getDeleteSapphireObjectMethod = getDeleteSapphireObjectMethod = 
              io.grpc.MethodDescriptor.<sapphire.kernel.app.DeleteRequest, sapphire.kernel.app.DeleteResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sapphire.kernel.app.AppService", "deleteSapphireObject"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.app.DeleteRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.app.DeleteResponse.getDefaultInstance()))
                  .build();
          }
        }
     }
     return getDeleteSapphireObjectMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getAcquireAppStubMethod()} instead. 
  public static final io.grpc.MethodDescriptor<sapphire.kernel.app.AcquireRequest,
      sapphire.kernel.app.AcquireResponse> METHOD_ACQUIRE_APP_STUB = getAcquireAppStubMethod();

  private static volatile io.grpc.MethodDescriptor<sapphire.kernel.app.AcquireRequest,
      sapphire.kernel.app.AcquireResponse> getAcquireAppStubMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<sapphire.kernel.app.AcquireRequest,
      sapphire.kernel.app.AcquireResponse> getAcquireAppStubMethod() {
    io.grpc.MethodDescriptor<sapphire.kernel.app.AcquireRequest, sapphire.kernel.app.AcquireResponse> getAcquireAppStubMethod;
    if ((getAcquireAppStubMethod = AppServiceGrpc.getAcquireAppStubMethod) == null) {
      synchronized (AppServiceGrpc.class) {
        if ((getAcquireAppStubMethod = AppServiceGrpc.getAcquireAppStubMethod) == null) {
          AppServiceGrpc.getAcquireAppStubMethod = getAcquireAppStubMethod = 
              io.grpc.MethodDescriptor.<sapphire.kernel.app.AcquireRequest, sapphire.kernel.app.AcquireResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sapphire.kernel.app.AppService", "acquireAppStub"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.app.AcquireRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.app.AcquireResponse.getDefaultInstance()))
                  .build();
          }
        }
     }
     return getAcquireAppStubMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getReleaseAppStubMethod()} instead. 
  public static final io.grpc.MethodDescriptor<sapphire.kernel.app.ReleaseRequest,
      sapphire.kernel.app.ReleaseResponse> METHOD_RELEASE_APP_STUB = getReleaseAppStubMethod();

  private static volatile io.grpc.MethodDescriptor<sapphire.kernel.app.ReleaseRequest,
      sapphire.kernel.app.ReleaseResponse> getReleaseAppStubMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<sapphire.kernel.app.ReleaseRequest,
      sapphire.kernel.app.ReleaseResponse> getReleaseAppStubMethod() {
    io.grpc.MethodDescriptor<sapphire.kernel.app.ReleaseRequest, sapphire.kernel.app.ReleaseResponse> getReleaseAppStubMethod;
    if ((getReleaseAppStubMethod = AppServiceGrpc.getReleaseAppStubMethod) == null) {
      synchronized (AppServiceGrpc.class) {
        if ((getReleaseAppStubMethod = AppServiceGrpc.getReleaseAppStubMethod) == null) {
          AppServiceGrpc.getReleaseAppStubMethod = getReleaseAppStubMethod = 
              io.grpc.MethodDescriptor.<sapphire.kernel.app.ReleaseRequest, sapphire.kernel.app.ReleaseResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sapphire.kernel.app.AppService", "releaseAppStub"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.app.ReleaseRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.app.ReleaseResponse.getDefaultInstance()))
                  .build();
          }
        }
     }
     return getReleaseAppStubMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getAttachMethod()} instead. 
  public static final io.grpc.MethodDescriptor<sapphire.kernel.app.AttachRequest,
      sapphire.kernel.app.AttachResponse> METHOD_ATTACH = getAttachMethod();

  private static volatile io.grpc.MethodDescriptor<sapphire.kernel.app.AttachRequest,
      sapphire.kernel.app.AttachResponse> getAttachMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<sapphire.kernel.app.AttachRequest,
      sapphire.kernel.app.AttachResponse> getAttachMethod() {
    io.grpc.MethodDescriptor<sapphire.kernel.app.AttachRequest, sapphire.kernel.app.AttachResponse> getAttachMethod;
    if ((getAttachMethod = AppServiceGrpc.getAttachMethod) == null) {
      synchronized (AppServiceGrpc.class) {
        if ((getAttachMethod = AppServiceGrpc.getAttachMethod) == null) {
          AppServiceGrpc.getAttachMethod = getAttachMethod = 
              io.grpc.MethodDescriptor.<sapphire.kernel.app.AttachRequest, sapphire.kernel.app.AttachResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sapphire.kernel.app.AppService", "attach"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.app.AttachRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.app.AttachResponse.getDefaultInstance()))
                  .build();
          }
        }
     }
     return getAttachMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getDetachMethod()} instead. 
  public static final io.grpc.MethodDescriptor<sapphire.kernel.app.DetachRequest,
      sapphire.kernel.app.DetachResponse> METHOD_DETACH = getDetachMethod();

  private static volatile io.grpc.MethodDescriptor<sapphire.kernel.app.DetachRequest,
      sapphire.kernel.app.DetachResponse> getDetachMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<sapphire.kernel.app.DetachRequest,
      sapphire.kernel.app.DetachResponse> getDetachMethod() {
    io.grpc.MethodDescriptor<sapphire.kernel.app.DetachRequest, sapphire.kernel.app.DetachResponse> getDetachMethod;
    if ((getDetachMethod = AppServiceGrpc.getDetachMethod) == null) {
      synchronized (AppServiceGrpc.class) {
        if ((getDetachMethod = AppServiceGrpc.getDetachMethod) == null) {
          AppServiceGrpc.getDetachMethod = getDetachMethod = 
              io.grpc.MethodDescriptor.<sapphire.kernel.app.DetachRequest, sapphire.kernel.app.DetachResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sapphire.kernel.app.AppService", "detach"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.app.DetachRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.app.DetachResponse.getDefaultInstance()))
                  .build();
          }
        }
     }
     return getDetachMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGenericInvokeMethod()} instead. 
  public static final io.grpc.MethodDescriptor<sapphire.kernel.app.InvokeRequest,
      sapphire.kernel.app.InvokeResponse> METHOD_GENERIC_INVOKE = getGenericInvokeMethod();

  private static volatile io.grpc.MethodDescriptor<sapphire.kernel.app.InvokeRequest,
      sapphire.kernel.app.InvokeResponse> getGenericInvokeMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<sapphire.kernel.app.InvokeRequest,
      sapphire.kernel.app.InvokeResponse> getGenericInvokeMethod() {
    io.grpc.MethodDescriptor<sapphire.kernel.app.InvokeRequest, sapphire.kernel.app.InvokeResponse> getGenericInvokeMethod;
    if ((getGenericInvokeMethod = AppServiceGrpc.getGenericInvokeMethod) == null) {
      synchronized (AppServiceGrpc.class) {
        if ((getGenericInvokeMethod = AppServiceGrpc.getGenericInvokeMethod) == null) {
          AppServiceGrpc.getGenericInvokeMethod = getGenericInvokeMethod = 
              io.grpc.MethodDescriptor.<sapphire.kernel.app.InvokeRequest, sapphire.kernel.app.InvokeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sapphire.kernel.app.AppService", "genericInvoke"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.app.InvokeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  sapphire.kernel.app.InvokeResponse.getDefaultInstance()))
                  .build();
          }
        }
     }
     return getGenericInvokeMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AppServiceStub newStub(io.grpc.Channel channel) {
    return new AppServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AppServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new AppServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AppServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new AppServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class AppServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void createSapphireObject(sapphire.kernel.app.CreateRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.app.CreateResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateSapphireObjectMethod(), responseObserver);
    }

    /**
     */
    public void setURL(sapphire.kernel.app.URLRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.app.URLResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSetURLMethod(), responseObserver);
    }

    /**
     */
    public void deleteSapphireObject(sapphire.kernel.app.DeleteRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.app.DeleteResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteSapphireObjectMethod(), responseObserver);
    }

    /**
     */
    public void acquireAppStub(sapphire.kernel.app.AcquireRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.app.AcquireResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAcquireAppStubMethod(), responseObserver);
    }

    /**
     */
    public void releaseAppStub(sapphire.kernel.app.ReleaseRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.app.ReleaseResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getReleaseAppStubMethod(), responseObserver);
    }

    /**
     */
    public void attach(sapphire.kernel.app.AttachRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.app.AttachResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAttachMethod(), responseObserver);
    }

    /**
     */
    public void detach(sapphire.kernel.app.DetachRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.app.DetachResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDetachMethod(), responseObserver);
    }

    /**
     */
    public void genericInvoke(sapphire.kernel.app.InvokeRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.app.InvokeResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGenericInvokeMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getCreateSapphireObjectMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                sapphire.kernel.app.CreateRequest,
                sapphire.kernel.app.CreateResponse>(
                  this, METHODID_CREATE_SAPPHIRE_OBJECT)))
          .addMethod(
            getSetURLMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                sapphire.kernel.app.URLRequest,
                sapphire.kernel.app.URLResponse>(
                  this, METHODID_SET_URL)))
          .addMethod(
            getDeleteSapphireObjectMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                sapphire.kernel.app.DeleteRequest,
                sapphire.kernel.app.DeleteResponse>(
                  this, METHODID_DELETE_SAPPHIRE_OBJECT)))
          .addMethod(
            getAcquireAppStubMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                sapphire.kernel.app.AcquireRequest,
                sapphire.kernel.app.AcquireResponse>(
                  this, METHODID_ACQUIRE_APP_STUB)))
          .addMethod(
            getReleaseAppStubMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                sapphire.kernel.app.ReleaseRequest,
                sapphire.kernel.app.ReleaseResponse>(
                  this, METHODID_RELEASE_APP_STUB)))
          .addMethod(
            getAttachMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                sapphire.kernel.app.AttachRequest,
                sapphire.kernel.app.AttachResponse>(
                  this, METHODID_ATTACH)))
          .addMethod(
            getDetachMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                sapphire.kernel.app.DetachRequest,
                sapphire.kernel.app.DetachResponse>(
                  this, METHODID_DETACH)))
          .addMethod(
            getGenericInvokeMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                sapphire.kernel.app.InvokeRequest,
                sapphire.kernel.app.InvokeResponse>(
                  this, METHODID_GENERIC_INVOKE)))
          .build();
    }
  }

  /**
   */
  public static final class AppServiceStub extends io.grpc.stub.AbstractStub<AppServiceStub> {
    private AppServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private AppServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AppServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new AppServiceStub(channel, callOptions);
    }

    /**
     */
    public void createSapphireObject(sapphire.kernel.app.CreateRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.app.CreateResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateSapphireObjectMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void setURL(sapphire.kernel.app.URLRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.app.URLResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSetURLMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void deleteSapphireObject(sapphire.kernel.app.DeleteRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.app.DeleteResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteSapphireObjectMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void acquireAppStub(sapphire.kernel.app.AcquireRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.app.AcquireResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAcquireAppStubMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void releaseAppStub(sapphire.kernel.app.ReleaseRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.app.ReleaseResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getReleaseAppStubMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void attach(sapphire.kernel.app.AttachRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.app.AttachResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAttachMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void detach(sapphire.kernel.app.DetachRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.app.DetachResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDetachMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void genericInvoke(sapphire.kernel.app.InvokeRequest request,
        io.grpc.stub.StreamObserver<sapphire.kernel.app.InvokeResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGenericInvokeMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class AppServiceBlockingStub extends io.grpc.stub.AbstractStub<AppServiceBlockingStub> {
    private AppServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private AppServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AppServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new AppServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public sapphire.kernel.app.CreateResponse createSapphireObject(sapphire.kernel.app.CreateRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateSapphireObjectMethod(), getCallOptions(), request);
    }

    /**
     */
    public sapphire.kernel.app.URLResponse setURL(sapphire.kernel.app.URLRequest request) {
      return blockingUnaryCall(
          getChannel(), getSetURLMethod(), getCallOptions(), request);
    }

    /**
     */
    public sapphire.kernel.app.DeleteResponse deleteSapphireObject(sapphire.kernel.app.DeleteRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteSapphireObjectMethod(), getCallOptions(), request);
    }

    /**
     */
    public sapphire.kernel.app.AcquireResponse acquireAppStub(sapphire.kernel.app.AcquireRequest request) {
      return blockingUnaryCall(
          getChannel(), getAcquireAppStubMethod(), getCallOptions(), request);
    }

    /**
     */
    public sapphire.kernel.app.ReleaseResponse releaseAppStub(sapphire.kernel.app.ReleaseRequest request) {
      return blockingUnaryCall(
          getChannel(), getReleaseAppStubMethod(), getCallOptions(), request);
    }

    /**
     */
    public sapphire.kernel.app.AttachResponse attach(sapphire.kernel.app.AttachRequest request) {
      return blockingUnaryCall(
          getChannel(), getAttachMethod(), getCallOptions(), request);
    }

    /**
     */
    public sapphire.kernel.app.DetachResponse detach(sapphire.kernel.app.DetachRequest request) {
      return blockingUnaryCall(
          getChannel(), getDetachMethod(), getCallOptions(), request);
    }

    /**
     */
    public sapphire.kernel.app.InvokeResponse genericInvoke(sapphire.kernel.app.InvokeRequest request) {
      return blockingUnaryCall(
          getChannel(), getGenericInvokeMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class AppServiceFutureStub extends io.grpc.stub.AbstractStub<AppServiceFutureStub> {
    private AppServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private AppServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AppServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new AppServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<sapphire.kernel.app.CreateResponse> createSapphireObject(
        sapphire.kernel.app.CreateRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateSapphireObjectMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<sapphire.kernel.app.URLResponse> setURL(
        sapphire.kernel.app.URLRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSetURLMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<sapphire.kernel.app.DeleteResponse> deleteSapphireObject(
        sapphire.kernel.app.DeleteRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteSapphireObjectMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<sapphire.kernel.app.AcquireResponse> acquireAppStub(
        sapphire.kernel.app.AcquireRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAcquireAppStubMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<sapphire.kernel.app.ReleaseResponse> releaseAppStub(
        sapphire.kernel.app.ReleaseRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getReleaseAppStubMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<sapphire.kernel.app.AttachResponse> attach(
        sapphire.kernel.app.AttachRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAttachMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<sapphire.kernel.app.DetachResponse> detach(
        sapphire.kernel.app.DetachRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDetachMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<sapphire.kernel.app.InvokeResponse> genericInvoke(
        sapphire.kernel.app.InvokeRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGenericInvokeMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_SAPPHIRE_OBJECT = 0;
  private static final int METHODID_SET_URL = 1;
  private static final int METHODID_DELETE_SAPPHIRE_OBJECT = 2;
  private static final int METHODID_ACQUIRE_APP_STUB = 3;
  private static final int METHODID_RELEASE_APP_STUB = 4;
  private static final int METHODID_ATTACH = 5;
  private static final int METHODID_DETACH = 6;
  private static final int METHODID_GENERIC_INVOKE = 7;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AppServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(AppServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_SAPPHIRE_OBJECT:
          serviceImpl.createSapphireObject((sapphire.kernel.app.CreateRequest) request,
              (io.grpc.stub.StreamObserver<sapphire.kernel.app.CreateResponse>) responseObserver);
          break;
        case METHODID_SET_URL:
          serviceImpl.setURL((sapphire.kernel.app.URLRequest) request,
              (io.grpc.stub.StreamObserver<sapphire.kernel.app.URLResponse>) responseObserver);
          break;
        case METHODID_DELETE_SAPPHIRE_OBJECT:
          serviceImpl.deleteSapphireObject((sapphire.kernel.app.DeleteRequest) request,
              (io.grpc.stub.StreamObserver<sapphire.kernel.app.DeleteResponse>) responseObserver);
          break;
        case METHODID_ACQUIRE_APP_STUB:
          serviceImpl.acquireAppStub((sapphire.kernel.app.AcquireRequest) request,
              (io.grpc.stub.StreamObserver<sapphire.kernel.app.AcquireResponse>) responseObserver);
          break;
        case METHODID_RELEASE_APP_STUB:
          serviceImpl.releaseAppStub((sapphire.kernel.app.ReleaseRequest) request,
              (io.grpc.stub.StreamObserver<sapphire.kernel.app.ReleaseResponse>) responseObserver);
          break;
        case METHODID_ATTACH:
          serviceImpl.attach((sapphire.kernel.app.AttachRequest) request,
              (io.grpc.stub.StreamObserver<sapphire.kernel.app.AttachResponse>) responseObserver);
          break;
        case METHODID_DETACH:
          serviceImpl.detach((sapphire.kernel.app.DetachRequest) request,
              (io.grpc.stub.StreamObserver<sapphire.kernel.app.DetachResponse>) responseObserver);
          break;
        case METHODID_GENERIC_INVOKE:
          serviceImpl.genericInvoke((sapphire.kernel.app.InvokeRequest) request,
              (io.grpc.stub.StreamObserver<sapphire.kernel.app.InvokeResponse>) responseObserver);
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
      synchronized (AppServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .addMethod(getCreateSapphireObjectMethod())
              .addMethod(getSetURLMethod())
              .addMethod(getDeleteSapphireObjectMethod())
              .addMethod(getAcquireAppStubMethod())
              .addMethod(getReleaseAppStubMethod())
              .addMethod(getAttachMethod())
              .addMethod(getDetachMethod())
              .addMethod(getGenericInvokeMethod())
              .build();
        }
      }
    }
    return result;
  }
}

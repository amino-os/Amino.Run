package sapphire.appexamples.hankstodo.app;

import com.google.protobuf.ByteString;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import sapphire.kernel.app.AppServiceGrpc;
import sapphire.kernel.app.*;

/**
 * Created by Venugopal Reddy K on 25/7/18.
 */

public class AppGrpcClient {
	private static final Logger logger = Logger.getLogger(AppGrpcClient.class.getName());
	private final ManagedChannel channel;
	private final AppServiceGrpc.AppServiceBlockingStub blockingStub;
	private final InetSocketAddress kernelClientAddr;

	public AppGrpcClient(String rmiServerHost, int rmiPort, String grpcServerHost, int grpcServerPort) {
		ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(grpcServerHost, grpcServerPort).usePlaintext(true);
		channel = channelBuilder.build();
        blockingStub = AppServiceGrpc.newBlockingStub(channel);
		kernelClientAddr = new InetSocketAddress(rmiServerHost, rmiPort);
	}

	public void shutdown() throws InterruptedException {
		if (null != channel) {
			channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
		}
	}

	public InetSocketAddress getKernelClientAddr() {
		return kernelClientAddr;
	}

	public static class SapphireClientInfo {
		private byte[] opaqueObject;
		private String clientId;
		private String sapphireId;

		public SapphireClientInfo(String clientId, String sapphireId, byte[] opaqueObject) {
			this.clientId = clientId;
			this.sapphireId = sapphireId;
			this.opaqueObject = opaqueObject;
		}

		public String getClientId() {
			return clientId;
		}

		public String getSapphireId() {
			return sapphireId;
		}

		public byte [] getOpaqueObject() {
			return opaqueObject;
		}
	}

	public String createSapphireObject(String className, String runtime, String initializerMethod, byte [] args) {
		try {
			CreateRequest.Builder builder = CreateRequest.newBuilder().setSoName(className).setLangType(runtime).setConstructName(initializerMethod);

			if(null != args) {
				builder.setConstructParams(ByteString.copyFrom(args));
			}

			CreateRequest request = builder.build();
			CreateResponse response = blockingStub.createSapphireObject(request);
			return response.getSId();
		} catch (Exception e) {
			// TODO: Need to handle the exceptions part
			e.printStackTrace();
		}

		return null;
	}

	public void deleteSapphireObject(String sid) {
		try {
			DeleteRequest request = DeleteRequest.newBuilder().setSId(sid).build();
			DeleteResponse response = blockingStub.deleteSapphireObject(request);
		} catch (Exception e) {
			// TODO: Need to handle the exceptions part
			e.printStackTrace();
		}
	}

	public SapphireClientInfo attach(String url) {
		try {
			AttachRequest request = AttachRequest.newBuilder().setUrl(url).setDmClientRmiEndPoint(getKernelClientAddr().getHostName() + ":" + getKernelClientAddr().getPort()).build();
			AttachResponse response = blockingStub.attach(request);
			return new SapphireClientInfo(response.getClientId(), response.getSId(), response.getObjectStream().toByteArray());
		} catch (Exception e) {
			// TODO: Need to handle the exceptions part
			e.printStackTrace();
		}

		return null;
	}

	public void detach(String sid, String clientId) {
		try {
			DetachRequest request = DetachRequest.newBuilder().setSId(sid).setClientId(clientId).build();
			DetachResponse response = blockingStub.detach(request);
		} catch (Exception e) {
			// TODO: Need to handle the exceptions part
			e.printStackTrace();
		}
	}

	public void setURL(String sid, String url) {
		try {
			URLRequest request = URLRequest.newBuilder().setSId(sid).setUrl(url).build();
			URLResponse response = blockingStub.setURL(request);
		} catch (Exception e) {
			// TODO: Need to handle the exceptions part
			e.printStackTrace();
		}
	}

	public SapphireClientInfo acquireSapphireObjRef(String sid) {
		try {
			AcquireRequest request = AcquireRequest.newBuilder().setSId(sid).setDmClientRmiEndPoint(getKernelClientAddr().getHostName() + ":" + getKernelClientAddr().getPort()).build();
			AcquireResponse response = blockingStub.acquireAppStub(request);
			return new SapphireClientInfo(response.getClientId(), sid, response.getObjectStream().toByteArray());
		} catch (Exception e) {
			// TODO: Need to handle the exceptions part
			e.printStackTrace();
		}

		return null;
	}

	public void releaseSapphireobjRef(String sid, String clientId) {
		try {
			ReleaseRequest request = ReleaseRequest.newBuilder().setSId(sid).setClientId(clientId).build();
			ReleaseResponse response = blockingStub.releaseAppStub(request);
		} catch (Exception e) {
			// TODO: Need to handle the exceptions part
			e.printStackTrace();
		}
	}

	public byte [] genericInvoke(String clientId, String method, ByteString stream) {
		try {
			InvokeRequest request = InvokeRequest.newBuilder().setDMClientId(clientId).setFuncName(method).setFuncParams(stream).build();
			InvokeResponse response = blockingStub.genericInvoke(request);
			return response.getObjectStream().toByteArray();
		} catch (Exception e) {
			// TODO: Need to handle the exceptions part
			e.printStackTrace();
		}

		return null;
	}
}

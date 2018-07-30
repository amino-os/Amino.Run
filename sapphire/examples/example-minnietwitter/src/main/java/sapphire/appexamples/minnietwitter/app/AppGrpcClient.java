package sapphire.appexamples.minnietwitter.app;

import com.google.protobuf.ByteString;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import sapphire.kernel.KernelServerApiToApp.GenericInvokeServiceGrpc;
import sapphire.kernel.KernelServerApiToApp.KernelServerApiToApp;
import sapphire.oms.OmsApiToApp.OMSServiceGrpc;
import sapphire.oms.OmsApiToApp.OmsApiToApp;

/**
 * Created by Venugopal Reddy K on 25/7/18.
 */

public class AppGrpcClient {
	private static final Logger logger = Logger.getLogger(AppGrpcClient.class.getName());
	private final ManagedChannel omsChannel;
	private final ManagedChannel kernelClientChannel;
	private final OMSServiceGrpc.OMSServiceBlockingStub  omsBlockingStub;
	private final GenericInvokeServiceGrpc.GenericInvokeServiceBlockingStub  kernelBlockingStub;
	private final InetSocketAddress kernelClientAddr;

	public AppGrpcClient(String grpcOmsHost, int grpcOmsPort, String grpcKernelClientHost, int grpcKernelClientPort, String rmiServerHost, int rmiPort) {
		ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(grpcOmsHost, grpcOmsPort).usePlaintext(true);
		omsChannel = channelBuilder.build();
		omsBlockingStub = OMSServiceGrpc.newBlockingStub(omsChannel);
		channelBuilder = ManagedChannelBuilder.forAddress(grpcKernelClientHost, grpcKernelClientPort).usePlaintext(true);
		kernelClientChannel = channelBuilder.build();
		kernelBlockingStub = GenericInvokeServiceGrpc.newBlockingStub(kernelClientChannel);
		kernelClientAddr = new InetSocketAddress(rmiServerHost, rmiPort);
	}

	public void shutdown() throws InterruptedException {
		if (null != omsChannel) {
			omsChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
		}

		if (null != kernelClientChannel) {
			kernelClientChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
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

		public Object getOpaqueObject() throws IOException, ClassNotFoundException {
			return toObject(opaqueObject);
		}
	}

	public static final byte[] toBytes(Object... args) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(bos);
			for (Object param : args) {
				out.writeObject(param);
			}
			out.flush();
			return bos.toByteArray();
		} finally {
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					logger.log(Level.WARNING, "failed to close stream bos", e);
				}
			}

			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					logger.log(Level.WARNING, "failed to close stream bos", e);
				}
			}
		}
	}

	public static Object toObject(byte[] bytes) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(bis);
			Object object = in.readObject();
			return object;
		} finally {
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException e) {
					logger.log(Level.WARNING, "failed to close stream bis", e);
				}
			}
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					logger.log(Level.WARNING, "failed to close stream in", e);
				}
			}
		}
	}

	public String createSapphireObject(String className, String runtime, String initializerMethod, Object... args) {
		try {
			OmsApiToApp.CreateRequest request =
					OmsApiToApp.CreateRequest.newBuilder().setSoName(className).setConstructName(initializerMethod).setConstructParams(ByteString.copyFrom(toBytes(args))).build();
			OmsApiToApp.CreateResponse response = omsBlockingStub.createSapphireObject(request);
			return response.getSId();
		} catch (Exception e) {
			// TODO: Need to handle the exceptions part
			e.printStackTrace();
		}

		return null;
	}

	public void deleteSapphireObject(String sid) {
		try {
			OmsApiToApp.DeleteRequest request =
					OmsApiToApp.DeleteRequest.newBuilder().setSId(sid).build();
			OmsApiToApp.DeleteResponse response = omsBlockingStub.deleteSapphireObject(request);
		} catch (Exception e) {
			// TODO: Need to handle the exceptions part
			e.printStackTrace();
		}
	}

	public SapphireClientInfo attach(String url) {
		try {
			OmsApiToApp.AttachRequest request =
					OmsApiToApp.AttachRequest.newBuilder().setUrl(url).setDmClientRmiEndPoint(getKernelClientAddr().getHostName() + ":" + getKernelClientAddr().getPort()).build();
			OmsApiToApp.AttachResponse response = omsBlockingStub.attach(request);
			return new SapphireClientInfo(response.getClientId(), response.getSId(), response.getObjectStream().toByteArray());
		} catch (Exception e) {
			// TODO: Need to handle the exceptions part
			e.printStackTrace();
		}

		return null;
	}

	public void detach(String sid, String clientId) {
		try {
			OmsApiToApp.DetachRequest request = OmsApiToApp.DetachRequest.newBuilder().setSId(sid).setClientId(clientId).build();
			OmsApiToApp.DetachResponse response = omsBlockingStub.detach(request);
		} catch (Exception e) {
			// TODO: Need to handle the exceptions part
			e.printStackTrace();
		}
	}

	public void setURL(String sid, String url) {
		try {
			OmsApiToApp.URLRequest request =
					OmsApiToApp.URLRequest.newBuilder().setSId(sid).setUrl(url).build();
			OmsApiToApp.URLResponse response = omsBlockingStub.setURL(request);
		} catch (Exception e) {
			// TODO: Need to handle the exceptions part
			e.printStackTrace();
		}
	}

	public SapphireClientInfo acquireSapphireObjRef(String sid) {
		SapphireClientInfo clientInfo;
		try {
			OmsApiToApp.AcquireRequest request =
					OmsApiToApp.AcquireRequest.newBuilder().setSId(sid).setDmClientRmiEndPoint(getKernelClientAddr().getHostName() + ":" + getKernelClientAddr().getPort()).build();
			OmsApiToApp.AcquireResponse response = omsBlockingStub.acquireAppStub(request);
			return new SapphireClientInfo(response.getClientId(), null, response.getObjectStream().toByteArray());
		} catch (Exception e) {
			// TODO: Need to handle the exceptions part
			e.printStackTrace();
		}

		return null;
	}

	public void releaseSapphireobjRef(String sid, String clientId) {
		try {
			OmsApiToApp.ReleaseRequest request = OmsApiToApp.ReleaseRequest.newBuilder().setSId(sid).setClientId(clientId).build();
			OmsApiToApp.ReleaseResponse response = omsBlockingStub.releaseAppStub(request);
		} catch (Exception e) {
			// TODO: Need to handle the exceptions part
			e.printStackTrace();
		}
	}

	public byte [] genericInvoke(String clientId, String method, Object... param) {
		try {
			KernelServerApiToApp.InvokeRequest request = KernelServerApiToApp.InvokeRequest.newBuilder().setDMClientId(clientId).setFuncName(method).setFuncParams(ByteString.copyFrom(toBytes(param))).build();
			KernelServerApiToApp.InvokeResponse response = kernelBlockingStub.genericInvoke(request);
			return response.getObjectStream().toByteArray();
		} catch (Exception e) {
			// TODO: Need to handle the exceptions part
			e.printStackTrace();
		}

		return null;
	}
}

package sapphire.kernel.server;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireReplicaID;
import sapphire.kernel.common.DMConfigInfo;
import sapphire.kernel.common.SapphireObjectInfo;
import sapphire.runtime.kernel.*;
import sapphire.runtime.kernel.KernelServiceGrpc;

/** Created by Venugopal Reddy K 00900280 on 23/7/18. */
public class KernelGrpcClient {
    private static final Logger logger = Logger.getLogger(KernelGrpcClient.class.getName());
    private final ManagedChannel channel;
    private final KernelServiceGrpc.KernelServiceBlockingStub blockingStub;

    public KernelGrpcClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true));
    }

    public KernelGrpcClient(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        blockingStub = KernelServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        if (null != channel) {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    public SapphireObjectInfo createSapphireReplica(
            String className,
            SapphireReplicaID sapphireReplicaId,
            SapphireObjectID sapphireParentObjId,
            byte[] opaqueObject,
            String constructorName,
            byte[] args) {
        try {
            CreateSObjReplicaRequest.Builder builder =
                    CreateSObjReplicaRequest.newBuilder()
                            .setSObjName(className)
                            .setSObjId(String.valueOf(sapphireReplicaId.getOID().getID()))
                            .setSObjReplicaId(String.valueOf(sapphireReplicaId.getID()))
                            .setSObjReplicaObject(ByteString.copyFrom(opaqueObject))
                            .setSObjConstructorName(constructorName)
                            .setSObjConstructorParams(ByteString.copyFrom(args));

            String parentObjId;
            if (null != sapphireParentObjId) {
                parentObjId = String.valueOf(sapphireParentObjId.getID());
                builder.setSObjParentSObjId(parentObjId);
            }

            CreateSObjReplicaResponse response = blockingStub.createSObjReplica(builder.build());

            DMConfigInfo dmConfigInfo =
                    new DMConfigInfo(
                            response.getSObjDMInfo().getClientPolicy(),
                            response.getSObjDMInfo().getServerPolicy(),
                            response.getSObjDMInfo().getGroupPolicy());
            return new SapphireObjectInfo(
                    dmConfigInfo, response.getSObjReplicaObject().toByteArray());
        } catch (StatusRuntimeException e) {
            // TODO: Need to translate and throw exceptions.. should not return null. applies to all
            // the methods in this file
            e.printStackTrace();
        }

        return null;
    }

    public boolean deleteSapphireReplica(SapphireReplicaID sapphireReplicaId) {
        try {
            DeleteSObjReplicaRequest request =
                    DeleteSObjReplicaRequest.newBuilder()
                            .setSObjId(String.valueOf(sapphireReplicaId.getOID().getID()))
                            .setSObjReplicaId(String.valueOf(sapphireReplicaId.getID()))
                            .build();
            DeleteSObjReplicaResponse response = blockingStub.deleteSObjReplica(request);
            return response.getStatus();
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
        }

        return false;
    }

    public Object rpcInvoke(
            SapphireReplicaID sapphireReplicaId, String method, ArrayList<Object> params)
            throws IOException {
        try {
            ByteString inStream = (ByteString) params.get(0);
            SObjMethodInvokeRequest request =
                    SObjMethodInvokeRequest.newBuilder()
                            .setSObjId(String.valueOf(sapphireReplicaId.getOID().getID()))
                            .setSObjReplicaId(String.valueOf(sapphireReplicaId.getID()))
                            .setSObjMethodName(method)
                            .setSObjMethodParams(ByteString.copyFrom(inStream.toByteArray()))
                            .build();
            SObjMethodInvokeResponse response = blockingStub.sObjMethodInvoke(request);
            return response.getSObjMethodResponse();
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
        }

        return null;
    }
}

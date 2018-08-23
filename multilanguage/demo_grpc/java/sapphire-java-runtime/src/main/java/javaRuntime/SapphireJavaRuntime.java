/** Created by Jithu Thomas on 18/7/18. */
package javaRuntime;

import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import sapphire.runtime.kernel.KernelServiceGrpc;
import sapphire.runtime.kernel.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public class SapphireJavaRuntime {

    static Logger logger = Logger.getLogger(SapphireJavaRuntime.class.getName());

    private Server gRPCServer;

    /*public static final int RuntimeHostPzort =
            Integer.parseInt(System.getenv("SAPPHIRE_RUNTIME_PORT"));
    public static final String SapphireObjectsPath = System.getenv("SAPPHIRE_SO_PATH");*/
    public static String RuntimeHostIP;
    public static int RuntimeHostPort;
    public static String KernelServerHostIP;
    public static int KernelServerHostPort;
    public static String SapphireObjectsPath;

    // Map for storing UUID and SapphireObject Replica mapping.
    public static final ConcurrentMap<SObjReplicaId, SapphireObject> SObjReplicaIDMap =
            new ConcurrentHashMap<SObjReplicaId, SapphireObject>();

    private void start() throws IOException {
        gRPCServer =
                ServerBuilder.forPort(RuntimeHostPort)
                        .addService(new KernelServerRuntimeGrpcImpl())
                        .build()
                        .start();
        logger.info("Server started, listening on port: " + RuntimeHostPort);

        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread() {
                            @Override
                            public void run() {
                                logger.warning(
                                        "Shutting down gRPC server since JVM is shutting down.");
                                SapphireJavaRuntime.this.stop();
                                logger.info("Server shut down.");
                            }
                        });
    }

    private void stop() {
        if (gRPCServer != null) {
            gRPCServer.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (gRPCServer != null) {
            gRPCServer.awaitTermination();
        }
    }

    public static void addSapphireObjToMap(String sapphireId, String replicaId, String parentSapphireId, SapphireObject sapphireObj) {
        SObjReplicaId objReplicaId = new SObjReplicaId(sapphireId, replicaId, parentSapphireId);
        SObjReplicaIDMap.put(objReplicaId, sapphireObj);
    }

    public static void main(String[] args) throws Exception {

        // Take the arguments from command-line, by default.
        // Argument order is: [host ip] [host port] [kernel_server_host] [kernel_server_port]
        // [SapphireObject PATH]
        if (args.length < 4) {
            System.out.println("Incorrect number of arguments to the Sapphire JavaRuntime.!!!");
            System.out.println(
                    "[host ip] [host port] [kernel_server_host] [kernel_server_port]");
            return;
        }
        RuntimeHostIP = args[0];
        RuntimeHostPort = Integer.parseInt(args[1]);
        KernelServerHostIP = args[2];
        KernelServerHostPort = Integer.parseInt(args[3]);
        // SapphireObjectsPath = args[4];

        final SapphireJavaRuntime runtime = new SapphireJavaRuntime();
        SapphireSdk sdk = SapphireSdk.getInstance();
        sdk.create_gRPCClient(KernelServerHostIP, KernelServerHostPort);
        runtime.start();
        runtime.blockUntilShutdown();
    }

    class KernelServerRuntimeGrpcImpl extends KernelServiceGrpc.KernelServiceImplBase {

        // Function to handle CreateSObjReplica request from Sapphire Kernel Server.
        public void createSObjReplica(
                CreateSObjReplicaRequest req,
                StreamObserver<CreateSObjReplicaResponse> responseObserver) {

            logger.info("Creating SapphireObject: " + req.getSObjName());
            Class<?> sObjClass = null;
            String sObjName = req.getSObjName();
            String className = null;
            String simpleName;

            int index = req.getSObjName().lastIndexOf('.');
            if (index > 0) {
                className = req.getSObjName().substring(0, index + 1);
                simpleName = req.getSObjName().substring(index + 1);
                className = String.format("%sgrpcStubs.%s", className, simpleName);
            } else {
                className = req.getSObjName();
                simpleName = className;
            }

            try {
                className = String.format("%s_Stub", className);
                sObjClass = Class.forName(className);
            } catch (ClassNotFoundException e) {
                logger.severe("SapphireObject class not found. " + e.getMessage());
                return;
            }

            // Create an instance of the Sapphire Object class.
            String consMethodName = String.format("%s_construct", simpleName);
            Method construct = null;
            Object sObj = null;
            try {
                construct =
                        sObjClass.getDeclaredMethod(
                                consMethodName,
                                String.class,
                                byte[].class,
                                String.class,
                                String.class,
                                String.class,
                                Object.class);
                sObj =
                        construct.invoke(
                                null,
                                sObjName,
                                req.getSObjConstructorParams().toByteArray(),
                                req.getSObjParentSObjId(),
                                req.getSObjId(),
                                req.getSObjReplicaId(),
                                req.getSObjReplicaObject().toByteArray());
            } catch (IllegalAccessException e) {
                logger.severe(
                        "SObj instance creation raised Illegal accessException.!!!"
                                + e.getMessage());
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                logger.severe("SObj instance created failed.!!!" + e.getMessage());
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                logger.severe("SObj instance creation failed.!!!" + e.getMessage());
                e.printStackTrace();
            }

            if (sObj != null) {
                // Add UUID and the Sapphire Object instance to the SapphireIDMap.
                addSapphireObjToMap(req.getSObjId(), req.getSObjReplicaId(), null, (SapphireObject) sObj);

                logger.info(
                        "Successfully created SapphireObject replica with ID: "
                                + req.getSObjId()
                                + ":"
                                + req.getSObjReplicaId());
            }
            /*
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(sObj);
                oos.flush();
            } catch (IOException e) {
                logger.severe("SapphireObject replica serialization failed." + e.getMessage());
            }
            byte[] data = bos.toByteArray();
            */
            DMInfo dmInfo =
                    DMInfo.newBuilder()
                            .setClientPolicy("")
                            .setServerPolicy("")
                            .setGroupPolicy("")
                            .build();

            CreateSObjReplicaResponse reply =
                    CreateSObjReplicaResponse.newBuilder()
                            .setSObjReplicaObject(((SapphireObject)sObj).getObjectStream())
                            .setSObjDMInfo(dmInfo)
                            .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        // Function to handle DeleteSapphireObject request from application.
        public void deleteSObjReplica(
                DeleteSObjReplicaRequest req,
                StreamObserver<DeleteSObjReplicaResponse> responseObserver) {

            // Check whether the Sapphire Object instance has been created and available.
            SObjReplicaId replicaId =
                    new SObjReplicaId(req.getSObjId(), req.getSObjReplicaId(), null);
            logger.info(
                    "Deleting SapphireObject instance with ID: "
                            + replicaId.sObjReplicaId.sObjId
                            + ":"
                            + replicaId.sObjReplicaId.sObjReplicaId);

            Object sObj = SObjReplicaIDMap.get(replicaId);
            if (sObj == null) {
                logger.warning(
                        "SapphireObject instance not found for ID: "
                                + replicaId.sObjReplicaId.sObjId
                                + ":"
                                + replicaId.sObjReplicaId.sObjReplicaId);
                return;
            }

            boolean status = true;
            Object res = SObjReplicaIDMap.remove(replicaId);
            if (res == null) {
                status = false;
            }

            DeleteSObjReplicaResponse reply =
                    DeleteSObjReplicaResponse.newBuilder().setStatus(status).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        // Function to handle GenericMethodInvoke request from application.
        public void sObjMethodInvoke(
                SObjMethodInvokeRequest req,
                StreamObserver<SObjMethodInvokeResponse> responseObserver) {

            SObjReplicaId replicaId =
                    new SObjReplicaId(req.getSObjId(), req.getSObjReplicaId(), null);

            // Check whether the Sapphire Object instance has been created and available.
            SapphireObject sObj = SObjReplicaIDMap.get(replicaId);
            if (sObj == null) {
                logger.warning(
                        "SapphireObject instance not found for UUID: " + req.getSObjReplicaId());
                return;
            }

            Class sObjClass = sObj.getObjectStub().getClass();

            String sObjMethodName = String.format("%s", req.getSObjMethodName());
            logger.fine("Retrieve SapphireObject Function from the class: " + sObjMethodName);
            Method sObjMethod = null;

            try {
                sObjMethod = sObjClass.getMethod(sObjMethodName, new Class[] {byte[].class});
            } catch (NoSuchMethodException e) {

                logger.severe(
                        "SapphireObject Method not found: " + sObjMethodName + e.getMessage());
                return;
            }

            logger.info("Invoking SapphireObject Method: " + sObjMethodName);

            // Invoker the method
            byte[] response = null;
            try {
                response =
                        (byte[]) sObjMethod.invoke(sObj.getObjectStub(), req.getSObjMethodParams().toByteArray());
            } catch (IllegalAccessException e) {
                logger.severe(
                        "SapphireObject Function invocation raised IllegalAccessException.!!!"
                                + e.getMessage());
            } catch (InvocationTargetException e) {
                logger.severe(
                        "SapphireObject Function invocation raised InvocationTargetException.!!!"
                                + e.getMessage());
            }

            SObjMethodInvokeResponse reply =
                    SObjMethodInvokeResponse.newBuilder()
                            .setSObjMethodResponse(ByteString.copyFrom(response))
                            .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}

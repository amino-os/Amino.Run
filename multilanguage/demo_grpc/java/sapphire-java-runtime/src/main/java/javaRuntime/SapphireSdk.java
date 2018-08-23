/** Created by Jithu Thomas on 18/7/18. */
package javaRuntime;

import static javaRuntime.SapphireJavaRuntime.KernelServerHostIP;
import static javaRuntime.SapphireJavaRuntime.KernelServerHostPort;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import sapphire.kernel.runtime.RuntimeServiceGrpc;
import sapphire.kernel.runtime.*;

public class SapphireSdk {

    static Logger logger = Logger.getLogger(SapphireSdk.class.getName());
    static SapphireSdk sdk;
    /* public static final String SapphireObjectsPath = System.getenv("SAPPHIRE_SO_PATH");
    public static final String KernelServerHostIP = System.getenv("SAPPHIRE_KERNEL_SERVER_HOST");
    public static final int KernelServerHostPort =
            Integer.parseInt(System.getenv("SAPPHIRE_KERNEL_SERVER_PORT"));*/

    // gRPC client -->
    private ManagedChannel channel;
    private RuntimeServiceGrpc.RuntimeServiceBlockingStub blockingStub;

    public void create_gRPCClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build();
        blockingStub = RuntimeServiceGrpc.newBlockingStub(channel);
    }

    public static SapphireSdk getInstance() {
        if (sdk == null) {
            synchronized (SapphireSdk.class) {
                if (sdk == null) {
                    sdk = new SapphireSdk();
                }
            }
        }

        return sdk;
    }

    public void shutdown_gRPCClient() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    // <-- gRPC client

    public static SapphireObject new_stub(String parentSObjId, Class<?> appObjectClass, Object... args) {
        Class<?> sObjClass = null;
        String className = appObjectClass.getName();
        String stubClassName;
        String simpleName;
        ByteArrayOutputStream bos = null;
        byte[] bytes = null;
        try {
            bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(args);
            out.flush();
            bytes = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
                ex.printStackTrace();
            }
        }

        if (null == bytes) {
            return null;
        }

        logger.info("Creating SapphireObject: " + className);
        int index = className.lastIndexOf('.');
        if (index > 0) {
            stubClassName = className.substring(0, index + 1);
            simpleName = className.substring(index + 1);
            className = String.format("%sgrpcStubs.%s", stubClassName, simpleName);
        } else {
            simpleName = className;
        }

        try {
            className = String.format("%s_Stub", className);
            sObjClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            logger.severe("SapphireObject class not found. " + e.getMessage());
            return null;
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
            //TODO: Need to pass the arguments properly */
            sObj = construct.invoke(null, consMethodName, bytes, parentSObjId, null, null, null);
        } catch (IllegalAccessException e) {
            logger.severe(
                    "SObj instance creation raised Illegal accessException.!!!" + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (NoSuchMethodException e) {
            logger.severe("SObj instance created failed.!!!" + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (InvocationTargetException e) {
            logger.severe("SObj instance creation failed.!!!" + e.getMessage());
            e.printStackTrace();
            return null;
        }

        logger.info("Successfully created Child SapphireObject replica: " + className);

        /*ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(sObj);
            oos.flush();
        } catch (IOException e) {
            logger.severe("SapphireObject replica serialization failed." + e.getMessage());
        }
        byte[] data = bos.toByteArray();
        */

        CreateChildSObjRequest req =
                CreateChildSObjRequest.newBuilder()
                        .setSObjName(className+":java")
                        .setSObjParentSObjId(parentSObjId)
                        .setSObjDMInfo(
                                DMInfo.newBuilder()
                                        .setClientPolicy("")
                                        .setServerPolicy("")
                                        .setGroupPolicy("")
                                        .build())
                        .setSObjReplicaObject(((SapphireObject) sObj).getObjectStream())
                        .build();


        CreateChildSObjResponse response = getInstance().blockingStub.createChildSObj(req);

        String childSObjId = response.getSObjId();
        String childSObjReplicaId = response.getSObjReplicaId();

        // Check whether the Sapphire Object instance has been created and available.
        SapphireJavaRuntime.addSapphireObjToMap(childSObjId, childSObjReplicaId, parentSObjId, (SapphireObject) sObj);
        logger.info(
                "Successfully created child SapphireObject replica with ID: " + childSObjId + ":" + childSObjReplicaId);

        /*
        Class<?> sclass[] = sObjClass.getDeclaredClasses();
        for (int i = 0; i < sclass.length; i++) {
            if (sclass[i].getName() == "SObjReplica") {
                try {
                    Method meth =
                            sclass[i].getMethod("setSObjReplicaId", String.class, String.class);
                    meth.invoke(sObj, childSObjId, childSObjReplicaId);
                } catch (NoSuchMethodException e) {
                    logger.severe("Setting of sObjReplicaId failed.!!!" + e.getMessage());
                    e.printStackTrace();
                    return null;
                } catch (IllegalAccessException e) {
                    logger.severe("Setting of sObjReplicaId failed.!!!" + e.getMessage());
                    e.printStackTrace();
                    return null;
                } catch (InvocationTargetException e) {
                    logger.severe("Setting of sObjReplicaId failed.!!!" + e.getMessage());
                    e.printStackTrace();
                    return null;
                }
            }
        }

        try {
            shutdown_gRPCClient();
        } catch (InterruptedException e) {
            logger.severe("Shutdown of gRPC client failed.!!!" + e.getMessage());
            e.printStackTrace();
        } */

        return new SapphireObject(((SapphireObject) sObj).getObjectStub(), ((SapphireObject) sObj).getObjectStream(), childSObjId, childSObjReplicaId);
    }

    public void delete_stub(Object sObj) {

        Class<?> sObjClass = sObj.getClass();
        String className = sObj.getClass().getName();
        SObjReplicaId replicaId = new SObjReplicaId();

        logger.info("Deleting SapphireObject: " + className);

        Class<?> sclass[] = sObjClass.getDeclaredClasses();
        for (int i = 0; i < sclass.length; i++) {
            if (sclass[i].getName() == "SObjReplica") {
                try {
                    Method meth = sclass[i].getMethod("getSObjReplicaId");
                    replicaId = (SObjReplicaId) meth.invoke(sObj);
                } catch (NoSuchMethodException e) {
                    logger.severe("Retrieving of sObjReplicaId failed.!!!" + e.getMessage());
                    e.printStackTrace();
                    return;
                } catch (IllegalAccessException e) {
                    logger.severe("Retrieving of sObjReplicaId failed.!!!" + e.getMessage());
                    e.printStackTrace();
                    return;
                } catch (InvocationTargetException e) {
                    logger.severe("Retrieving of sObjReplicaId failed.!!!" + e.getMessage());
                    e.printStackTrace();
                    return;
                }
            }
        }

        DeleteChildSObjRequest req =
                DeleteChildSObjRequest.newBuilder()
                        .setSObjId(replicaId.sObjReplicaId.sObjId)
                        .build();

        create_gRPCClient(KernelServerHostIP, KernelServerHostPort);
        DeleteChildSObjResponse response = blockingStub.deleteChildSObj(req);

        try {
            shutdown_gRPCClient();
        } catch (InterruptedException e) {
            logger.severe("Shutdown of gRPC client failed.!!!" + e.getMessage());
            e.printStackTrace();
        }
    }
}

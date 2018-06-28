/**
 * Created by Srinivas on 21/6/18.
 */

package sapphire.sapphireProcess;

import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import sapphire.api.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SapphireProcess {

    private Server server;
    public static final int sapphire_process_port = 7000;
    public static final String SharedLibsPath = "/home/root1/Work/DCAP/Multi-Language/trials/June27/dcap_java/";

    // Map's needed for storing the Sapphire Object details.
    // 1). Map for storing SapphireObjName and the corrsponding Java Class mapping.
    public static final ConcurrentMap<String, Class> SapphireNameMap = new ConcurrentHashMap<String, Class>();
    // 2). Map for storing UUID and SapphireObject instance mapping.
    public static final ConcurrentMap<String, Object> SapphireIDMap = new ConcurrentHashMap<String, Object>();

    private void start() throws IOException {
        server = ServerBuilder.forPort(sapphire_process_port)
                .addService(new MgmtgrpcServiceImpl())
                .build()
                .start();
        System.out.println("Sapphire_process: Server started, listening on port: " + sapphire_process_port);
        System.out.println();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("Sapphire_process: shutting down gRPC server since JVM is shutting down.");
                SapphireProcess.this.stop();
                System.err.println("Sapphire_process: server shut down.");
            }
        });
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    // Function for generation UUID, needed for mapping Sapphire Objects
    public static String generateUUID() {
        String uid = UUID.randomUUID().toString();
        return uid;
    }

    public static void main(String[] args) throws Exception {

        System.out.println("Sapphire_process: Inside Sapphire Process main()");

        final SapphireProcess runtime = new SapphireProcess();
        runtime.start();
        runtime.blockUntilShutdown();
    }

    static class MgmtgrpcServiceImpl extends MgmtgrpcServiceGrpc.MgmtgrpcServiceImplBase {

        // Function to handle CreateSapphireObject request from application.
        public void createSapphireObject(CreateRequest req, StreamObserver<CreateReply> responseObserver) {

            System.out.println("Inside createSapphireObject(), SObj_name: " + req.getName());

            URLClassLoader urlClassLoader = null;
            Class <?> sObjClass = null;
            String className = null;

            sObjClass = SapphireNameMap.get(req.getName());
            if (sObjClass == null) {
                System.out.println("Sapphire Object class not found in Map. SObj_name: " + req.getName());

                String sharedClassPath = String.format("file://%ssapphireObj_%s.jar", SharedLibsPath, req.getName());

                    try {
                        URL[] classLoaderUrls = new URL[] { new URL(sharedClassPath)};
                        urlClassLoader = new URLClassLoader(classLoaderUrls);

                        className = String.format("sapphire.userApp.sapphireObject.%s.%sStub", req.getName(), req.getName());
                        sObjClass = urlClassLoader.loadClass(className);

                        // Add the Sapphire Object class to the SapphireNameMap
                        SapphireNameMap.put(req.getName(), sObjClass);

                    } catch (ClassNotFoundException e) {
                        System.out.println("Sapphire Object class not found. Class: " + className);
                        System.out.println(e.getMessage());
                        return;
                    } catch (MalformedURLException e) {
                        System.out.println("Malformed Sapphire Object Class path detected. Classpath: " + sharedClassPath);
                        System.out.println(e.getMessage());
                        return;
                    }
            } else {
                System.out.println("Sapphire Object class found in SapphireName Map. Class: " + sObjClass);
            }

            // Create an instance of the Sapphire Object class.
            Object sObj = null;
            try {
                sObj = sObjClass.newInstance();
            } catch (InstantiationException e) {
                System.out.println("Sapphire Object instance creation failed.!!!");
                System.out.println(e.getMessage());
                return;
            } catch (IllegalAccessException e) {
                System.out.println("Sapphire Object instance creation raised Illegal accessException.!!!");
                System.out.println(e.getMessage());
                return;
            }

            // Generate a UUID for the created Sapphire Object instance.
            String sObjUid = generateUUID();
            // Add UUID and the Sapphire Object instance to the SapphireIDMap.
            SapphireIDMap.put(sObjUid, sObj);

            System.out.println("Successfully created Sapphire Object instance with UUID: " + sObjUid);

            CreateReply reply = CreateReply.newBuilder()
                    .setObjId(sObjUid).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        // Function to handle DeleteSapphireObject request from application.
        public void deleteSapphireObject(DeleteRequest req, StreamObserver<DeleteReply> responseObserver) {

            // Check whether the Sapphire Object instance has been created and available.
            System.out.println("Deleting Sapphire Object instance with UUID: " + req.getObjId());

            Object sObj = SapphireIDMap.get(req.getObjId());
            if (sObj == null) {
                System.out.println("Sapphire Object instance not found for UUID: " + req.getObjId());
                return;
            }

            boolean status = true;
            Object res = SapphireIDMap.remove(req.getObjId());
            if (res == null) {
                status = false;
            }

            DeleteReply reply = DeleteReply.newBuilder()
                    .setFlag(status).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();

            System.out.println();
        }

        // Function to handle GenericMethodInvoke request from application.
        public void genericMethodInvoke(GenericMethodRequest req, StreamObserver<GenericMethodReply> responseObserver) {

            // Check whether the Sapphire Object class has been loaded.
            Class sObjClass = SapphireNameMap.get(req.getSapphireObjName());
            if (sObjClass == null) {
                System.out.println("Sapphire Object class not loaded, for Sapphire Object: " + req.getSapphireObjName());
                return;
            }

            // Check whether the Sapphire Object instance has been created and available.
            Object sObj = SapphireIDMap.get(req.getObjId());
            if (sObj == null) {
                System.out.println("Sapphire Object instance not found for UUID: " + req.getObjId());
                return;
            }

            String sObjFuncName = String.format("%s_Wrap", req.getFuncName());
            System.out.println("Retrieve the Sapphire Object Function from the class: " + sObjFuncName);
            Method sObjFunc = null;
            try {
                sObjFunc = sObjClass.getMethod(sObjFuncName, new Class[] {byte[].class});
            } catch (NoSuchMethodException e) {

                System.out.println("Sapphire Object Method not found. !!!" + sObjFuncName);
                System.out.println(e.getMessage());
                return;
            }

            System.out.println("Invoking Sapphire Object Function: " + sObjFuncName);

            // Invoker the method
            byte[] response = null;
            try {
                response = (byte[])sObjFunc.invoke(sObj, req.getParams().toByteArray());
            } catch (IllegalAccessException e) {
                System.out.println("Sapphire Object Function invocation raised IllegalAccessException.!!!");
                System.out.println(e.getMessage());
            } catch (InvocationTargetException e) {
                System.out.println("Sapphire Object Function invocation raised InvocationTargetException.!!!");
                System.out.println(e.getMessage());
            }

            GenericMethodReply reply = GenericMethodReply.newBuilder()
                    .setRet(ByteString.copyFrom(response)).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}

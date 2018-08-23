/**
 * Created by Jithu Thomas on 18/7/18.
 */

package hankstodo.app.grpcStubs;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import hankstodo.app.TodoList;
import hankstodo.app.TodoListManager;
import hankstodo.app.TodoListManagerOuterClass;
import hankstodo.app.TodoListOuterClass;
import javaRuntime.SapphireObject;
import javaRuntime.SapphireSdk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.logging.Logger;

public final class TodoListManager_Stub extends TodoListManager {
    private String parentSobjId;
    private String sobjId;

    static Logger logger = Logger.getLogger(TodoListManager_Stub.class.getName());

    public static SapphireObject TodoListManager_construct(String sObjConstructorName, byte[] sObjConstructorParams,
                               String parentSObjId, String sObjId, String replicaId,
                               Object sObjReplicaObject) {
        Class<?> cl = TodoListManager_Stub.class;
        TodoListManager_Stub sObjReplica = null;
        if (0 == sObjConstructorParams.length) {
            try {
                sObjReplica = (TodoListManager_Stub)cl.newInstance();
                sObjReplica.sobjId = sObjId;
                sObjReplica.parentSobjId = parentSObjId;
            } catch (InstantiationException e) {
                logger.severe("SObj instance creation failed.!!!" + e.getMessage());
                e.printStackTrace();
                return null;
            } catch (IllegalAccessException e) {
                logger.severe(
                        "SObj instance creation raised Illegal accessException.!!!"
                                + e.getMessage());
                e.printStackTrace();
                return null;
            }
        } else {
            /*ByteArrayInputStream bis = new ByteArrayInputStream(sObjConstructorParams);
            Object inparams = null;
            try {
                inparams = (new ObjectInputStream(bis)).readObject();
            } catch (IOException e) {
                logger.severe("SObj Constructor params deserialization failed.!!!" + e.getMessage());
                e.printStackTrace();
                return null;
            } catch (ClassNotFoundException e) {
                logger.severe("SObj Constructor params deserialization failed.!!!" + e.getMessage());
                e.printStackTrace();
                return null;
            } */
        }

        return new SapphireObject(sObjReplica, ByteString.copyFrom(TodoListManagerOuterClass.TodoListManager.newBuilder().setSid(sObjId).build().toByteArray()), sObjId, replicaId);
    }

    public TodoListManager_Stub() {
        super();
    }

    //  Server side rpc call receiving method
    public byte[] newTodoList(byte[] params) throws InvalidProtocolBufferException {
        TodoListManagerOuterClass.newTodo input = TodoListManagerOuterClass.newTodo.parseFrom(params);
        TodoList result = super.newTodoList(input.getArg0());
        /* TODO: Detect if this obj should be of inner SO. Create inner SO and get sid and rid from kernel server */
        SapphireObject sObj = SapphireSdk.new_stub(sobjId, result.getClass(), result.getName());
        //serialize TodoList it and send as byte array
        TodoListOuterClass.TodoList serializeResult =  TodoListOuterClass.TodoList.newBuilder().setSid(sObj.getSid()).setName(result.getName()).build();
        byte [] x = serializeResult.toByteArray();
        return x;
    }
}

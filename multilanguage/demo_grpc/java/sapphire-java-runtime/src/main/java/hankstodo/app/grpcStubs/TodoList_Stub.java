package hankstodo.app.grpcStubs;


import com.google.protobuf.ByteString;
import hankstodo.app.TodoList;
import hankstodo.app.TodoListOuterClass;
import javaRuntime.SapphireObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: root1
 * Date: 6/8/18
 * Time: 3:27 PM
 * To change this template use File | Settings | File Templates.
 */
public final class TodoList_Stub extends TodoList {
    private String parentSobjId;
    private String sobjId;

    static Logger logger = Logger.getLogger(TodoList_Stub.class.getName());

    public TodoList_Stub(String name) {
        super(name);
    }

    public static SapphireObject TodoList_construct(String sObjConstructorName, byte[] sObjConstructorParams,
                                                           String parentSObjId, String sObjId, String replicaId,
                                                           Object sObjReplicaObject) {
        Class<?> cl = TodoList_Stub.class;
        TodoList_Stub sObjReplica = null;
        if (0 == sObjConstructorParams.length) {
            try {
                sObjReplica = (TodoList_Stub)cl.newInstance();
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
            /* TODO: This is only for the inner SOs. When the object is created from parent SO, it uses java conventional serilization */
            ByteArrayInputStream in = new ByteArrayInputStream(sObjConstructorParams);
            Object [] param = null;
            //TODO: Need to read all the params
            try {
                ObjectInputStream is = new ObjectInputStream(in);
                param = (Object[])is.readObject();
                sObjReplica = (TodoList_Stub)cl.getConstructor(String.class).newInstance(param[0]);
            } catch (IOException e) {
                logger.severe("SObj Constructor params deserialization failed.!!!" + e.getMessage());
                e.printStackTrace();
                return null;
            } catch (ClassNotFoundException e) {
                logger.severe("SObj Constructor params deserialization failed.!!!" + e.getMessage());
                e.printStackTrace();
                return null;
            } catch (InvocationTargetException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (NoSuchMethodException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (InstantiationException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        return new SapphireObject(sObjReplica, ByteString.copyFrom(TodoListOuterClass.TodoList.newBuilder().build().toByteArray()), sObjId, replicaId);
    }

    public byte[] addToDo(byte[] params) throws Exception {
        TodoListOuterClass.inAddToDo input = TodoListOuterClass.inAddToDo.parseFrom(params);
        String result = super.addToDo(input.getArg0());
        /* TODO: Check if the method return type is primitive or string or autoboxed, then set the return value as is and serialize */
        //serialize TodoList it and send as byte array
        TodoListOuterClass.outAddToDo serializeResult =  TodoListOuterClass.outAddToDo.newBuilder().setArg0(result).build();
        byte [] x = serializeResult.toByteArray();
        return x;
    }
}

package hankstodo.app.grpcStubs;

import com.google.protobuf.ByteString;
import hankstodo.app.GlobalGrpcClientRef;
import hankstodo.app.TodoListOuterClass;


/**
 * Created by root1 on 25/7/18.
 */

public final class TodoList_Stub {
	private String sapphireObjId;
	private String clientId;
	private TodoListOuterClass.TodoList object;

	private String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	private String getSapphireObjId() {
		return sapphireObjId;
	}

	public void setSapphireObjId(String sapphireObjId) {
		this.sapphireObjId = sapphireObjId;
	}

	public TodoListOuterClass.TodoList getObject() {
		return object;
	}

	public void setObject(TodoListOuterClass.TodoList object) {
		this.object = object;
	}

	public String addToDo(String todo) throws Exception {
        String method = "AddToDo";
        if (null == sapphireObjId) {
            /* Local invocation if case of stub instance is not for a sapphire object */
            //TODO: Need to check on how to update the local object */
            return "OK!";
        }

        byte[] inStream = TodoListOuterClass.inAddToDo.newBuilder().setArg0(todo).build().toByteArray();
        byte[] outstream = GlobalGrpcClientRef.grpcClient.genericInvoke(getClientId(), method, ByteString.copyFrom(inStream));
        TodoListOuterClass.outAddToDo result = TodoListOuterClass.outAddToDo.parseFrom(outstream);
        /* TODO: Check if the method return type is primitive or string or autoboxed, then return them as is to app */
        String stub = new String(result.getArg0());
        return stub;
	}
}
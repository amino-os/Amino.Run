package hankstodo.app.grpcStubs;


import com.google.protobuf.ByteString;
import hankstodo.app.AppGrpcClient;
import hankstodo.app.GlobalGrpcClientRef;

import hankstodo.app.TodoListManagerOuterClass;
import hankstodo.app.TodoListOuterClass;

/**
 * Created by root1 on 25/7/18.
 */

public final class TodoListManager_Stub {
	private String sapphireObjId;
	private String clientId;
	private TodoListManagerOuterClass.TodoListManager object;

	private String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

    public void setSapphireObjId(String sapphireObjId) {
        this.sapphireObjId = sapphireObjId;
    }

	public TodoListManagerOuterClass.TodoListManager getObject() {
		return object;
	}

	public void setObject(TodoListManagerOuterClass.TodoListManager object) {
		this.object = object;
	}

	// user create instance of this obj
	public TodoListManager_Stub () throws Exception {

        /* Get client and app stub */
		AppGrpcClient.SapphireClientInfo sapphireClientInfo = GlobalGrpcClientRef.grpcClient.attach("Hankstodo.GoRuntime");
		sapphireObjId= sapphireClientInfo.getSapphireId();
		clientId = sapphireClientInfo.getClientId();
		object = TodoListManagerOuterClass.TodoListManager.parseFrom(sapphireClientInfo.getOpaqueObject());
		System.out.println("Attach success full sapphire id of "+sapphireObjId);
	}

	//rpc call
	public TodoList_Stub newTodoList(String name) throws Exception {
		String method = "NewTodoList";
        byte [] inStream = TodoListManagerOuterClass.newTodo.newBuilder().setArg0(name).build().toByteArray();
		byte [] outstream = GlobalGrpcClientRef.grpcClient.genericInvoke(getClientId(), method, ByteString.copyFrom(inStream));
        TodoListOuterClass.TodoList result = TodoListOuterClass.TodoList.parseFrom(outstream);
        // check and create the stub instance and set all the field values in it
        TodoList_Stub stub = null;

        if (!result.getSid().isEmpty()) {
            /* Inner SO */
            /* TODO: Check if stub exists for this sid on the client side. Create stub if not exists. otherwise return stub  */
            stub = new TodoList_Stub();

            /* Get client and app stub */
            AppGrpcClient.SapphireClientInfo sapphireClientInfo = GlobalGrpcClientRef.grpcClient.acquireSapphireObjRef(result.getSid());
            stub.setClientId( sapphireClientInfo.getClientId());
            stub.setSapphireObjId( sapphireClientInfo.getSapphireId());
            stub.setObject(result);
            /* TODO: Add the stub to object store if created now */
        } else {
            stub = new TodoList_Stub();
            stub.setObject(result);
        }

        /* TODO: Need to copy all the fields from deserialized object to stub object */
		return stub;
	}
}
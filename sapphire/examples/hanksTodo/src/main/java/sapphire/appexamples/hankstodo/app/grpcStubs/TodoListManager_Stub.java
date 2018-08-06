package sapphire.appexamples.hankstodo.app.grpcStubs;


import com.google.protobuf.ByteString;

import hankstodo.app.TodoListManagerOuterClass;
import hankstodo.app.TodoListOuterClass;
import sapphire.appexamples.hankstodo.app.AppGrpcClient;
import sapphire.appexamples.hankstodo.app.GlobalGrpcClientRef;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;

import static com.dyuproject.protostuff.runtime.RuntimeSchema.getSchema;;

/**
 * Created by root1 on 25/7/18.
 */

public final class TodoListManager_Stub {
	private final Schema<TodoListManagerOuterClass.TodoListManager> TODOLISTMGR_SCHEMA = getSchema(TodoListManagerOuterClass.TodoListManager.class);
	private final Schema<TodoListOuterClass.TodoList> TODOLIST_SCHEMA = getSchema(TodoListOuterClass.TodoList.class);
	private final Schema<TodoListManagerOuterClass.newTodo> NEWTODO_SCHEMA = getSchema(TodoListManagerOuterClass.newTodo.class);
	private final LinkedBuffer BUFFER = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
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

	public static <T> byte[] serialize(T obj, Schema<T> schema, LinkedBuffer buffer) {
		try {
			return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
		} finally {
			buffer.clear();
		}
	}

	public static <T> T deserialize(byte[] stream, Schema<T> schema) throws Exception {
		T obj = schema.newMessage();
		ProtostuffIOUtil.mergeFrom(stream, obj, schema);
		return obj;
	}

	// user create instance of this obj
	public TodoListManager_Stub () throws Exception {
		/* create sapphire obj on server */
		sapphireObjId = GlobalGrpcClientRef.grpcClient.createSapphireObject("hankstodo.app.TodoListManager", "java", "hankstodo.app.TodoListManager",null);

        /* Get client and app stub */
		AppGrpcClient.SapphireClientInfo sapphireClientInfo = GlobalGrpcClientRef.grpcClient.acquireSapphireObjRef(sapphireObjId);
		clientId = sapphireClientInfo.getClientId();

		object = deserialize(sapphireClientInfo.getOpaqueObject(), TODOLISTMGR_SCHEMA);
	}

	//rpc call
	public TodoList_Stub newTodoList(String name) throws Exception {
		String method = "newTodoList";
		byte [] inStream = serialize(TodoListManagerOuterClass.newTodo.newBuilder().setArg0(name).build(), NEWTODO_SCHEMA, BUFFER);
		byte [] outstream = GlobalGrpcClientRef.grpcClient.genericInvoke(getClientId(), method, ByteString.copyFrom(inStream));
		TodoListOuterClass.TodoList result = deserialize(outstream, TODOLIST_SCHEMA);

        // check and create the stub instance and set all the field values in it
        TodoList_Stub stub = null;

        if (result.hasSid()) {
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
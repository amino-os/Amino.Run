package sapphire.appexamples.hankstodo.app.grpcStubs;


import com.dyuproject.protostuff.ByteArrayInput;
import com.dyuproject.protostuff.ProtobufIOUtil;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;

import net.webby.protostuff.runtime.ProtostuffDefault;

import hankstodo.app.TodoListManagerOuterClass;
import hankstodo.app.TodoListOuterClass;
import sapphire.appexamples.hankstodo.app.AppGrpcClient;
import sapphire.appexamples.hankstodo.app.GlobalGrpcClientRef;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;

import java.io.ByteArrayInputStream;

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

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
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
		Schema<TodoListOuterClass.TodoList> TODOLIST_SCHEMA = getSchema(TodoListOuterClass.TodoList.class);
		String method = "newTodoList";
		byte [] inStream = serialize(TodoListManagerOuterClass.newTodo.newBuilder().setArg0(name).build(), NEWTODO_SCHEMA, BUFFER);

		byte [] outstream = GlobalGrpcClientRef.grpcClient.genericInvoke(getClientId(), method, ByteString.copyFrom(inStream));
		TodoListOuterClass.TodoList result = deserialize(outstream, TODOLIST_SCHEMA);

		// check and create the stub instance and set all the field values in it
		TodoList_Stub todoList = new TodoList_Stub();
		todoList.setObject(result);
		return todoList;
	}
}
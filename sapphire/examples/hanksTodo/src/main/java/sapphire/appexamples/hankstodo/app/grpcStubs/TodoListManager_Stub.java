package sapphire.appexamples.hankstodo.app.grpcStubs;


import com.google.protobuf.MessageLite;

import net.webby.protostuff.runtime.ProtostuffDefault;

import hankstodo.app.TodoListManagerOuterClass;
import hankstodo.app.TodoListOuterClass;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import sapphire.appexamples.hankstodo.app.AppGrpcClient;
import sapphire.appexamples.hankstodo.app.GlobalGrpcClientRef;

import static io.protostuff.runtime.RuntimeSchema.getSchema;

/**
 * Created by root1 on 25/7/18.
 */

public final class TodoListManager_Stub implements TodoListManagerOuterClass.TodoListManagerOrBuilder {
	public final Schema<TodoListManagerOuterClass.TodoListManager> TODOLISTMGR_SCHEMA = getSchema(TodoListManagerOuterClass.TodoListManager.class);
	//public final Schema<TodoListOuterClass.TodoList> TODOLIST_SCHEMA = getSchema(TodoListOuterClass.TodoList.class);
	public final Schema<TodoListManagerOuterClass.newTodo> NEWTODO_SCHEMA = getSchema(TodoListManagerOuterClass.newTodo.class);
	private final LinkedBuffer BUFFER = LinkedBuffer.allocate();
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

	public static <T> T deserialize(byte[] stream, Schema<T> schema) {
		T obj = schema.newMessage();
		ProtostuffIOUtil.mergeFrom(stream, obj, schema);
		return obj;
	}

	public TodoListManager_Stub () {
		sapphireObjId = GlobalGrpcClientRef.grpcClient.createSapphireObject("hankstodo.app.TodoListManager", "java", "hankstodo.app.TodoListManager",null);
		AppGrpcClient.SapphireClientInfo sapphireClientInfo = GlobalGrpcClientRef.grpcClient.acquireSapphireObjRef(sapphireObjId);
		clientId = sapphireClientInfo.getClientId();
		object = deserialize(sapphireClientInfo.getOpaqueObject(), TODOLISTMGR_SCHEMA);
	}

	public TodoList_Stub newTodoList(String name) {
		Schema<TodoListOuterClass.TodoList> TODOLIST_SCHEMA = getSchema(TodoListOuterClass.TodoList.class);
		String method = "newTodoList";
		byte [] inStream = serialize(TodoListManagerOuterClass.newTodo.newBuilder().setArg0(name).build(), NEWTODO_SCHEMA, BUFFER);
		byte [] outstream = GlobalGrpcClientRef.grpcClient.genericInvoke(getClientId(), method, inStream);
		TodoListOuterClass.TodoList result = deserialize(outstream, TODOLIST_SCHEMA);
		TodoList_Stub todoList = new TodoList_Stub();
		todoList.setObject(result);
		return todoList;
	}

	@Override
	public boolean hasTodoLists() {
		return false;
	}

	@Override
	public ProtostuffDefault.MapObjectObject getTodoLists() {
		return null;
	}

	@Override
	public MessageLite getDefaultInstanceForType() {
		return null;
	}

	@Override
	public boolean isInitialized() {
		return false;
	}
}
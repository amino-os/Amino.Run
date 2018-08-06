package sapphire.appexamples.hankstodo.app.grpcStubs;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.google.protobuf.ByteString;
import sapphire.appexamples.hankstodo.app.GlobalGrpcClientRef;
import hankstodo.app.TodoListOuterClass;
import com.dyuproject.protostuff.Schema;
import static com.dyuproject.protostuff.runtime.RuntimeSchema.getSchema;;


/**
 * Created by root1 on 25/7/18.
 */

public final class TodoList_Stub {
    private final Schema<TodoListOuterClass.TodoList> TODOLIST_SCHEMA = getSchema(TodoListOuterClass.TodoList.class);
    private final Schema<TodoListOuterClass.inAddToDo> INADDTODO_SCHEMA = getSchema(TodoListOuterClass.inAddToDo.class);
    private final Schema<TodoListOuterClass.outAddToDo> OUTTODO_SCHEMA = getSchema(TodoListOuterClass.outAddToDo.class);
    private final LinkedBuffer BUFFER = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
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

	public String addToDo(String todo) throws Exception {
        String method = "addToDo";
        if (null == sapphireObjId) {
            /* Local invocation in case of stub instance is not for a sapphire object */
            //TODO: Need to check on how to update the local object */
            return "OK!";
        }
        byte [] inStream = serialize(TodoListOuterClass.inAddToDo.newBuilder().setArg0(todo).build(), INADDTODO_SCHEMA, BUFFER);
        byte [] outstream = GlobalGrpcClientRef.grpcClient.genericInvoke(getClientId(), method, ByteString.copyFrom(inStream));
        TodoListOuterClass.outAddToDoOrBuilder result = deserialize(outstream, OUTTODO_SCHEMA);

        /* TODO: Check if the method return type is primitive or string or autoboxed, then return them as is to app */
        String stub = new String(result.getArg0());
        return stub;
	}
}
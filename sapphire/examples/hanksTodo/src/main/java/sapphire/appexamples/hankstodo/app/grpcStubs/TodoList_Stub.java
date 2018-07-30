package sapphire.appexamples.hankstodo.app.grpcStubs;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;

import net.webby.protostuff.runtime.ProtostuffDefault;

import java.util.List;

import hankstodo.app.TodoListOuterClass;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;


/**
 * Created by root1 on 25/7/18.
 */

public final class TodoList_Stub implements TodoListOuterClass.TodoListOrBuilder {
	public static final Schema<TodoListOuterClass.TodoList> TODOLIST_SCHEMA = RuntimeSchema.getSchema(TodoListOuterClass.TodoList.class);
	private String sapphireObjId;
	private String clientId;
	private TodoListOuterClass.TodoList object;

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getSapphireObjId() {
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

	public String addToDo(String todo) {
		System.out.println("Inside todo: " + todo);
		return "OK!";
	}

	@Override
	public List<ProtostuffDefault.DynamicObject> getToDosList() {
		return null;
	}

	@Override
	public ProtostuffDefault.DynamicObject getToDos(int index) {
		return null;
	}

	@Override
	public int getToDosCount() {
		return 0;
	}

	@Override
	public boolean hasName() {
		return false;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public ByteString getNameBytes() {
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
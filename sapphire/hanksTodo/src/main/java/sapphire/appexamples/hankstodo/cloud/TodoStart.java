package sapphire.appexamples.hankstodo.cloud;

import sapphire.app.AppEntryPoint;
import sapphire.app.AppObjectNotCreatedException;
import sapphire.appexamples.hankstodo.app.TodoListManager;
import sapphire.runtime.Sapphire;
import sapphire.common.AppObjectStub;

public class TodoStart implements AppEntryPoint {

	@Override
	public AppObjectStub start() throws AppObjectNotCreatedException {
			return (AppObjectStub) Sapphire.new_(TodoListManager.class);
	}
}

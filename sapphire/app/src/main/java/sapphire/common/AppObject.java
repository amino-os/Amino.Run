package sapphire.common;


public class AppObject extends ObjectHandler {

	@Override
	protected Class<?> getClass(Object obj) {
		return obj.getClass().getSuperclass();
	}

	public AppObject(Object obj) {
		super(obj);
	}
}

package sapphire.common;


public class AppObject extends ObjectHandler {

	@Override
	protected Class<?> getClass(Object obj) {
		return obj.getClass().getSuperclass();
	}

	public AppObject(Object obj) {
		super(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o);
	}
}

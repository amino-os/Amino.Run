package sapphire.common;

public class AppObject extends ObjectHandler {

    @Override
    public Class<?> getClass(Object obj) {
        return obj.getClass().getSuperclass();
    }

    public AppObject(Object obj) {
        super(obj);
    }
}

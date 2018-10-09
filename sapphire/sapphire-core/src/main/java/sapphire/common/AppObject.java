package sapphire.common;

public class AppObject extends ObjectHandler {

    @Override
    protected Class<?> getClass(Object obj) {
        if (super.isGraalObject()) {
            return obj.getClass();
        }

        return obj.getClass().getSuperclass();
    }

    public AppObject(Object obj) {
        super(obj);
    }
}

package amino.run.common;

public class AppObject extends ObjectHandler {

    @Override
    protected Class<?> getClass(Object obj) {
        if (isGraalObject(obj)) {
            return obj.getClass();
        }

        return obj.getClass().getSuperclass();
    }

    public AppObject(Object obj) {
        super(obj);
    }
}

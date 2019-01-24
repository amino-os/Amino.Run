package amino.run.runtime;

public class Event {
    private String method;

    public Event(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }
}

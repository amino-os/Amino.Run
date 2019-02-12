package amino.run.runtime;

public class Event {
    private String method;
    private String name;

    public Event(String name, String method) {
        this.method = method;
        this.name = name;
    }

    public String getMethod() {
        return method;
    }

    public String getName() {
        return name;
    }
}

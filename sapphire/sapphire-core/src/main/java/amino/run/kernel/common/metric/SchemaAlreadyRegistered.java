package amino.run.kernel.common.metric;

/** Thrown when Schema already present on Metric server. */
public class SchemaAlreadyRegistered extends Exception {
    public SchemaAlreadyRegistered() {
        // TODO Auto-generated constructor stub
    }

    public SchemaAlreadyRegistered(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public SchemaAlreadyRegistered(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    public SchemaAlreadyRegistered(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }
}

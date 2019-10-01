package amino.run.oms.migrationdecision;

/**
 * MetricNotFoundException is the superclass of those exceptions that can be thrown due to metric
 * unavailability issues
 *
 * @author AmitRoushan
 */
public class MetricNotFoundException extends Exception {

    public MetricNotFoundException() {
        super();
    }

    public MetricNotFoundException(String message) {
        super(message);
    }

    public MetricNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public MetricNotFoundException(Throwable cause) {
        super(cause);
    }
}

package sapphire.policy.scalability.masterslave;

import java.io.Serializable;
import java.util.Objects;

/**
 * Log entry is the basic unit for logging and replication.
 *
 * @author terryz
 */
public class LogEntry extends Entry implements Serializable {
    private final MethodInvocationRequest request;

    public LogEntry(LogEntry.Builder builder) {
        super(builder.term, builder.index);
        this.request = builder.request;
    }

    public static LogEntry.Builder newBuilder() {
        return new LogEntry.Builder();
    }

    public final MethodInvocationRequest getRequest() {
        return this.request;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LogEntry)) return false;
        if (!super.equals(o)) return false;
        LogEntry logEntry = (LogEntry) o;
        return Objects.equals(getRequest(), logEntry.getRequest());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getRequest());
    }

    public static final class Builder {
        private long term;
        private long index;
        private MethodInvocationRequest request;

        public LogEntry.Builder term(long term) {
            this.term = term;
            return this;
        }

        public LogEntry.Builder index(long index) {
            this.index = index;
            return this;
        }

        public LogEntry.Builder request(MethodInvocationRequest request) {
            this.request = request;
            return this;
        }

        public LogEntry build() {
            if (request == null) {
                throw new NullPointerException("request is null");
            }

            return new LogEntry(this);
        }
    }
}

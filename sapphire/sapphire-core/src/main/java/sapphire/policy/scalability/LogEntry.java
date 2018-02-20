package sapphire.policy.scalability;

import sapphire.runtime.MethodInvocationRequest;

/**
 * @author terryz
 */
public class LogEntry extends Entry {
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

    public final static class Builder {
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

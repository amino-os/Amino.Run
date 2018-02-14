package sapphire.policy.scalability;

import java.io.Serializable;

import sapphire.runtime.MethodInvocationRequest;

/**
 * @author terryz
 */
public class LogEntry implements Serializable {
    private final long term;
    private final long index;
    private final MethodInvocationRequest request;
    private final Object appObjectSnapshot;

    public LogEntry(Builder builder) {
        this.term = builder.term;
        this.index = builder.index;
        this.request = builder.request;
        this.appObjectSnapshot = builder.appObjectSnapshot;
    }

    public long getTerm() {
        return this.term;
    }

    public long getIndex() {
        return this.index;
    }

    public MethodInvocationRequest getRequest() {
        return this.request;
    }

    public Object getAppObjectSnapshot() {
        return this.appObjectSnapshot;
    }

    public final static class Builder {
        private long term;
        private long index;
        private MethodInvocationRequest request;
        private Object appObjectSnapshot;

        public Builder(long term) {
            this.term = term;
        }

        public Builder index(long index) {
            this.index = index;
            return this;
        }

        public Builder request(MethodInvocationRequest request) {
            this.request = request;
            return this;
        }

        public Builder appObjectSnapshot(Object appObjectSnapshot) {
            this.appObjectSnapshot = appObjectSnapshot;
            return this;
        }

        public LogEntry build() {
            return new LogEntry(this);
        }
    }
}

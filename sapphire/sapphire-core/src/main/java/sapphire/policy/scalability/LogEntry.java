package sapphire.policy.scalability;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

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

    public static Builder newBuilder() {
        return new Builder();
    }

    public final long getTerm() {
        return this.term;
    }

    public final long getIndex() {
        return this.index;
    }

    public final MethodInvocationRequest getRequest() {
        return this.request;
    }

    public final Object getAppObjectSnapshot() {
        return this.appObjectSnapshot;
    }

    public final static class Builder {
        private long term;
        private long index;
        private MethodInvocationRequest request;
        private Object appObjectSnapshot;

        public Builder term(long term) {
            this.term = term;
            return this;
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
            if (term < 0) {
                throw new IllegalArgumentException(String.format("invalid negative term(%s)", term));
            }

            if (index < 0) {
                throw new IllegalArgumentException(String.format("invalid index term(%s)", index));
            }

            if (request == null) {
                throw new NullPointerException("request is null");
            }

            return new LogEntry(this);
        }
    }
}

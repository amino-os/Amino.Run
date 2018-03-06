package sapphire.policy.scalability;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by terryz on 2/15/18.
 */
public class SequenceGenerator {
    private final String name;
    private final AtomicLong sequence;
    private final long step;

    private SequenceGenerator(Builder builder) {
        this.name = builder.name;
        this.sequence = new AtomicLong(builder.startingNumber);
        this.step = builder.step;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public final long getNextSequence() {
        return sequence.getAndAdd(step);
    }

    @Override
    public String toString() {
        return "SequenceGenerator{" +
                "name='" + name + '\'' +
                ", sequence=" + sequence +
                ", step=" + step +
                '}';
    }

    public static class Builder {
        private String name = "seq";
        private long startingNumber = 0L;
        private long step = 1L;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder startingAt(long startingNumber) {
            this.startingNumber = startingNumber;
            return this;
        }

        public Builder step(long step) {
            this.step = step;
            return this;
        }

        public SequenceGenerator build() {
            if (step <= 0) {
                throw new IllegalArgumentException(String.format("invalid sequence step(%s)", step));
            }

            return new SequenceGenerator(this);
        }
    }
}

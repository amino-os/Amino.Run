package amino.run.policy.scalability.masterslave;

import com.google.common.base.Objects;
import java.io.Serializable;

/** @author terryz */
public abstract class Entry implements Serializable {
    private long term;
    private long index;

    public Entry(long term, long index) {
        if (term < 0) {
            throw new IllegalArgumentException(String.format("invalid negative term(%s)", term));
        }

        if (index < 0) {
            throw new IllegalArgumentException(String.format("invalid index term(%s)", index));
        }

        this.term = term;
        this.index = index;
    }

    public final long getTerm() {
        return this.term;
    }

    public final long getIndex() {
        return this.index;
    }

    public final Entry setIndex(long index) {
        if (index < 0) {
            throw new IllegalArgumentException(String.format("invalid negative index %s", index));
        }

        this.index = index;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entry)) return false;
        Entry entry = (Entry) o;
        return getTerm() == entry.getTerm() && getIndex() == entry.getIndex();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getTerm(), getIndex());
    }
}

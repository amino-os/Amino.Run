package sapphire.policy.scalability;

import java.io.Serializable;

/**
 * @author terryz
 */
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

    public final Entry setTerm(long term) {
        if (term < 0) {
            throw new IllegalArgumentException(String.format("invalid negative term %s", term));
        }

        this.term = term;
        return this;
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
}

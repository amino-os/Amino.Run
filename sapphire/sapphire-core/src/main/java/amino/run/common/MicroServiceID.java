package amino.run.common;

import java.io.Serializable;
import java.util.UUID;

public class MicroServiceID implements Serializable {
    private final UUID oid;

    public MicroServiceID(UUID oid) {
        this.oid = oid;
    }

    public UUID getID() {
        return this.oid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MicroServiceID that = (MicroServiceID) o;

        return oid.equals(that.oid);
    }

    @Override
    public int hashCode() {
        return oid.hashCode();
    }

    @Override
    public String toString() {
        String ret = "MicroService(" + oid + ")";
        return ret;
    }
}

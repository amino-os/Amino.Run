package amino.run.common;

import java.io.Serializable;
import java.util.UUID;

public class ReplicaID implements Serializable {
    private final MicroServiceID oid;
    private final UUID rid;

    public ReplicaID(MicroServiceID oid, UUID rid) {
        this.oid = oid;
        this.rid = rid;
    }

    public MicroServiceID getOID() {
        return this.oid;
    }

    public UUID getID() {
        return this.rid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReplicaID that = (ReplicaID) o;

        return rid.equals(that.rid);
    }

    @Override
    public int hashCode() {
        return rid.hashCode();
    }

    @Override
    public String toString() {
        String ret = "Replica(" + oid + "," + rid + ")";
        return ret;
    }
}

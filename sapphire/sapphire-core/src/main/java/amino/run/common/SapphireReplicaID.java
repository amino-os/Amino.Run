package amino.run.common;

import java.io.Serializable;
import java.util.UUID;

public class SapphireReplicaID implements Serializable {
    private final SapphireObjectID oid;
    private final UUID rid;

    public SapphireReplicaID(SapphireObjectID oid, UUID rid) {
        this.oid = oid;
        this.rid = rid;
    }

    public SapphireObjectID getOID() {
        return this.oid;
    }

    public UUID getID() {
        return this.rid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SapphireReplicaID that = (SapphireReplicaID) o;

        return rid.equals(that.rid);
    }

    @Override
    public int hashCode() {
        return rid.hashCode();
    }

    @Override
    public String toString() {
        String ret = "SapphireReplica(" + oid + "," + rid + ")";
        return ret;
    }
}

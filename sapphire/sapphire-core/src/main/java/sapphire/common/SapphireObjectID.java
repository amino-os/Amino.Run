package sapphire.common;

import java.io.Serializable;

public class SapphireObjectID implements Serializable {
    private int oid;

    public SapphireObjectID(int oid) {
        this.oid = oid;
    }

    public int getID() {
        return this.oid;
    }

    @Override
    public boolean equals(Object obj) {
        final SapphireObjectID other = (SapphireObjectID) obj;
        if (oid != other.getID()) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return oid;
    }

    @Override
    public String toString() {
        String ret = "SapphireObject(" + oid + ")";
        return ret;
    }
}

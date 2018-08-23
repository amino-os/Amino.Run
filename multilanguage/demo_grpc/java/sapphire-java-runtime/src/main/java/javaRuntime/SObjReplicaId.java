/** Created by Jithu Thomas on 18/7/18. */
package javaRuntime;

public class SObjReplicaId {

    public class ReplicaId {
        public String sObjId;
        public String sObjReplicaId;

        public ReplicaId() {
            sObjId = null;
            sObjReplicaId = null;
        }

        public ReplicaId(String objId, String replicaId) {
            sObjId = objId;
            sObjReplicaId = replicaId;
        }

        public void setReplicaId(String objId, String replicaId) {
            sObjId = objId;
            sObjReplicaId = replicaId;
        }

        public ReplicaId getReplicaId() {
            return (new ReplicaId(sObjId, sObjReplicaId));
        }
    }

    public ReplicaId sObjReplicaId;
    public String sObjParentSObjId;

    public SObjReplicaId() {
        sObjReplicaId = new ReplicaId(null, null);
        sObjParentSObjId = null;
    }

    public SObjReplicaId(String sObjId, String replicaId, String sObjParentId) {
        sObjReplicaId = new ReplicaId(sObjId, replicaId);
        sObjParentSObjId = sObjParentId;
    }

    public void setsObjReplicaId(String sObjId, String replicaId, String sObjParentId) {
        sObjReplicaId.setReplicaId(sObjId, replicaId);
        sObjParentSObjId = sObjParentId;
    }

    public SObjReplicaId getSObjReplicaId() {
        return (new SObjReplicaId(
                sObjReplicaId.sObjId, sObjReplicaId.sObjReplicaId, sObjParentSObjId));
    }

    @Override
    public boolean equals(Object obj) {
        SObjReplicaId other = (SObjReplicaId) obj;
        return sObjReplicaId.sObjId.equals(other.sObjReplicaId.sObjId) && sObjReplicaId.sObjReplicaId.equals(other.sObjReplicaId.sObjReplicaId);
    }

    @Override
    public int hashCode() {
        return Integer.parseInt(sObjReplicaId.sObjId);
    }
}

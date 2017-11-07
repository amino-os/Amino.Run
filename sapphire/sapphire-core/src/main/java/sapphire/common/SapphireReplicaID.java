package sapphire.common;

public class SapphireReplicaID {
	private SapphireObjectID oid;
	private int rid;
	
	public SapphireReplicaID(SapphireObjectID oid, int rid) {
		this.oid = oid;
		this.rid = rid;
	}
	
	public SapphireObjectID getOID() {
		return this.oid;
	}
	
	public int getID() {
		return this.rid;
	}
	
	@Override
	public boolean equals(Object obj) {
		final SapphireReplicaID other = (SapphireReplicaID) obj;
		if (oid != other.getOID() || rid != other.getID())
			return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		return (oid.getID()<<16)|rid;
	}
	
	@Override
	public String toString() {
		String ret = "SapphireReplica("+oid+","+rid+")";
		return ret;
	}
}
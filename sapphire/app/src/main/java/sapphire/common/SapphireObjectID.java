package sapphire.common;

import sapphire.kernel.common.KernelOID;

public class SapphireObjectID {
	private int oid;
	
	public SapphireObjectID(int oid) {
		this.oid = oid;
	}
	
	public int getID() {
		return this.oid;
	}
	
	@Override
	public boolean equals(Object obj) {
		final KernelOID other = (KernelOID) obj;
		if (oid != other.getID())
			return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		return oid;
	}
	
	@Override
	public String toString() {
		String ret = "SapphireObject("+oid+")";
		return ret;
	}
}

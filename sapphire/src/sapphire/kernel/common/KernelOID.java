package sapphire.kernel.common;

import java.io.Serializable;

/** 
 * ID for a Sapphire kernel object
 * 
 * @author iyzhang
 *
 */

public class KernelOID implements Serializable {
	private int oid;
	
	public KernelOID(int oid) {
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
		String ret = "KernelObject("+oid+")";
		return ret;
	}
}

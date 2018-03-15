package sapphire.kernel.common;
import java.io.Serializable;
import java.util.*;

/** 
 * Sapphire Kernel RPC
 * Includes the object being called, the method and the parameters
 * @author iyzhang
 *
 */

public class KernelRPC implements Serializable {
	private KernelOID oid;
	private String method;
	private ArrayList<Object> params;
	
	public KernelRPC(KernelOID oid, String method, ArrayList<Object> params) {
		this.oid = oid;
		this.method = method;
		this.params = params;
	}
	
	public KernelOID getOID() {
		return oid;
	}
	
	public String getMethod() {
		return method;
	}
	
	public ArrayList<Object> getParams() {
		return params;
	}
	
	@Override
	public String toString() {
		String ret = method;
		if (params.size() > 0) {
			ret += "(";
			Iterator<Object> it = params.iterator();
			for (Object obj = it.next(); it.hasNext(); obj = it.next()) {
				if (it.hasNext()) {
					ret = ret + obj.toString() + ",";
				} else {
					ret = ret + obj.toString() + ")";
				}
			}
		}
		return ret;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof KernelRPC)) return false;

		KernelRPC kernelRPC = (KernelRPC) o;

		if (oid != null ? !oid.equals(kernelRPC.oid) : kernelRPC.oid != null) return false;
		if (getMethod() != null ? !getMethod().equals(kernelRPC.getMethod()) : kernelRPC.getMethod() != null)
			return false;
		return getParams() != null ? getParams().equals(kernelRPC.getParams()) : kernelRPC.getParams() == null;
	}

	@Override
	public int hashCode() {
		int result = oid != null ? oid.hashCode() : 0;
		result = 31 * result + (getMethod() != null ? getMethod().hashCode() : 0);
		result = 31 * result + (getParams() != null ? getParams().hashCode() : 0);
		return result;
	}
}

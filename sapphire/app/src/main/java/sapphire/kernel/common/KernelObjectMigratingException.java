package sapphire.kernel.common;

/**
 * Sapphire Kernel throws this exception if it receives an RPC for a kernel
 * object that is currently migrating. The caller is expected to contact the
 * OMS later and try again.
 * 
 * @author iyzhang
 *
 */

public class KernelObjectMigratingException extends Exception {

	public KernelObjectMigratingException() {
		// TODO Auto-generated constructor stub
	}

	public KernelObjectMigratingException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public KernelObjectMigratingException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public KernelObjectMigratingException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

}

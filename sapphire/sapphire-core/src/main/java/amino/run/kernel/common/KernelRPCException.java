package amino.run.kernel.common;

/**
 * Exception thrown when an RPC to a kernel object causes an exception in the object itself.
 *
 * @author iyzhang
 */
public class KernelRPCException extends Exception {

    private Exception e;

    public KernelRPCException(Exception e) {
        // TODO Auto-generated constructor stub
        this.e = e;
    }

    public KernelRPCException(String detailMessage) {
        super(detailMessage);
        e = this;
    }

    public Exception getException() {
        return e;
    }
}

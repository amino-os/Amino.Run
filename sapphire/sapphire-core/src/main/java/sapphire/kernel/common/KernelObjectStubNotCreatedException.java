package sapphire.kernel.common;

/** Created by Venugopal Reddy K 00900280 on 16/7/18. */

/** Kernel object stub creation failure exception */
public class KernelObjectStubNotCreatedException extends Exception {
    public KernelObjectStubNotCreatedException() {}

    public KernelObjectStubNotCreatedException(String detailMessage) {
        super(detailMessage);
    }

    public KernelObjectStubNotCreatedException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public KernelObjectStubNotCreatedException(Throwable throwable) {
        super(throwable);
    }
}

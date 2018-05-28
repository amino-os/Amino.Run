package sapphire.kernel.common;

/**
 * Created by SrinivasChilveri on 5/28/18.
 * Server overload exception
 */



public class KernelServerNotFoundException extends Exception {

    public KernelServerNotFoundException() {}

    public KernelServerNotFoundException(String message) {
        super(message);
    }

    public KernelServerNotFoundException(Throwable cause) {
        super(cause);
    }

    public KernelServerNotFoundException(String message, Throwable cause) { super(message, cause); }
}

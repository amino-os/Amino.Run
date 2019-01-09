package amino.run.policy.mobility.explicitmigration;

import java.net.InetSocketAddress;

/** Created by Malepati Bala Siva Sai Akhil on 2/2/18. */
public class NotFoundDestinationKernelServerException extends MigrationException {

    private InetSocketAddress notFoundDestinationAddr;

    public NotFoundDestinationKernelServerException() {}

    public NotFoundDestinationKernelServerException(String message) {
        super(message);
    }

    public NotFoundDestinationKernelServerException(Throwable cause) {
        super(cause);
    }

    public NotFoundDestinationKernelServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundDestinationKernelServerException(
            InetSocketAddress notFoundDestinationAddr, String message) {
        super(message);
        this.notFoundDestinationAddr = notFoundDestinationAddr;
    }

    public InetSocketAddress getNotFoundDestinationAddress() {
        return notFoundDestinationAddr;
    }
}

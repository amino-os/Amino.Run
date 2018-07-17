package sapphire.policy.scalability.masterslave;

import java.io.Closeable;
import java.util.concurrent.Future;

/** @author terryz */
public interface Replicator extends Closeable {
    /** Initializes the replicator */
    void open();

    /**
     * Replicates the given request to remote servers synchronously
     *
     * @param request request to be replicated
     * @return replication response
     */
    ReplicationResponse replicateInSync(ReplicationRequest request);

    /**
     * Replicates the given request to remote servers asynchronously
     *
     * @param request request to be replicated
     * @return a promise of the replication response
     */
    Future<ReplicationResponse> replicateInAsync(ReplicationRequest request);
}

package amino.run.kernel.common.metric;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

/**
 * RPC metrics are computed on kernel server at {@link
 * amino.run.kernel.server.KernelObject#invoke(UUID, String, ArrayList)} and stored in {@link
 * amino.run.kernel.server.KernelObject} for kernel object whenever it receives RPC call for that
 * particular kernel object. Server policy reports these collected metrics periodically to its group
 * policy.
 */
public class RPCMetric implements Serializable {
    public long dataSize; // Data IN and OUT sizes
    public long processTime; // RPC processing time

    @Override
    public String toString() {
        return "RPCMetric{" + "dataSize=" + dataSize + ", processTime=" + processTime + '}';
    }
}

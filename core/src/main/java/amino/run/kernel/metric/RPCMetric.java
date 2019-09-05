package amino.run.kernel.metric;

import amino.run.common.AppObjectStub;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.UUID;

/**
 * RPC metrics are computed on kernel server at {@link
 * amino.run.kernel.server.KernelObject#invoke(AppObjectStub.Context, String, ArrayList)} and stored
 * in {@link amino.run.kernel.server.KernelObject} for kernel object whenever it receives RPC call
 * for that particular kernel object. Server policy reports these collected metrics periodically to
 * its group policy.
 */
public class RPCMetric implements Serializable {
    public final UUID callerId; /* Client for which below data is measured */
    public final InetSocketAddress clientHost; /* Kernel server address on client */
    public long dataSize; /* Aggregation of RPC call data IN and OUT in bytes. */
    /* Time taken to process the RPC in nanoseconds. Time difference from the moment RPC call has arrived until it is
    returned */
    public long elapsedTime;

    public RPCMetric(UUID callerId, InetSocketAddress host) {
        this.callerId = callerId;
        this.clientHost = host;
    }

    @Override
    public String toString() {
        return "RPCMetric{"
                + "callerId="
                + callerId
                + ", clientHost="
                + clientHost
                + ", dataSize(bytes)="
                + dataSize
                + ", elapsedTime(ns)="
                + elapsedTime
                + '}';
    }
}

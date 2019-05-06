package amino.run.kernel.metric;

import amino.run.common.AppObjectStub;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;

/**
 * RPC metrics are computed on kernel server at {@link
 * amino.run.kernel.server.KernelObject#invoke(AppObjectStub.Context, String, ArrayList)} and stored
 * in {@link amino.run.kernel.server.KernelObject} for kernel object whenever it receives RPC call
 * for that particular kernel object. Server policy reports these collected metrics periodically to
 * its group policy.
 */
public class RPCMetric implements Serializable {
    public InetSocketAddress clientHost; // Client for which below data is measured
    public long dataSize; // Data IN and OUT sizes
    public long processTime; // RPC processing time

    public RPCMetric(InetSocketAddress host) {
        this.clientHost = host;
    }

    @Override
    public String toString() {
        return "RPCMetric{"
                + "clientHost="
                + clientHost
                + ", dataSize="
                + dataSize
                + ", processTime="
                + processTime
                + '}';
    }
}

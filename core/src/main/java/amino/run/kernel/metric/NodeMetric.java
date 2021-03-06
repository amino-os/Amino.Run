package amino.run.kernel.metric;

import amino.run.kernel.client.KernelClient;
import amino.run.kernel.common.ServerInfo;
import java.io.Serializable;

/**
 * This class is used to hold the local kernel server metric relative to a remote kernel server.
 * Local kernel server collects metrics w.r.t all remote kernel servers in {@link
 * KernelClient#measureMetrics(KernelClient.KernelServerInfo)} and sends to OMS in the heartBeats at
 * {@link amino.run.kernel.server.KernelServerImpl#sendHeartBeat(ServerInfo)}
 */
public class NodeMetric implements Serializable {
    public long latency; // Latency between kernel servers in nanoseconds
    public long rate; // Data transfer rate between kernel servers in Bytes/Sec

    @Override
    public String toString() {
        return "NodeMetric{" + "latency(ns)=" + latency + ", rate(Bps)=" + rate + '}';
    }
}

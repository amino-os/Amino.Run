package amino.run.kernel.common.metric;

import amino.run.kernel.client.KernelClient;
import amino.run.kernel.common.ServerInfo;
import java.io.Serializable;
import org.yaml.snakeyaml.Yaml;

/**
 * This class is used to hold the local kernel server metric relative to a remote kernel server.
 * Local kernel server collects metrics w.r.t all remote kernel servers in {@link
 * KernelClient#measureServerMetrics()} and sends to OMS in the heartBeats at {@link
 * amino.run.kernel.server.KernelServerImpl#sendHeartBeat(ServerInfo)}
 */
public class NodeMetric implements Serializable {
    public long latency; // Latency between kernel servers in nanoseconds
    public double rate; // Data transfer rate between kernel servers in Mbps
}

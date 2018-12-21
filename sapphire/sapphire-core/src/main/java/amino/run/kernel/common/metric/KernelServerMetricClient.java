package amino.run.kernel.common.metric;

import amino.run.kernel.common.metric.clients.LoggingClient;
import amino.run.kernel.common.metric.schema.Schema;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Class implements metric client interface and initializes client by retrieving Metric Server
 * configurations from OMS.
 *
 * <p>If no Metric client configured on OMS, Kernel server by default initializes Metric Logging
 * Client specially for printing Kernel server Metric.
 */
public class KernelServerMetricClient {
    private static Logger logger = Logger.getLogger(KernelServerMetricClient.class.getName());
    private MetricClient client;
    private boolean initialized = false;

    public boolean isInitialized() {
        return initialized;
    }

    public void initialize() {
        // TODO: Metric client creation needs metric server deployed and configured on OMS.
        // OMS will expose RPC API to get metric server information which will
        // be used by each kernel server to create Metric Client.
        // If Metric Server is not configured on OMS, Kernel server will use MetricLoggerClient
        // for Kernel server metric.

        // TODO: Currently Metric client pushes metrics into logs. Add metric client creation which
        // involves :
        // 1. Retrieve Metric Server configurations from OMS
        // 2. Use configuration to connect with Metric Server.
        // 3. Add support for Third party Metric server.
        client = new LoggingClient();
        initialized = true;
    }

    /**
     * Post metrics to configured metric server
     *
     * @param metrics
     * @throws Exception
     */
    public void send(ArrayList<Metric> metrics) throws Exception {
        client.send(metrics);
    }

    /**
     * Register metric schema with configured metric server
     *
     * @param schema
     */
    public void registerSchema(Schema schema) {
        try {
            if (!client.isRegistered(schema)) {
                client.register(schema);
            }
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
    }
}

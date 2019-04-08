package amino.run.kernel.common.metric;

import amino.run.kernel.common.metric.clients.LoggingClient;
import amino.run.kernel.common.metric.metricHandler.MicroServiceMetricManager;
import amino.run.kernel.common.metric.schema.Schema;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * Class implements metric client interface and initializes client by retrieving Metric Server
 * configurations from OMS.
 *
 * <p>Currently if no Metric client configured on OMS, Kernel server by default initializes Metric
 * Logging Client specially for printing Kernel server Metric.
 *
 * @author AmitRoushan
 */
public class KernelMetricClient {
    private static Logger logger = Logger.getLogger(KernelMetricClient.class.getName());
    private HashSet<Schema> schemas = new HashSet<Schema>();
    private MetricClient client;
    private boolean initialized = false;

    public boolean isInitialized() {
        return initialized;
    }

    public void initialize() {
        /*  Metric client creation needs metric server deployed and configured on OMS.
            OMS will expose RPC API to get metric server information which will
            be used by each kernel server to create Metric Client.
            If Metric Server is not configured on OMS, Kernel server will ignore metric reporting.

            TODO: Currently Metric client pushes metrics into logs. Add metric client creation which
            involves :
            1. Retrieve Metric Server configurations from OMS
            2. Use configuration to connect with Metric Server.
            3. Add support for Third party Metric server.
        */
        client = new LoggingClient();
        initialized = true;
    }

    /**
     * Post metrics to configured metric server
     *
     * @param manager
     * @param metrics
     * @throws Exception
     */
    public void send(MicroServiceMetricManager manager, ArrayList<Metric> metrics)
            throws Exception {
        client.send(metrics);
    }

    /**
     * Register metric schema with configured metric server
     *
     * @param manager
     * @param schema
     */
    public void registerSchema(MicroServiceMetricManager manager, Schema schema) {
        try {
            if (!schemas.contains(schema)) {
                schemas.add(schema);
                client.register(schema);
            }
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
    }

    /**
     * UnRegister metric schema with configured metric server
     *
     * @param schema
     */
    public void unRegisterSchema(MicroServiceMetricManager manager, Schema schema) {
        try {
            if (schemas.remove(schema)) {
                client.unregister(schema);
            }
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
    }
}

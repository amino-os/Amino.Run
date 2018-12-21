package amino.run.kernel.common.metric;

import java.util.ArrayList;
import java.util.HashSet;
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
        // TODO: Currently Metric client pushes metrics into logs. Add metric client creation which
        // involves :
        // 1. Retrieve Metric Server configurations from OMS
        // 2. Use configuration to connect with Metric Server.
        // 3. Add support for Third party Metric server.
        client = new Logging();
        initialized = true;
    }

    /**
     * Post metric to configured metric server
     *
     * @param metric
     * @throws Exception
     */
    public void send(Metric metric) throws Exception {
        client.send(metric);
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
    public void registerSchema(MetricSchema schema) {
        try {
            if (!client.isRegistered(schema)) {
                client.register(schema);
            }
        } catch (SchemaAlreadyRegistered e) {
            logger.warning(e.getMessage());
        }
    }

    private class Logging implements MetricClient {
        private HashSet<MetricSchema> schemas = new HashSet<MetricSchema>();

        @Override
        public void send(Metric metric) throws Exception {
            logger.info(String.format("Metric collected : %s", metric));
        }

        @Override
        public void send(ArrayList<Metric> metrics) throws Exception {
            for (Metric metric : metrics) {
                logger.info(String.format("Metric collected : %s", metric));
            }
        }

        @Override
        public boolean isRegistered(MetricSchema schema) {
            return schemas.contains(schema);
        }

        @Override
        public boolean register(MetricSchema schema) throws SchemaAlreadyRegistered {
            if (schemas.contains(schema)) {
                throw new SchemaAlreadyRegistered();
            }
            schemas.add(schema);
            logger.info(String.format("Metric schema %s registered", schema));
            return true;
        }
    }
}

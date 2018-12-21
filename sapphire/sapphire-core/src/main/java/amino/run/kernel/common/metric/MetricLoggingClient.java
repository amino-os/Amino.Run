package amino.run.kernel.common.metric;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * Metric client implementation which prints Metrics on Console.
 *
 * <p>If no Metric client configured on kernel server, Kernel server by default initializes
 * MetricLoggingClient specially for printing Kernel server Metric.
 */
public class MetricLoggingClient implements MetricClient {
    private static Logger logger = Logger.getLogger(MetricLoggingClient.class.getName());
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

package amino.run.kernel.common.metric.clients;

import amino.run.kernel.common.metric.*;
import amino.run.kernel.common.metric.schema.Schema;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * Metric client for logging server. Currently it just push metrics on standard output.
 *
 * @author AmitRoushan
 */
public class LoggingClient implements MetricClient {
    private HashSet<Schema> schemas = new HashSet<Schema>();
    private static Logger logger = Logger.getLogger(LoggingClient.class.getName());

    @Override
    public void send(ArrayList<Metric> metrics) throws Exception {
        for (Metric metric : metrics) {
            if (!schemas.contains(metric.getSchema())) {
                logger.warning(String.format("Unregistered schema : ", metric.getSchema()));
            }
            logger.info(String.format("Metric collected : %s", metric));
        }
    }

    @Override
    public boolean isRegistered(Schema schema) {
        return schemas.contains(schema);
    }

    @Override
    public boolean register(Schema schema) throws Exception {
        if (schemas.contains(schema)) {
            throw new Exception("Schema already Register");
        }
        schemas.add(schema);
        logger.info(String.format("Metric schema %s registered", schema));
        return true;
    }

    @Override
    public boolean unregister(Schema schema) throws Exception {
        schemas.remove(schema);
        logger.info(String.format("Metric schema %s unregistered", schema));
        return true;
    }
}

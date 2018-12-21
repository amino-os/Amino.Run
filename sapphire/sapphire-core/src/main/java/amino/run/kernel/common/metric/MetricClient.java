package amino.run.kernel.common.metric;

import amino.run.kernel.common.metric.schema.Schema;
import java.util.ArrayList;

/** Interface of client used for metric server interactions */
public interface MetricClient {
    /**
     * Post metrics to metric server
     *
     * @param metrics object maintaining metric information
     * @throws Exception
     */
    void send(ArrayList<Metric> metrics) throws Exception;

    /**
     * Check metric schema registration with metric server
     *
     * @param schema which need to check for registration status
     * @return registration status
     */
    boolean isRegistered(Schema schema);

    /**
     * Register metric schema with metric server
     *
     * @param schema which need to register
     * @return registration status
     */
    boolean register(Schema schema) throws Exception;
}

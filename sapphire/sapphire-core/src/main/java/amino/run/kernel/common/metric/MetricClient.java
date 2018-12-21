package amino.run.kernel.common.metric;

import java.util.ArrayList;

/** Interface used by Kernel server and AppObjects for Metric server interaction */
public interface MetricClient {
    /**
     * Post metric to metric server
     *
     * @param metric object maintaining metric information
     * @throws Exception
     */
    void send(Metric metric) throws Exception;

    /**
     * Post metrics to metric server
     *
     * @param metrics object maintaining metric information
     * @throws Exception
     */
    void send(ArrayList<Metric> metrics) throws Exception;

    /**
     * Check metric registration with Metric server
     *
     * @param schema which need to check for registration status
     * @return registration status
     */
    boolean isRegistered(MetricSchema schema);

    /**
     * Register metric schema with Metric server
     *
     * @param schema which need to register
     * @return registration status
     * @throws SchemaAlreadyRegistered if already registered
     */
    boolean register(MetricSchema schema) throws SchemaAlreadyRegistered;
}

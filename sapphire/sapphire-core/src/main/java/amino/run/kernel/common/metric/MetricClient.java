package amino.run.kernel.common.metric;

import amino.run.kernel.common.metric.schema.Schema;
import java.util.ArrayList;

/**
 * Interface of client used for metric server interactions
 *
 * @author AmitRoushan
 */
public interface MetricClient {
    /**
     * Post metrics to metric server
     *
     * @param metrics object maintaining metric information
     * @throws Exception
     */
    void send(ArrayList<Metric> metrics) throws Exception;

    /**
     * Post metric schema information to metric server
     *
     * <p>metric server create data structure to maintain incoming metric based on Schema input.</>
     *
     * @param schema which need to register
     * @return registration status
     */
    boolean register(Schema schema) throws Exception;

    /**
     * Delete metric schema metadata information from metric server
     *
     * <p>This method should be used to inform about non existence of metric source entity. Metric
     * server can maintain metric data but should stop accepting any metric information for
     * specified schema until again registered</>
     *
     * @param schema which need to register
     * @return registration status
     */
    boolean unregister(Schema schema) throws Exception;
}

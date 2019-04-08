package amino.run.kernel.common.metric;

import amino.run.app.MicroServiceSpec;

/**
 * Constants used for {@link MicroServiceSpec} annotations in metric collection configuration.
 *
 * @author AmitRoushan
 */
public class MetricConstants {
    /** Constant for enabling metric collection */
    public static final String METRIC_ANNOTATION = "amino.metric";
    /** Constant for metric collection frequency configuration */
    public static final String METRIC_FREQUENCY = "frequency";
    /** Constant for list of enabled metrics */
    public static final String RPC_METRIC_HANDLERS = "rpcMetrics";
}

package amino.run.kernel.common.metric;

import amino.run.app.MicroServiceSpec;

/** Constants used for {@link MicroServiceSpec} annotations in metric collection configuration. */
public class MetricConstants {
    /** Constant for enabling metric collection */
    public static String METRIC_ANNOTATION = "amino.metric";
    /** Constant for metric collection frequency configuration */
    public static String METRIC_FREQUENCY = "frequency";
    /** Constant for list of enabled metrics */
    public static String RPC_METRIC_HANDLERS = "rpcMetricHandlers";
}

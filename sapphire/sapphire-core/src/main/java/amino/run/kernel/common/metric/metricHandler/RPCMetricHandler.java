package amino.run.kernel.common.metric.metricHandler;

import amino.run.kernel.common.metric.MetricCollector;
import java.util.ArrayList;

/**
 * Base implementation for RPC metric handling.
 *
 * <p>All RPC metric handler should extends this class.
 */
public abstract class RPCMetricHandler implements MetricCollector {
    protected RPCMetricHandler nextHandler;

    /**
     * Collect metric processing and call next metric handler for further metric collection in chain
     *
     * @param method rpc method
     * @param params rpc parameters
     * @return rpc response
     * @throws Exception
     */
    public abstract Object handle(String method, ArrayList<Object> params) throws Exception;

    /**
     * set next rpc metric handler in chain
     *
     * @param handler
     */
    public void setNextHandler(RPCMetricHandler handler) {
        nextHandler = handler;
    }

    /**
     * get next rpc metric handler
     *
     * @return rpc metric handler
     */
    public RPCMetricHandler getNextHandler() {
        return nextHandler;
    }
}

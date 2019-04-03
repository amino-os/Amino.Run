package amino.run.kernel.common.metric.metricHandler;

import java.util.ArrayList;

/**
 * Interface for RPC metric handling.
 *
 * <p>All RPC metric handler need to implement this class.
 */
public abstract class RPCMetricHandler implements MetricHandler {
    private RPCMetricHandler nextHandler = null;
    /**
     * Collect metric processing and call next metric handler for further metric collection in chain
     *
     * @param method rpc method
     * @param params rpc parameters
     * @return rpc response
     * @throws Exception
     */
    public abstract Object handle(String method, ArrayList<Object> params) throws Exception;

    public void setNextHandler(RPCMetricHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    public RPCMetricHandler getNextHandler() {
        return nextHandler;
    }
}

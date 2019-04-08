package amino.run.kernel.common.metric.metricHandler.RPCMetric;

import amino.run.kernel.common.metric.metricHandler.MetricHandler;
import amino.run.kernel.common.metric.metricHandler.MicroServiceMetricManager;
import java.util.ArrayList;

/**
 * Abstract class used for RPC handler chain creation. All RPC metric handler need to implement this
 * class.
 *
 * @author AmitRoushan
 */
public abstract class RPCMetricHandler implements MetricHandler {
    protected MicroServiceMetricManager manager;
    /** Hold next RPC handler in RPC handles's chain */
    private RPCMetricHandler nextHandler = null;

    RPCMetricHandler(MicroServiceMetricManager manager) {
        this.manager = manager;
    }
    /**
     * Method get called in onRPC call for each metric handler . Each RPC handler should implement
     * metric collection logic in this method and call next RPC metric handler for further metric
     * collection in chain
     *
     * @param method rpc method
     * @param params rpc parameters
     * @return rpc response
     * @throws Exception
     */
    public abstract Object handle(String method, ArrayList<Object> params) throws Exception;

    /**
     * Set nest handler object in RCP handler chain
     *
     * @param nextHandler
     */
    public void setNextHandler(RPCMetricHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    /**
     * Return RPC next RPC handler
     *
     * @return
     */
    public RPCMetricHandler getNextHandler() {
        return nextHandler;
    }

    public MicroServiceMetricManager getManager() {
        return manager;
    }
}

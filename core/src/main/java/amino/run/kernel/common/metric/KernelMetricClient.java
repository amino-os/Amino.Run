package amino.run.kernel.common.metric;

import amino.run.kernel.common.metric.clients.LoggingClient;
import amino.run.kernel.common.metric.schema.Schema;
import amino.run.policy.util.ResettableTimer;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Class implements metric client interface and initializes client by retrieving Metric Server
 * configurations from OMS.
 *
 * <p>Currently if no Metric client configured on OMS, Kernel server by default initializes Metric
 * Logging Client specially for printing Kernel server Metric.
 *
 * @author AmitRoushan
 */
public class KernelMetricClient {
    private static Logger logger = Logger.getLogger(KernelMetricClient.class.getName());
    private ConcurrentHashMap<HashMap<String, String>, ResettableTimer> metricManagerTimers =
            new ConcurrentHashMap<HashMap<String, String>, ResettableTimer>();
    private MetricClient client;

    public void initialize() {
        /*  Metric client creation needs metric server deployed and configured on OMS.
            OMS will expose RPC API to get metric server information which will
            be used by each kernel server to create Metric Client.
            If Metric Server is not configured on OMS, Kernel server will ignore metric reporting.

            TODO: Currently Metric client pushes metrics into logs. Add metric client creation which
            involves :
            1. Retrieve Metric Server configurations from OMS
            2. Use configuration to connect with Metric Server.
            3. Add support for Third party Metric server.
        */
        client = new LoggingClient();
    }

    /**
     * Add metric manager instance managed by Kernel metric client.
     *
     * <p>It pull metric schemas from manager and push it to metric server for registration.
     * Initializes and register timer for metric retrieval from metric manager and push it to metric
     * server </>
     *
     * @param manager
     */
    public void registerMetricManager(final MetricManager manager) {
        registerSchema(manager);

        ResettableTimer metricTimer =
                new ResettableTimer(
                        new TimerTask() {
                            public void run() {
                                try {
                                    // collect metric
                                    client.send(manager.getLabels(), manager.getMetrics());
                                } catch (Exception e) {
                                    logger.warning(
                                            String.format(
                                                    "%s: Sending metric failed", e.toString()));
                                    return;
                                }
                                // reset the count value and timer after push is done
                                metricManagerTimers.get(manager.getLabels()).reset();
                            }
                        },
                        manager.getMetricUpdateFrequency());
        metricTimer.start();
        metricManagerTimers.put(manager.getLabels(), metricTimer);
    }

    /**
     * Unregister and remove metric manager schema and timer
     *
     * @param manager
     */
    public void unregisterMetricManager(MetricManager manager) {
        metricManagerTimers.get(manager.getLabels()).cancel();
        metricManagerTimers.remove(manager.getLabels());
    }

    /**
     * Register metric schema with configured metric server
     *
     * @param manager
     */
    public void registerSchema(MetricManager manager) {
        for (Schema schema : manager.getSchemas()) {
            try {
                client.register(schema);
            } catch (Exception e) {
                logger.warning(e.getMessage());
            }
        }
    }
}

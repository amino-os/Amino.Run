package sapphire.policy.metric;

import java.util.ArrayList;
import java.util.Map;
import java.util.TimerTask;
import java.util.logging.Logger;
import sapphire.app.SapphireObjectServer;
import sapphire.app.SapphireObjectSpec;
import sapphire.app.labelselector.Selector;
import sapphire.common.AppObjectStub;
import sapphire.common.Utils;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.oms.OMSServer;
import sapphire.policy.SapphirePolicyUpcalls;
import sapphire.policy.util.ResettableTimer;
import sapphire.sysSapphireObjects.metricCollector.MetricCollector;
import sapphire.sysSapphireObjects.metricCollector.metric.MetricCollectorLabels;
import sapphire.sysSapphireObjects.metricCollector.metric.counter.CounterMetric;
import sapphire.sysSapphireObjects.metricCollector.metric.counter.Schema;

public class MetricAggregator {
    private static final String METRIC_NAME_RPC_COUNTER = "so_rpc_counter";
    private MetricPolicy.Config config = new MetricPolicy.Config();
    private MetricCollector collectorStub;
    private CounterMetric rpcCounter;
    private ResettableTimer metricSendTimer;
    static Logger logger = Logger.getLogger(MetricAggregator.class.getCanonicalName());

    private OMSServer oms() {
        return GlobalKernelReferences.nodeServer.oms;
    }

    public MetricAggregator(SapphireObjectSpec spec) {
        if (spec != null) {
            Map<String, SapphirePolicyUpcalls.SapphirePolicyConfig> configMap =
                    Utils.fromDMSpecListToFlatConfigMap(spec.getDmList());
            if (configMap != null) {
                SapphirePolicyUpcalls.SapphirePolicyConfig config =
                        configMap.get(MetricPolicy.Config.class.getName());
                if (config != null) {
                    this.config = ((MetricPolicy.Config) config);
                }
            }
        }
    }

    public boolean initialize() {
        return !(collectorStub == null);
    }

    public void $__initialize() throws Exception {
        if (!initialize()) {
            // create selector
            Selector select = MetricCollectorLabels.labels.asSelector();

            SapphireObjectServer server = (SapphireObjectServer) oms();

            // acquire sapphire objects based on selector
            ArrayList<AppObjectStub> sapphireStubList = server.acquireSapphireObjectStub(select);

            switch (sapphireStubList.size()) {
                case 0:
                    throw new Exception("Metric server not deployed");
                case 1:
                    break;
                default:
                    throw new Exception("Multiple metric server detected");
            }

            for (AppObjectStub stub : sapphireStubList) {
                collectorStub = (MetricCollector) stub;
            }
        }

        // register metric
        Schema metricCounterSchema = new Schema(METRIC_NAME_RPC_COUNTER, config.getMetricLabels());
        // TODO: handle exceptions if metric already registered
        collectorStub.Register(metricCounterSchema);
        rpcCounter = new CounterMetric(METRIC_NAME_RPC_COUNTER, config.getMetricLabels());

        metricSendTimer =
                new ResettableTimer(
                        new TimerTask() {
                            public void run() {
                                sendMetric();
                            }
                        },
                        config.getMetricUpdateFrequency());
    }

    public void incRpcCounter() {
        rpcCounter.incCount();
    }

    public void sendMetric() {
        try {
            collectorStub.push(rpcCounter);
        } catch (Exception e) {
            logger.warning(String.format("%s: Sending metric failed", e.toString()));
            return;
        }

        rpcCounter.reset();
        metricSendTimer.reset();
    }
}
